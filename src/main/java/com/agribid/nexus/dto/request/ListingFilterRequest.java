package com.agribid.nexus.dto.request;

import com.agribid.nexus.domain.auction.AuctionStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Backs the dynamic BidListing search endpoint. Every field is
 * optional — BidListingSpecifications treats a null field as "don't
 * filter on this," so any combination of these can be supplied.
 */
public record ListingFilterRequest(
        String cropTypeCode,
        BigDecimal minQuantityKg,
        String district,
        Instant closingBefore,
        AuctionStatus status,
        int page,
        int size
) {
    public ListingFilterRequest {
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;
    }
}