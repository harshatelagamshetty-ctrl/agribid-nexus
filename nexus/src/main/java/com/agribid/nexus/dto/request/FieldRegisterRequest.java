package com.agribid.nexus.dto.request;

import jakarta.validation.constraints.NotNull;

public record FieldRegisterRequest(
    // optional — the Field entity's constructor falls back to a
    // coordinate-based label (e.g. "Field near 19.9975, 73.7898")
    // if omitted, so a farmer is never blocked from registering a
    // field just because they didn't name it
    String fieldName,

    @NotNull(message = "latitude is required")
    Double latitude,

    @NotNull(message = "longitude is required")
    Double longitude,

    // optional — Field entity defaults to 500m if omitted
    Double radiusMeters
) {
}
