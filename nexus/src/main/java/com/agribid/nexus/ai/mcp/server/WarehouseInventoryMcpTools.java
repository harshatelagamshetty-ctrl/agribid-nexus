package com.agribid.nexus.ai.mcp.server;

import com.agribid.nexus.service.WarehouseService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Auto-registered by the spring-ai-starter-mcp-server-webmvc starter
 * at startup — no manual builder/registration code needed, unlike
 * the older ToolCallback-based MCP API. Any external MCP-compliant
 * client (a logistics partner's own agent) can call
 * "warehouse-inventory" over the configured transport
 * (spring.ai.mcp.server.protocol=STREAMABLE) without any bespoke
 * REST contract on our side.
 */
@Component
public class WarehouseInventoryMcpTools {

    private final WarehouseService warehouseService;

    public WarehouseInventoryMcpTools(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @McpTool(name = "warehouse-inventory", description = "Get current available storage capacity in kg for a given warehouse ID")
    public BigDecimal getWarehouseInventory(
            @McpToolParam(description = "The warehouse's numeric ID", required = true) Long warehouseId) {
        return warehouseService.getAvailableCapacity(warehouseId);
    }

    @McpTool(name = "match-fulfillment-center", description = "Find the nearest warehouse in a region with capacity for a required quantity")
    public Long matchFulfillmentCenter(
            @McpToolParam(description = "Region to search within", required = true) String region,
            @McpToolParam(description = "Required capacity in kg", required = true) BigDecimal requiredCapacityKg) {
        return warehouseService.matchNearestFulfillmentCenter(region, requiredCapacityKg);
    }
}