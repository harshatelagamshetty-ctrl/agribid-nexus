package com.agribid.nexus.service.impl;

import com.agribid.nexus.domain.logistics.Warehouse;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.repository.WarehouseRepository;
import com.agribid.nexus.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Override
    public BigDecimal getAvailableCapacity(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + warehouseId));
        return warehouse.availableCapacityKg();
    }

    /**
     * Returns null when no warehouse in the region has enough
     * capacity, rather than throwing. The only two callers of this
     * method (ai/tools/FulfillmentMatchTool and
     * ai/mcp/server/WarehouseInventoryMcpTools) both invoke it from
     * inside an AI tool-calling loop — throwing ResourceNotFoundException
     * here (as an earlier version of this method did) propagates
     * straight through Gemini's tool-calling mechanism and breaks the
     * ENTIRE response, not just this one fact. A null return lets the
     * model say "no warehouse in that region has capacity" instead of
     * the whole request failing with a 500.
     */
    @Override
    public Long matchNearestFulfillmentCenter(String region, BigDecimal requiredCapacityKg) {
        List<Warehouse> candidates = warehouseRepository.findByRegion(region);

        return candidates.stream()
                .filter(w -> w.availableCapacityKg().compareTo(requiredCapacityKg) >= 0)
                .max(Comparator.comparing(Warehouse::availableCapacityKg))
                .map(Warehouse::getId)
                .orElse(null);
    }
}