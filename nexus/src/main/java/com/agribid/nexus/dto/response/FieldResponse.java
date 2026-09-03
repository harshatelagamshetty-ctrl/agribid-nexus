package com.agribid.nexus.dto.response;

import java.time.Instant;

public record FieldResponse(
    Long id,
    Long ownerId,
    String fieldName,
    Double latitude,
    Double longitude,
    Double radiusMeters,
    Instant createdAt
) {
}
