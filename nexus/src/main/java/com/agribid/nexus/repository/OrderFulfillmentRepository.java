package com.agribid.nexus.repository;

import com.agribid.nexus.domain.contract.FulfillmentStatus;
import com.agribid.nexus.domain.contract.OrderFulfillment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderFulfillmentRepository extends JpaRepository<OrderFulfillment, Long> {
    Page<OrderFulfillment> findByOrderId(Long orderId, Pageable pageable);
    Page<OrderFulfillment> findByStatus(FulfillmentStatus status, Pageable pageable);

    /**
     * Identifies "the winning distributor" the same way
     * BidListingServiceImpl.convertToContract does — the bid with
     * the maximum amount on the listing behind this order's
     * contract. There's no direct distributorId column on Order or
     * OrderFulfillment; this traces the real relationship chain
     * instead of duplicating that data redundantly.
     */
    @Query("""
        SELECT f FROM OrderFulfillment f
        JOIN f.order o
        JOIN o.contract c
        JOIN c.sourceListing l
        JOIN l.bids b
        WHERE b.bidder.id = :distributorId
        AND b.amount = (SELECT MAX(b2.amount) FROM Bid b2 WHERE b2.listing = l)
        """)
    List<OrderFulfillment> findByWinningDistributorId(Long distributorId);
}