package com.agribid.nexus.ai.planning.model;

import java.util.List;

public record CropRecommendationSet(
        List<Recommendation> recommendations
) {
    public record Recommendation(
            String categoryCode,
            int rank,
            DemandOutlook outlook,
            String reason
    ) {
    }
}
