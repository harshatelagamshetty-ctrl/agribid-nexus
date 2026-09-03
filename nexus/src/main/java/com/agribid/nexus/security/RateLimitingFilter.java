package com.agribid.nexus.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * In-memory token-bucket rate limiting, deliberately scoped to
 * exactly two endpoint patterns rather than every request:
 *
 *   - POST /api/v1/auth/login — was previously brute-forceable with
 *     zero attempt limit, an open finding from the security audit.
 *   - POST /api/v1/crop-lots/{id}/video — the video-grading pipeline
 *     calls paid, real Gemini API requests per submission; with no
 *     limit, a single account could spam this endpoint and run up
 *     real API cost with no defense at all.
 *
 * In-memory (ConcurrentHashMap), not Redis-backed — the right choice
 * for a single-instance hackathon deployment. Explicitly NOT the
 * right choice if this ever runs as multiple horizontally-scaled
 * instances, since each instance would track its own independent
 * bucket per key, meaning the effective limit becomes
 * (configured limit x instance count) — that's a real, honest
 * limitation of this implementation, not a hidden one.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Pattern LOGIN_PATH = Pattern.compile("^/api/v1/auth/login$");
    private static final Pattern VIDEO_UPLOAD_PATH = Pattern.compile("^/api/v1/crop-lots/\\d+/video$");
    private static final Pattern WHATSAPP_WEBHOOK_PATH = Pattern.compile("^/api/v1/whatsapp/inbound$");
    private static final Pattern USSD_CALLBACK_PATH = Pattern.compile("^/api/v1/ussd/callback$");

    private final ConcurrentHashMap<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> videoUploadBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> whatsappWebhookBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equals(method) && LOGIN_PATH.matcher(path).matches()) {
            // Keyed by client IP, not email — the request body (which
            // would contain the email) hasn't been parsed yet at the
            // filter layer, and keying by IP is exactly the right
            // granularity for a brute-force defense anyway (an
            // attacker trying many emails from one IP is the actual
            // threat model, not one legitimate user's occasional typo).
            if (!tryConsume(loginBuckets, clientIp(request), this::newLoginBucket)) {
                respondTooManyRequests(response, "Too many login attempts. Please wait a minute and try again.");
                return;
            }
        } else if ("POST".equals(method) && VIDEO_UPLOAD_PATH.matcher(path).matches()) {
            // Keyed by IP for the same reason — the authenticated
            // farmer's identity isn't resolved yet at this point in
            // the filter chain (JwtAuthFilter runs after this one, or
            // before, depending on registration order; keying by IP
            // avoids that ordering dependency entirely).
            if (!tryConsume(videoUploadBuckets, clientIp(request), this::newVideoUploadBucket)) {
                respondTooManyRequests(response, "Too many video submissions in a short time. Please wait before submitting again.");
                return;
            }
        } else if ("POST".equals(method) && (WHATSAPP_WEBHOOK_PATH.matcher(path).matches() || USSD_CALLBACK_PATH.matcher(path).matches())) {
            // Public, unauthenticated by necessity (Twilio can't send
            // a JWT) — rate limiting is the only real defense against
            // abuse here. Generous limit since Twilio itself may
            // legitimately retry a delivery.
            if (!tryConsume(whatsappWebhookBuckets, clientIp(request), this::newWebhookBucket)) {
                respondTooManyRequests(response, "Too many requests to this webhook from this source.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean tryConsume(ConcurrentHashMap<String, Bucket> buckets, String key,
                                java.util.function.Supplier<Bucket> factory) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> factory.get());
        return bucket.tryConsume(1);
    }

    /**
     * 5 attempts per minute — generous enough that a genuine user
     * mistyping their password twice is never affected, tight enough
     * to make a real brute-force attempt impractically slow.
     */
    private Bucket newLoginBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(1)).build())
                .build();
    }

    /**
     * 10 video submissions per hour per IP — well above what a real
     * farmer listing multiple genuine lots in a session would need,
     * far below what a scripted spam attempt against the Gemini API
     * would want.
     */
    private Bucket newVideoUploadBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofHours(1)).build())
                .build();
    }

    private Bucket newWebhookBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(30).refillGreedy(30, Duration.ofMinutes(1)).build())
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void respondTooManyRequests(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"title\":\"Too Many Requests\",\"detail\":\"" + message + "\",\"status\":429}");
    }
}
