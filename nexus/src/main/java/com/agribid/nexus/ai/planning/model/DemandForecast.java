package com.agribid.nexus.ai.planning.model;

import java.util.List;

/**
 * Coerced directly via ChatClient's .entity(DemandForecast.class),
 * same discipline as CropGradeAssessment and ReservePriceSuggestion —
 * this is never shown to a farmer as raw chat text, only as this
 * typed shape.
 */
public record DemandForecast(
        DemandOutlook outlook,
        double confidenceScore,
        String narrative,
        List<String> citedSources
) {
}
