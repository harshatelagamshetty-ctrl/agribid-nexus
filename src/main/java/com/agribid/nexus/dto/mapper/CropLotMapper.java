package com.agribid.nexus.dto.mapper;

import com.agribid.nexus.domain.crop.CropLot;
import com.agribid.nexus.domain.crop.PestTag;
import com.agribid.nexus.dto.response.CropLotResponse;

import java.util.List;

/**
 * Hand-written mapping rather than MapStruct/ModelMapper — the
 * allowed stack specifies "DTO Pattern and Model Mapping" as a
 * concept, not a specific mapping library, and a plain static mapper
 * keeps the dependency surface minimal for a hackathon build.
 */
public final class CropLotMapper {

    private CropLotMapper() {
    }

    public static CropLotResponse toResponse(CropLot lot) {
        List<String> tagLabels = lot.getPestTags().stream()
            .map(PestTag::getLabel)
            .toList();

        return new CropLotResponse(
            lot.getId(),
            lot.getOwner().getId(),
            lot.getCategory() != null ? lot.getCategory().getCode() : null,
            lot.getCategory() != null ? lot.getCategory().getName() : null,
            lot.getQuantityKg(),
            lot.getImageUrl(),
            lot.getStatus(),
            lot.getQualityGrade() != null ? lot.getQualityGrade().getGradeLabel() : null,
            lot.getQualityGrade() != null ? lot.getQualityGrade().getEstimatedShelfLifeDays() : null,
            tagLabels,
            lot.getCreatedAt()
        );
    }
}