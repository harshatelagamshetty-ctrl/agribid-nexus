package com.agribid.nexus.dto.mapper;

import com.agribid.nexus.domain.auction.Bid;
import com.agribid.nexus.domain.auction.BidListing;
import com.agribid.nexus.dto.response.BidListingResponse;
import com.agribid.nexus.dto.response.BidResponse;
import com.agribid.nexus.dto.response.BidStreamPage;

import java.util.List;

public final class BidListingMapper {

    private BidListingMapper() {
    }

    public static BidListingResponse toResponse(BidListing listing) {
        return new BidListingResponse(
            listing.getId(),
            listing.getCropLot().getId(),
            listing.getCropLot().getCategory() != null ? listing.getCropLot().getCategory().getCode() : null,
            listing.getCropLot().getQuantityKg(),
            listing.getReservePrice(),
            listing.getCurrentHighestBid(),
            listing.getAuctionOpenTime(),
            listing.getAuctionCloseTime(),
            listing.getStatus(),
            listing.getBids().size()
        );
    }

    public static BidResponse toResponse(Bid bid) {
        return new BidResponse(
            bid.getId(),
            bid.getListing().getId(),
            bid.getBidder().getId(),
            bid.getAmount(),
            bid.getBidTimestamp()
        );
    }

    public static BidStreamPage toStreamPage(List<Bid> bids, boolean hasMore) {
        List<BidResponse> responses = bids.stream().map(BidListingMapper::toResponse).toList();
        var nextCursor = bids.isEmpty() ? null : bids.get(bids.size() - 1).getBidTimestamp();
        return new BidStreamPage(responses, nextCursor, hasMore);
    }
}