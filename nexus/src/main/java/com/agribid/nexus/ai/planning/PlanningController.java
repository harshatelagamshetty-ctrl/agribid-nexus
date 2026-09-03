package com.agribid.nexus.ai.planning;

import com.agribid.nexus.ai.planning.model.CropRecommendationSet;
import com.agribid.nexus.ai.planning.model.DemandForecast;
import com.agribid.nexus.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Pre-harvest planning — this is what lets a farmer ask "what should
 * I grow, and is it worth it?" BEFORE a single CropLot exists,
 * closing the gap where the rest of this system's AI only engages
 * with produce that's already been harvested.
 */
@RestController
@RequestMapping("/api/v1/planning")
@RequiredArgsConstructor
public class PlanningController {

    private final DemandForecastService demandForecastService;

    @GetMapping("/demand-forecast")
    @PreAuthorize("hasRole('FARMER') or hasRole('AGRONOMIST') or hasRole('ADMIN')")
    public ResponseEntity<DemandForecast> forecastDemand(
            @RequestParam String categoryCode,
            @RequestParam String region,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long farmerId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(demandForecastService.forecastDemand(categoryCode, region, farmerId));
    }

    @GetMapping("/crop-recommendations")
    @PreAuthorize("hasRole('FARMER') or hasRole('AGRONOMIST') or hasRole('ADMIN')")
    public ResponseEntity<CropRecommendationSet> recommendCrops(
            @RequestParam String region,
            @RequestParam(defaultValue = "5") int topN,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long farmerId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(demandForecastService.recommendCropsToGrow(region, topN, farmerId));
    }
}
