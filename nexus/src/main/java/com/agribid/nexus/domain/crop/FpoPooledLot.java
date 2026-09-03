package com.agribid.nexus.domain.crop;

import com.agribid.nexus.domain.user.FarmerProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Directly answers the "FPO aggregation" gap: smallholders rarely
 * move enough individual volume to negotiate seriously, but several
 * of them pooling one harvest window's output into a single listing
 * do. Deliberately does NOT reimplement auction mechanics — once
 * {@link #resultingCropLot} exists, it is an ordinary CropLot and
 * flows through the existing BidListing/Bid/ForwardContract/
 * OrderFulfillment pipeline unchanged. Payout is split back out
 * pro-rata across contributors afterward (see FpoPoolingService).
 */
@Entity
@Table(name = "fpo_pooled_lots")
@Getter
@Setter
@NoArgsConstructor
public class FpoPooledLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Matches FarmerProfile.fpoAffiliation. Kept as a plain string
     * rather than a separate FpoGroup entity/table — the affiliation
     * is already a first-class field farmers set at registration, so
     * this reuses it instead of introducing a second, competing
     * notion of "which FPO a farmer belongs to".
     */
    @Column(name = "fpo_name", nullable = false)
    private String fpoName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * The farmer who opened the pool. Owns the resulting aggregate
     * CropLot once aggregation happens — someone has to be the
     * single farmer-of-record the marketplace's existing ownership
     * model expects, and the opener is the natural choice.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coordinator_farmer_id", nullable = false)
    private FarmerProfile coordinatorFarmer;

    @Column(name = "target_quantity_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal targetQuantityKg;

    @Column(name = "aggregated_quantity_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal aggregatedQuantityKg = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PoolStatus status = PoolStatus.OPEN;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resulting_crop_lot_id")
    private CropLot resultingCropLot;

    @OneToMany(mappedBy = "pooledLot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FpoPooledLotContribution> contributions = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "aggregated_at")
    private Instant aggregatedAt;

    /**
     * Without this, two farmers contributing to the same pool at
     * the same instant produce a lost update on aggregatedQuantityKg
     * (both read the same starting value, both add their own
     * quantity to it, the later commit silently overwrites the
     * earlier one's effect) — and getPayoutBreakdown() uses that
     * exact field as the payout denominator, so a lost update there
     * is a real financial-correctness bug, not a cosmetic one. This
     * single column protects BOTH contribute() and aggregate(),
     * since both methods ultimately save() this same row — same
     * mechanism already proven on BidListing.
     */
    @Version
    @Column(name = "version")
    private Long version;

    public FpoPooledLot(String fpoName, Category category, FarmerProfile coordinatorFarmer, BigDecimal targetQuantityKg) {
        this.fpoName = fpoName;
        this.category = category;
        this.coordinatorFarmer = coordinatorFarmer;
        this.targetQuantityKg = targetQuantityKg;
    }

    public boolean isOpen() {
        return status == PoolStatus.OPEN;
    }

    public boolean hasReachedTarget() {
        return aggregatedQuantityKg.compareTo(targetQuantityKg) >= 0;
    }

    public void addContribution(BigDecimal quantityKg) {
        this.aggregatedQuantityKg = this.aggregatedQuantityKg.add(quantityKg);
    }

    public void markAggregated(CropLot resultingCropLot) {
        this.resultingCropLot = resultingCropLot;
        this.status = PoolStatus.AGGREGATED;
        this.aggregatedAt = Instant.now();
    }
}
