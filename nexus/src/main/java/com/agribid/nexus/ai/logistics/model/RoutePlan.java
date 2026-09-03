package com.agribid.nexus.ai.logistics.model;

import java.util.List;

public record RoutePlan(
        Long warehouseId,
        String warehouseName,
        List<RouteStop> orderedStops,
        double totalDistanceKm,
        double naiveOrderDistanceKm,
        double distanceSavedPercent,
        int farmersPooled
) {
}
