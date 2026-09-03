package com.agribid.nexus.ai.tools;

import com.agribid.nexus.ai.logistics.RouteOptimizationService;
import com.agribid.nexus.ai.logistics.model.RoutePlan;
import com.agribid.nexus.ai.logistics.model.RouteStop;
import com.agribid.nexus.exception.ResourceNotFoundException;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Thin @Tool wrapper around RouteOptimizationService, same pattern as
 * WarehouseCapacityTool — no routing logic duplicated here. Returns
 * a compact plain-text summary rather than the raw RoutePlan record:
 * Gemini reads this directly back to the distributor in chat, and a
 * short ordered-stop-list-plus-distance sentence is far more useful
 * there than a JSON blob.
 */
@Component
public class RouteOptimizationTool {

    private final RouteOptimizationService routeOptimizationService;

    public RouteOptimizationTool(RouteOptimizationService routeOptimizationService) {
        this.routeOptimizationService = routeOptimizationService;
    }

    /**
     * This is the third occurrence, found during audit, of the same
     * bug class already fixed once in MspLookupTool and once in
     * WarehouseServiceImpl: RouteOptimizationService throws real
     * exceptions (missing GPS on a lot, warehouse not geocoded,
     * unknown ID) that are entirely correct as REST-layer errors, but
     * fatal if allowed to propagate raw out of a @Tool method — an
     * exception here doesn't fail one fact, it kills Gemini's entire
     * response for the whole conversation turn. Catching here and
     * returning an informative string lets the model relay a clear,
     * specific "why this couldn't be computed" back to the
     * distributor instead of the request failing outright.
     */
    @Tool(description = "Compute the most efficient pickup order and total travel distance for collecting several crop lots and delivering them to one warehouse, using real GPS coordinates. Returns an explanation instead of a route if any lot or the warehouse is missing GPS data — relay that explanation to the user rather than guessing a route.")
    public String optimizePickupRoute(
            @ToolParam(description = "The destination warehouse's numeric ID") Long warehouseId,
            @ToolParam(description = "The numeric IDs of the crop lots to collect, in any order") List<Long> cropLotIds) {

        RoutePlan plan;
        try {
            plan = routeOptimizationService.optimizePickupRoute(warehouseId, cropLotIds);
        } catch (ResourceNotFoundException ex) {
            return "Could not plan a route: " + ex.getMessage();
        } catch (IllegalStateException ex) {
            return "Could not plan a route: " + ex.getMessage()
                    + ". A route can only be computed once every lot has an attached, GPS-tagged video and the warehouse has geocoordinates on file.";
        } catch (IllegalArgumentException ex) {
            return "Could not plan a route: " + ex.getMessage();
        }

        String stopSummary = plan.orderedStops().stream()
                .map(RouteStop::label)
                .collect(Collectors.joining(" -> "));

        return "Optimized route for %d pooled crop lot(s) to %s: %s. Total distance: %.2f km (vs %.2f km in submission order, a %.1f%% reduction)."
                .formatted(plan.farmersPooled(), plan.warehouseName(), stopSummary,
                        plan.totalDistanceKm(), plan.naiveOrderDistanceKm(), plan.distanceSavedPercent());
    }
}
