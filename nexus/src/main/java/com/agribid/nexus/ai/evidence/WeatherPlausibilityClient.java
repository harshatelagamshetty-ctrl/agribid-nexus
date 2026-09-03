package com.agribid.nexus.ai.evidence;

import com.agribid.nexus.ai.evidence.model.WeatherPlausibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Uses Open-Meteo's historical weather archive
 * (https://archive-api.open-meteo.com/v1/archive) — free, no API
 * key, no signup, 10,000 requests/day on the non-commercial tier,
 * data back to 1940. Chosen specifically because it has no
 * registration wall to fail behind during a live demo, unlike most
 * weather APIs.
 *
 * This is used as SUPPORTING evidence only, per the explicit design
 * rule: extreme, physically implausible conditions (e.g. a farmer
 * claiming a fresh tomato harvest immediately after a day of severe
 * flooding at that exact location) lower confidence to NEEDS_REVIEW.
 * Ordinary weather never raises confidence on its own — this check
 * can only ever flag inconsistency, never "prove" a submission is
 * genuine, matching the rule against overclaiming any single signal.
 */
@Component
public class WeatherPlausibilityClient {

    private static final Logger log = LoggerFactory.getLogger(WeatherPlausibilityClient.class);
    private static final String ARCHIVE_URL = "https://archive-api.open-meteo.com/v1/archive";
    private static final double EXTREME_RAIN_MM = 100.0; // a genuinely severe single-day total

    /**
     * Explicit timeouts, not the JDK HttpClient's unbounded default —
     * without this, a hung (not failed) request to Open-Meteo could
     * block for as long as the OS-level TCP timeout, which can be
     * minutes. A 3-second connect / 5-second read window is generous
     * for a same-continent API call but still fails fast enough that
     * a slow third party can never stall a live demo.
     */
    private final RestClient restClient = RestClient.builder()
            .requestFactory(clientHttpRequestFactory())
            .build();

    private static org.springframework.http.client.ClientHttpRequestFactory clientHttpRequestFactory() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return factory;
    }

    /**
     * Returns UNAVAILABLE (never a hard failure) if the API is
     * unreachable or the response is malformed — a third-party
     * outage should never block a farmer's legitimate submission,
     * per the explicit "define a fallback" rule for every external
     * dependency in this system.
     */
    public WeatherResult checkPlausibility(double latitude, double longitude, Instant capturedAt) {
        try {
            LocalDate date = capturedAt.atZone(ZoneOffset.UTC).toLocalDate();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(ARCHIVE_URL + "?latitude={lat}&longitude={lon}&start_date={date}&end_date={date}&daily=precipitation_sum&timezone=UTC",
                            latitude, longitude, date, date)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("daily")) {
                return new WeatherResult(WeatherPlausibility.UNAVAILABLE, "Weather data unavailable for this location/date");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> daily = (Map<String, Object>) response.get("daily");
            @SuppressWarnings("unchecked")
            List<Number> precipitation = (List<Number>) daily.get("precipitation_sum");

            if (precipitation == null || precipitation.isEmpty() || precipitation.get(0) == null) {
                return new WeatherResult(WeatherPlausibility.UNAVAILABLE, "No precipitation record found for this date");
            }

            double rainMm = precipitation.get(0).doubleValue();
            if (rainMm >= EXTREME_RAIN_MM) {
                return new WeatherResult(WeatherPlausibility.INCONSISTENT,
                        "Severe rainfall recorded (%.1fmm) on the capture date at this location — worth a closer look, not a hard block".formatted(rainMm));
            }

            return new WeatherResult(WeatherPlausibility.CONSISTENT, "No extreme weather recorded on the capture date");

        } catch (Exception ex) {
            log.warn("Weather plausibility check failed, treating as UNAVAILABLE (non-fatal): {}", ex.getMessage());
            return new WeatherResult(WeatherPlausibility.UNAVAILABLE, "Weather service unreachable — this check was skipped");
        }
    }

    public record WeatherResult(WeatherPlausibility plausibility, String note) {
    }
}
