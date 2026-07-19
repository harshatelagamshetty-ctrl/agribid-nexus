package com.agribid.nexus.dto.response;

import com.agribid.nexus.domain.auction.AuctionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record BidListingResponse(
        Long id,
        Long cropLotId,
        String cropCategoryCode,
        BigDecimal quantityKg,
        BigDecimal reservePrice,
        BigDecimal currentHighestBid,
        Instant auctionOpenTime,
        Instant auctionCloseTime,
        AuctionStatus status,
        int bidCount
) {
}