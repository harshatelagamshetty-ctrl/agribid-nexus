package com.agribid.nexus.repository;

import com.agribid.nexus.domain.auction.AuctionStatus;
import com.agribid.nexus.domain.auction.BidListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;


public interface BidListingRepository extends
        JpaRepository<BidListing, Long>,
        JpaSpecificationExecutor<BidListing> {

    Page<BidListing> findByStatus(AuctionStatus status, Pageable pageable);

    // used by the scheduled auction-close job
    List<BidListing> findByStatusAndAuctionCloseTimeBefore(AuctionStatus status, Instant now);
}