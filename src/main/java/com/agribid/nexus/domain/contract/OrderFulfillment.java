package com.agribid.nexus.domain.contract;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_fulfillments")
@Getter
@Setter
@NoArgsConstructor
public class OrderFulfillment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "tranche_quantity_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal trancheQuantityKg;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FulfillmentStatus status = FulfillmentStatus.PENDING;

    public OrderFulfillment(Order order, BigDecimal trancheQuantityKg) {
        this.order = order;
        this.trancheQuantityKg = trancheQuantityKg;
    }

    public void markDelivered() {
        this.status = FulfillmentStatus.DELIVERED;
        this.deliveredAt = Instant.now();
    }
}