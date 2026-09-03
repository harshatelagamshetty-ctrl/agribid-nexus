package com.agribid.nexus.service;

import java.math.BigDecimal;

public interface WarehouseService {

    /**
     * Backs the checkWarehouseCapacity @Tool bean that Gemini invokes
     * during negotiation — must return a real, current figure, not a
     * cached/stale one, since it's grounding an AI-generated
     * counter-offer in actual logistics feasibility.
     */
    BigDecimal getAvailableCapacity(Long warehouseId);

    Long matchNearestFulfillmentCenter(String region, BigDecimal requiredCapacityKg);
}
