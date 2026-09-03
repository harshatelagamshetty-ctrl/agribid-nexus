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

    /**
     * The most recent position update from whoever is physically
     * transporting this tranche — deliberately just a raw position
     * with a timestamp, not a full route history. Real ETA is
     * computed on read (see LogisticsTrackingService) from this
     * point plus the destination warehouse's fixed coordinates, not
     * stored redundantly here.
     */
    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "position_updated_at")
    private Instant positionUpdatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_warehouse_id")
    private com.agribid.nexus.domain.logistics.Warehouse destinationWarehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FulfillmentStatus status = FulfillmentStatus.PENDING;

    public OrderFulfillment(Order order, BigDecimal trancheQuantityKg) {
        this.order = order;
        this.trancheQuantityKg = trancheQuantityKg;
    }

    public void updatePosition(Double latitude, Double longitude) {
        this.currentLatitude = latitude;
        this.currentLongitude = longitude;
        this.positionUpdatedAt = Instant.now();
        if (this.status == FulfillmentStatus.PENDING) {
            this.status = FulfillmentStatus.IN_TRANSIT;
        }
    }

    public void markDelivered() {
        this.status = FulfillmentStatus.DELIVERED;
        this.deliveredAt = Instant.now();
    }
}