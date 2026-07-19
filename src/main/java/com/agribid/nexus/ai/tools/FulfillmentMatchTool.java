package com.agribid.nexus.ai.tools;

import com.agribid.nexus.service.WarehouseService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FulfillmentMatchTool {

    private final WarehouseService warehouseService;

    public FulfillmentMatchTool(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @Tool(description = "Find the nearest warehouse in a region with enough available capacity to fulfill a given quantity")
    public Long matchNearestFulfillmentCenter(
            @ToolParam(description = "Region to search within, e.g. warehouseRegion of the winning distributor") String region,
            @ToolParam(description = "Required storage capacity in kg") BigDecimal requiredCapacityKg) {
        return warehouseService.matchNearestFulfillmentCenter(region, requiredCapacityKg);
    }
}