package com.agribid.nexus.dto.mapper;

import com.agribid.nexus.domain.crop.CropLotEvidenceReport;
import com.agribid.nexus.dto.response.CropLotEvidenceReportResponse;

public final class CropLotEvidenceReportMapper {

    private CropLotEvidenceReportMapper() {
    }

    public static CropLotEvidenceReportResponse toResponse(CropLotEvidenceReport report) {
        return new CropLotEvidenceReportResponse(
            report.getCropLot().getId(),
            report.getFieldMatch(),
            report.getFieldMatchDistanceMeters(),
            report.getTravelPlausibility(),
            report.getDuplicateCheck(),
            report.getSeasonalityCheck(),
            report.getWeatherPlausibility(),
            report.getWeatherNote(),
            report.getChallengeResult(),
            report.getCoverageResult(),
            report.getOverallEvidence(),
            report.getReviewStatus(),
            report.getReviewedBy(),
            report.getReviewedAt(),
            report.getReviewNote(),
            report.getAssessedAt()
        );
    }
}
