package com.agribid.nexus.dto.response;

import com.agribid.nexus.domain.crop.PoolStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record FpoPoolResponse(
        Long id,
        String fpoName,
        String categoryCode,
        String categoryName,
        Long coordinatorFarmerId,
        BigDecimal targetQuantityKg,
        BigDecimal aggregatedQuantityKg,
        BigDecimal percentToTarget,
        PoolStatus status,
        Long resultingCropLotId,
        Instant createdAt,
        Instant aggregatedAt
) {
}
