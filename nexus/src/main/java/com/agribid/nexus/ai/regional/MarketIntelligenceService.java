package com.agribid.nexus.ai.regional;

import com.agribid.nexus.domain.auction.Bid;
import com.agribid.nexus.domain.auction.BidListing;
import com.agribid.nexus.domain.regional.RegionalSignal;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.repository.BidListingRepository;
import com.agribid.nexus.repository.RegionalSignalRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Four related, honestly-scoped analyses, all pure computation over
 * data already persisted elsewhere — none of these are trained
 * models, and none claim more certainty than the underlying sample
 * size actually supports.
 */
@Service
public class MarketIntelligenceService {

    private static final Duration SNIPING_WINDOW = Duration.ofSeconds(30);
    private static final double VOLATILITY_FLAG_THRESHOLD = 0.20; // 20% swing week-over-week

    private final BidListingRepository bidListingRepository;
    private final RegionalSignalRepository regionalSignalRepository;

    public MarketIntelligenceService(BidListingRepository bidListingRepository, RegionalSignalRepository regionalSignalRepository) {
        this.bidListingRepository = bidListingRepository;
        this.regionalSignalRepository = regionalSignalRepository;
    }

    /**
     * "Sniping" here means simply: were multiple bids placed within
     * the last 30 seconds before close. This is descriptive, not
     * accusatory — clustering near a deadline is completely normal,
     * rational bidder behavior, not inherently evidence of anything
     * improper. Framed to agronomists/admins as an observation, not
     * a fraud finding.
     */
    public Map<String, Object> getBidPatternAnalysis(Long listingId) {
        BidListing listing = bidListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + listingId));

        long lateBids = listing.getBids().stream()
                .filter(b -> Duration.between(b.getBidTimestamp(), listing.getAuctionCloseTime())
                        .compareTo(SNIPING_WINDOW) <= 0)
                .count();

        return Map.of(
                "listingId", listingId,
                "totalBids", listing.getBids().size(),
                "bidsInFinal30Seconds", lateBids,
                "note", "Descriptive only — clustering near a deadline is normal bidder behavior, not evidence of manipulation on its own."
        );
    }

    /**
     * A real ratio over two consecutive weeks' verified settled
     * prices in RegionalSignal — flags only when both weeks actually
     * have data, never fabricates a comparison from a single data
     * point.
     */
    public Map<String, Object> getPriceVolatilityFlag(String district, Long categoryId) {
        List<RegionalSignal> history = regionalSignalRepository
                .findByDistrictAndCategoryIdOrderByWeekStartDesc(district, categoryId);

        List<RegionalSignal> withPrices = history.stream()
                .filter(s -> s.getAvgSettledPricePerKg() != null)
                .sorted(Comparator.comparing(RegionalSignal::getWeekStart).reversed())
                .limit(2)
                .toList();

        if (withPrices.size() < 2) {
            return Map.of("district", district, "categoryId", categoryId, "flagged", false,
                    "note", "Not enough weeks of settled-price history yet to assess volatility.");
        }

        BigDecimal latest = withPrices.get(0).getAvgSettledPricePerKg();
        BigDecimal prior = withPrices.get(1).getAvgSettledPricePerKg();
        double changeRatio = prior.compareTo(BigDecimal.ZERO) == 0 ? 0
                : latest.subtract(prior).abs().divide(prior, 4, java.math.RoundingMode.HALF_UP).doubleValue();

        return Map.of(
                "district", district, "categoryId", categoryId,
                "latestWeekAvgPrice", latest, "priorWeekAvgPrice", prior,
                "changeRatio", changeRatio, "flagged", changeRatio >= VOLATILITY_FLAG_THRESHOLD,
                "note", "A real week-over-week comparison of verified settled prices, not a prediction."
        );
    }

    /**
     * Compares the same category's latest verified price across two
     * named districts — a real signal, but explicitly not a
     * transport-cost-adjusted recommendation, since this system has
     * no real freight-cost data to net against the raw price gap.
     */
    public Map<String, Object> getCrossRegionComparison(String districtA, String districtB, Long categoryId) {
        BigDecimal priceA = latestPrice(districtA, categoryId);
        BigDecimal priceB = latestPrice(districtB, categoryId);
        // Map.of() throws NullPointerException on any null value —
        // priceA/priceB are genuinely null whenever a district has no
        // settled-price history yet, which is the normal state for
        // most districts early on, not an edge case.
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("districtA", districtA);
        result.put("priceA", priceA);
        result.put("districtB", districtB);
        result.put("priceB", priceB);
        result.put("note", "A raw price comparison only — does not account for real transport cost between the two districts, which this system does not have data for.");
        return result;
    }

    private BigDecimal latestPrice(String district, Long categoryId) {
        return regionalSignalRepository.findByDistrictAndCategoryIdOrderByWeekStartDesc(district, categoryId)
                .stream()
                .filter(s -> s.getAvgSettledPricePerKg() != null)
                .findFirst()
                .map(RegionalSignal::getAvgSettledPricePerKg)
                .orElse(null);
    }

    /**
     * Deliberately simple: compares this week's verified average
     * against the trailing 4-week average and suggests SELL_NOW or
     * WAIT only when the signal is unambiguous — anything close
     * returns NO_STRONG_SIGNAL rather than forcing a guess.
     */
    public Map<String, Object> getBestTimeToSellGuidance(String district, Long categoryId) {
        List<RegionalSignal> history = regionalSignalRepository
                .findByDistrictAndCategoryIdOrderByWeekStartDesc(district, categoryId)
                .stream().filter(s -> s.getAvgSettledPricePerKg() != null).limit(5).toList();

        if (history.size() < 3) {
            return Map.of("district", district, "categoryId", categoryId, "guidance", "INSUFFICIENT_DATA",
                    "note", "Fewer than 3 weeks of verified settled-price history — no guidance offered rather than a guess.");
        }

        BigDecimal current = history.get(0).getAvgSettledPricePerKg();
        BigDecimal trailingAvg = history.stream()
                .skip(1)
                .map(RegionalSignal::getAvgSettledPricePerKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(history.size() - 1), 2, java.math.RoundingMode.HALF_UP);

        double deltaRatio = trailingAvg.compareTo(BigDecimal.ZERO) == 0 ? 0
                : current.subtract(trailingAvg).divide(trailingAvg, 4, java.math.RoundingMode.HALF_UP).doubleValue();

        String guidance = deltaRatio >= 0.10 ? "CONSIDER_SELLING_NOW"
                : deltaRatio <= -0.10 ? "CONSIDER_WAITING"
                : "NO_STRONG_SIGNAL";

        return Map.of(
                "district", district, "categoryId", categoryId,
                "currentWeekAvgPrice", current, "trailingFourWeekAvgPrice", trailingAvg,
                "guidance", guidance,
                "note", "A real comparison against recent verified settled prices — not a forecast, and not financial advice."
        );
    }
}
