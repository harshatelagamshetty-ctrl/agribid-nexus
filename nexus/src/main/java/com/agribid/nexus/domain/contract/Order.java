package com.agribid.nexus.domain.contract;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The executable order derived from an ACTIVE ForwardContract.
 * Kept as its own entity (rather than fulfilling directly against
 * the contract) so order-level metadata — creation time, cancellation,
 * dispute flags — doesn't pollute the contract's own lifecycle.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", unique = true, nullable = false)
    private ForwardContract contract;

    /**
     * One-to-Many: bulk agricultural procurement is essentially
     * never delivered in a single shipment. Modeling fulfillments
     * as a child collection reflects that reality directly in the
     * schema rather than treating "order" as an atomic delivery.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderFulfillment> fulfillments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public Order(ForwardContract contract) {
        this.contract = contract;
    }
}