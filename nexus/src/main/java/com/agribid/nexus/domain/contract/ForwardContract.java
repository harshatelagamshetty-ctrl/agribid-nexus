package com.agribid.nexus.domain.contract;

import com.agribid.nexus.domain.auction.BidListing;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The binding conversion of a winning bid into a locked-price
 * delivery contract.
 *
 * @OneToOne with a UNIQUE constraint on listing_id means it is
 * structurally impossible — at the database level, not just via
 * application discipline — for a single auction to spawn two
 * competing contracts.
 *
 * Does NOT hold OrderFulfillment directly: fulfillments belong to
 * Order (see domain/contract/Order.java), which is itself a
 * one-to-one child of this contract. That extra level of indirection
 * is deliberate — see Order's Javadoc — but it means this class has
 * no fulfillments collection of its own.
 */
@Entity
@Table(name = "forward_contracts")
@Getter
@Setter
@NoArgsConstructor
public class ForwardContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", unique = true, nullable = false)
    private BidListing sourceListing;

    @Column(name = "locked_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal lockedPrice;

    @Column(name = "delivery_deadline", nullable = false)
    private LocalDate deliveryDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status = ContractStatus.ACTIVE;

    public ForwardContract(BidListing sourceListing, BigDecimal lockedPrice, LocalDate deliveryDeadline) {
        this.sourceListing = sourceListing;
        this.lockedPrice = lockedPrice;
        this.deliveryDeadline = deliveryDeadline;
    }
}