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

    @Override
    public Long matchNearestFulfillmentCenter(String region, BigDecimal requiredCapacityKg) {
        List<Warehouse> candidates = warehouseRepository.findByRegion(region);

        return candidates.stream()
            .filter(w -> w.availableCapacityKg().compareTo(requiredCapacityKg) >= 0)
            .max(Comparator.comparing(Warehouse::availableCapacityKg))
            .map(Warehouse::getId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No warehouse in region " + region + " has capacity for " + requiredCapacityKg + "kg"));
    }
}
