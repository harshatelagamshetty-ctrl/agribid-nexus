package com.agribid.nexus.ai.logistics.model;

/**
 * cropLotId is null for the warehouse stop (sequence 0 and, on the
 * return leg, the final stop) — every other stop is one farmer's
 * pickup point, sourced from CropLot.captureLatitude/captureLongitude.
 */
public record RouteStop(
        int sequence,
        Long cropLotId,
        Long farmerId,
        String label,
        double latitude,
        double longitude,
        double legDistanceFromPreviousKm
) {
}
