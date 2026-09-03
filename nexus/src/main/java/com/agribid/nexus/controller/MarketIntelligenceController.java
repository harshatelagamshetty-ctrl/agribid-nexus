package com.agribid.nexus.controller;

import com.agribid.nexus.ai.regional.MarketIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/market-intelligence")
@RequiredArgsConstructor
public class MarketIntelligenceController {

    private final MarketIntelligenceService marketIntelligenceService;

    @GetMapping("/listings/{listingId}/bid-pattern")
    public ResponseEntity<Map<String, Object>> getBidPattern(@PathVariable Long listingId) {
        return ResponseEntity.ok(marketIntelligenceService.getBidPatternAnalysis(listingId));
    }

    @GetMapping("/price-volatility")
    public ResponseEntity<Map<String, Object>> getPriceVolatility(
            @RequestParam String district, @RequestParam Long categoryId) {
        return ResponseEntity.ok(marketIntelligenceService.getPriceVolatilityFlag(district, categoryId));
    }

    @GetMapping("/cross-region-comparison")
    public ResponseEntity<Map<String, Object>> getCrossRegionComparison(
            @RequestParam String districtA, @RequestParam String districtB, @RequestParam Long categoryId) {
        return ResponseEntity.ok(marketIntelligenceService.getCrossRegionComparison(districtA, districtB, categoryId));
    }

    @GetMapping("/best-time-to-sell")
    public ResponseEntity<Map<String, Object>> getBestTimeToSell(
            @RequestParam String district, @RequestParam Long categoryId) {
        return ResponseEntity.ok(marketIntelligenceService.getBestTimeToSellGuidance(district, categoryId));
    }
}
