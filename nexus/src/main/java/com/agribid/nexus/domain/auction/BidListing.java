package com.agribid.nexus.domain.auction;

import com.agribid.nexus.domain.contract.ForwardContract;
import com.agribid.nexus.domain.crop.CropLot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The market event for a CropLot.
 *
 * @Version drives Hibernate's optimistic locking: any concurrent
 * write attempting to update currentHighestBid against a stale
 * version is rejected with an OptimisticLockException and forced to
 * re-read-then-retry. This is the primary defense against race
 * conditions during high-frequency, closing-seconds bidding.
 */
@Entity
@Table(name = "bid_listings")
@Getter
@Setter
@NoArgsConstructor
public class BidListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_lot_id", nullable = false)
    private CropLot cropLot;

    @Column(name = "reserve_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal reservePrice;

    @Column(name = "current_highest_bid", precision = 12, scale = 2)
    private BigDecimal currentHighestBid;

    @Column(name = "auction_open_time", nullable = false)
    private Instant auctionOpenTime;

    @Column(name = "auction_close_time", nullable = false)
    private Instant auctionCloseTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status = AuctionStatus.OPEN;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("amount DESC")
    private List<Bid> bids = new ArrayList<>();

    @OneToOne(mappedBy = "sourceListing", cascade = CascadeType.ALL)
    private ForwardContract contract;

    /**
     * Optimistic lock token. Hibernate increments this on every
     * UPDATE and checks it in the WHERE clause of subsequent
     * UPDATEs, guaranteeing no bid is accepted against auction
     * state that has already moved on.
     */
    @Version
    @Column(name = "version")
    private Long version;

    public BidListing(CropLot cropLot, BigDecimal reservePrice, Instant openTime, Instant closeTime) {
        this.cropLot = cropLot;
        this.reservePrice = reservePrice;
        this.auctionOpenTime = openTime;
        this.auctionCloseTime = closeTime;
    }

    public boolean isOpen() {
        return status == AuctionStatus.OPEN && Instant.now().isBefore(auctionCloseTime);
    }
}