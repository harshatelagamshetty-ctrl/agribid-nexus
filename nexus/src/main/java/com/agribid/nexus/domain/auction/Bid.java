package com.agribid.nexus.domain.auction;

import com.agribid.nexus.domain.user.DistributorProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Individual bid record. bidTimestamp is the anchor column for the
 * keyset-pagination bid-stream query (see BidRepository) — chosen
 * specifically so live bid-stream reads never degrade under OFFSET
 * pagination as bid volume grows during a hot auction.
 */
@Entity
@Table(name = "bids", indexes = {
        @Index(name = "idx_bid_listing_timestamp", columnList = "listing_id, bid_timestamp")
})
@Getter
@Setter
@NoArgsConstructor
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private BidListing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distributor_id", nullable = false)
    private DistributorProfile bidder;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @CreationTimestamp
    @Column(name = "bid_timestamp", updatable = false)
    private Instant bidTimestamp;

    public Bid(BidListing listing, DistributorProfile bidder, BigDecimal amount) {
        this.listing = listing;
        this.bidder = bidder;
        this.amount = amount;
    }
}