package com.agribid.nexus.domain.crop;

/**
 * OPEN          - accepting contributions from FPO members
 * AGGREGATED    - target reached and coordinator has merged
 *                 contributions into one resulting CropLot
 * LISTED        - the resulting CropLot has been published as a
 *                 BidListing (tracked informationally here; the
 *                 actual source of truth is CropLot/BidListing)
 * CANCELLED     - pool abandoned before aggregation; contributions
 *                 released back to members' individual lots
 */
public enum PoolStatus {
    OPEN,
    AGGREGATED,
    LISTED,
    CANCELLED
}
