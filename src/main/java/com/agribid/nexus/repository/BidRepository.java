package com.agribid.nexus.repository;

import com.agribid.nexus.domain.auction.Bid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface BidRepository extends JpaRepository<Bid, Long> {

    /**
     * Keyset pagination anchored on bidTimestamp. Chosen deliberately
     * over OFFSET-based Pageable.of(page, size) for this endpoint:
     * OFFSET cost grows linearly with page depth because the database
     * must still scan and discard every preceding row. Under a hot
     * auction generating thousands of bids, deep OFFSET pages become a
     * performance cliff. This query's cost stays constant regardless
     * of how far into the bid history the client is paging.
     */
    @Query("""
        SELECT b FROM Bid b
        WHERE b.listing.id = :listingId
          AND (:lastTimestamp IS NULL OR b.bidTimestamp < :lastTimestamp)
        ORDER BY b.bidTimestamp DESC
    """)
    Slice<Bid> findRecentBids(
            @Param("listingId") Long listingId,
            @Param("lastTimestamp") Instant lastTimestamp,
            Pageable pageable
    );

    long countByListingId(Long listingId);
}