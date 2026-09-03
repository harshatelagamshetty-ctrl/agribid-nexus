package com.agribid.nexus.dto.mapper;

import com.agribid.nexus.domain.crop.CropLot;
import com.agribid.nexus.domain.crop.PestTag;
import com.agribid.nexus.dto.response.CropLotResponse;

import java.util.List;

public final class CropLotMapper {

    private CropLotMapper() {
    }

    public static CropLotResponse toResponse(CropLot lot) {
        List<String> tagLabels = lot.getPestTags().stream()
                .map(PestTag::getLabel)
                .toList();

        // The badge is TRUE only when the farmer explicitly reported
        // BOTH fields as low-usage — a missing/unset field never
        // silently counts as "low," since that would misrepresent a
        // farmer who simply didn't answer as one who made a claim.
        Boolean lowInputBadge = (Boolean.TRUE.equals(lot.getSelfReportedLowWaterUsage())
                && Boolean.TRUE.equals(lot.getSelfReportedLowPesticideUsage()));

        return new CropLotResponse(
                lot.getId(),
                lot.getOwner().getId(),
                lot.getCategory() != null ? lot.getCategory().getCode() : null,
                lot.getCategory() != null ? lot.getCategory().getName() : null,
                lot.getQuantityKg(),
                lot.getVideoUrl(),
                lot.getCaptureLatitude(),
                lot.getCaptureLongitude(),
                lot.getCapturedAt(),
                lot.getStatus(),
                lot.getQualityGrade() != null ? lot.getQualityGrade().getGradeLabel() : null,
                lot.getQualityGrade() != null ? lot.getQualityGrade().getEstimatedShelfLifeDays() : null,
                tagLabels,
                lot.getCreatedAt(),
                lot.getSelfReportedLowWaterUsage(),
                lot.getSelfReportedLowPesticideUsage(),
                lowInputBadge,
                lot.getWasOfflineCapture()
        );
    }
}