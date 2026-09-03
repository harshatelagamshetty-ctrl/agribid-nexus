package com.agribid.nexus.ai.tools;

import com.agribid.nexus.service.WarehouseService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Thin @Tool wrapper around WarehouseService — deliberately doesn't
 * duplicate any logic here, so the "what counts as available
 * capacity" rule lives in exactly one place whether it's called from
 * a REST controller or from Gemini's tool-calling loop.
 */
@Component
public class WarehouseCapacityTool {

    private final WarehouseService warehouseService;

    public WarehouseCapacityTool(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @Tool(description = "Check the current available storage capacity, in kg, at a given warehouse")
    public BigDecimal checkWarehouseCapacity(
            @ToolParam(description = "The warehouse's numeric ID") Long warehouseId) {
        return warehouseService.getAvailableCapacity(warehouseId);
    }
}