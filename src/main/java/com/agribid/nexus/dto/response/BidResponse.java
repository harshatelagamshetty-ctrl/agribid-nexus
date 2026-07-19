package com.agribid.nexus.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record BidResponse(
        Long id,
        Long listingId,
        Long bidderId,
        BigDecimal amount,
        Instant bidTimestamp
) {
}