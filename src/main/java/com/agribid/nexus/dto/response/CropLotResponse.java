package com.agribid.nexus.dto.response;

import com.agribid.nexus.domain.crop.LotStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CropLotResponse(
        Long id,
        Long ownerId,
        String categoryCode,
        String categoryName,
        BigDecimal quantityKg,
        String imageUrl,
        LotStatus status,
        String qualityGrade,
        Integer estimatedShelfLifeDays,
        List<String> pestTags,
        Instant createdAt
) {
}