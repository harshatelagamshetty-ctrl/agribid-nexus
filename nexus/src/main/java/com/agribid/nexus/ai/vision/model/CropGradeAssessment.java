package com.agribid.nexus.ai.vision.model;

import java.util.List;

/**
 * The exact shape Gemini's vision response is coerced into via
 * ChatClient's .entity(CropGradeAssessment.class) — the moment the
 * model speaks, its output becomes this strongly-typed record, never
 * stored or trusted as raw text.
 */
public record CropGradeAssessment(
    String qualityGrade,
    int estimatedShelfLifeDays,
    List<String> detectedPestTags,
    double confidenceScore
) {
}
