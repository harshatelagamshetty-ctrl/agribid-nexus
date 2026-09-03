package com.agribid.nexus.dto.response;

import com.agribid.nexus.domain.contract.FulfillmentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderFulfillmentResponse(
    Long id,
    Long orderId,
    BigDecimal trancheQuantityKg,
    Instant deliveredAt,
    FulfillmentStatus status
) {
}