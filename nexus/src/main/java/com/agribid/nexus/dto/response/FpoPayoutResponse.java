package com.agribid.nexus.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record FpoPayoutResponse(
        Long pooledLotId,
        BigDecimal totalQuantityKg,
        BigDecimal settledPricePerKg,
        BigDecimal totalSettledAmount,
        List<Share> shares
) {
    public record Share(
            Long contributorFarmerId,
            Long cropLotId,
            BigDecimal contributedQuantityKg,
            BigDecimal shareOfTotal,
            BigDecimal payoutAmount
    ) {
    }
}
