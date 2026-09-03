package com.agribid.nexus.dto.response;

import com.agribid.nexus.ai.evidence.model.*;

import java.time.Instant;

public record CropLotEvidenceReportResponse(
    Long cropLotId,
    FieldMatchResult fieldMatch,
    Double fieldMatchDistanceMeters,
    TravelPlausibility travelPlausibility,
    DuplicateCheckResult duplicateCheck,
    SeasonalityResult seasonalityCheck,
    WeatherPlausibility weatherPlausibility,
    String weatherNote,
    ChallengeResult challengeResult,
    CoverageResult coverageResult,
    OverallEvidence overallEvidence,
    ReviewStatus reviewStatus,
    Long reviewedBy,
    Instant reviewedAt,
    String reviewNote,
    Instant assessedAt
) {
}
