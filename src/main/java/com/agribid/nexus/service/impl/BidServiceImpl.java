package com.agribid.nexus.service.impl;

import com.agribid.nexus.domain.auction.Bid;
import com.agribid.nexus.domain.auction.BidListing;
import com.agribid.nexus.domain.user.DistributorProfile;
import com.agribid.nexus.dto.mapper.BidListingMapper;
import com.agribid.nexus.dto.request.BidRequest;
import com.agribid.nexus.dto.response.BidResponse;
import com.agribid.nexus.dto.response.BidStreamPage;
import com.agribid.nexus.exception.AuctionClosedException;
import com.agribid.nexus.exception.ConcurrentBidConflictException;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.repository.BidListingRepository;
import com.agribid.nexus.repository.BidRepository;
import com.agribid.nexus.repository.DistributorProfileRepository;
import com.agribid.nexus.security.UserPrincipal;
import com.agribid.nexus.service.BidService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BidServiceImpl implements BidService {

    private final BidListingRepository bidListingRepository;
    private final BidRepository bidRepository;
    private final DistributorProfileRepository distributorProfileRepository;

    /**
     * The @Version column on BidListing (see domain/auction/BidListing)
     * does the actual concurrency enforcement: this method reads
     * currentHighestBid, and if another transaction commits a higher
     * bid between our read and our write, Hibernate's UPDATE ... WHERE
     * version = :readVersion affects zero rows and throws
     * ObjectOptimisticLockingFailureException — which we translate
     * into a 409 the client can safely retry against fresh state.
     *
     * @MinBidIncrement on BidRequest already rejected sub-threshold
     * bids before this method runs, so by the time we're here the
     * bid is known to be numerically valid against the state the
     * client last read — this method's job is purely to guarantee
     * that state hasn't moved on since.
     */
    @Override
    @Transactional
    public BidResponse placeBid(BidRequest request, UserPrincipal distributorPrincipal) {
        BidListing listing = bidListingRepository.findById(request.listingId())
            .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + request.listingId()));

        if (!listing.isOpen()) {
            throw new AuctionClosedException("Auction for listing " + listing.getId() + " is not open");
        }

        DistributorProfile bidder = distributorProfileRepository.findById(distributorPrincipal.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Distributor not found: " + distributorPrincipal.getId()));

        try {
            Bid bid = new Bid(listing, bidder, request.amount());
            listing.setCurrentHighestBid(request.amount());
            listing.getBids().add(bid);

            bidListingRepository.saveAndFlush(listing);
            return BidListingMapper.toResponse(bid);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConcurrentBidConflictException(
                "Listing " + listing.getId() + " changed since you last read it — refresh and resubmit your bid");
        }
    }

    @Override
    public BidStreamPage streamBids(Long listingId, Instant lastTimestamp, int pageSize) {
        // request one extra row to cheaply determine hasMore without a separate count query
        Slice<Bid> slice = bidRepository.findRecentBids(listingId, lastTimestamp, PageRequest.of(0, pageSize));
        return BidListingMapper.toStreamPage(slice.getContent(), slice.hasNext());
    }
}
