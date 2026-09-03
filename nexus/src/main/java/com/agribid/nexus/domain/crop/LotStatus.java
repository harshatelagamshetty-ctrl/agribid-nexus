package com.agribid.nexus.domain.crop;

/**
 * Enforces, at the schema level, that a lot cannot be auctioned
 * before it has passed AI quality inspection: business rules
 * encoded in the domain model, not just the service layer.
 */
public enum LotStatus {
    DRAFT,
    GRADED,
    LISTED,
    SOLD,
    EXPIRED
}