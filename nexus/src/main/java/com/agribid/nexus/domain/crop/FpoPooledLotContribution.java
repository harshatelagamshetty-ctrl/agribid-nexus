package com.agribid.nexus.domain.crop;

import com.agribid.nexus.domain.user.FarmerProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A CropLot may appear in at most one contribution, ever (enforced
 * by a unique constraint on crop_lot_id in V8) — that's what makes
 * per-contributor payout math well-defined later: every kg of the
 * aggregated lot traces back to exactly one contribution, so the
 * final settled price can be split pro-rata without ambiguity.
 */
@Entity
@Table(name = "fpo_pooled_lot_contributions")
@Getter
@Setter
@NoArgsConstructor
public class FpoPooledLotContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pooled_lot_id", nullable = false)
    private FpoPooledLot pooledLot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contributor_farmer_id", nullable = false)
    private FarmerProfile contributorFarmer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_lot_id", nullable = false)
    private CropLot cropLot;

    @Column(name = "contributed_quantity_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal contributedQuantityKg;

    @Column(name = "contributed_at", nullable = false)
    private Instant contributedAt = Instant.now();

    public FpoPooledLotContribution(FpoPooledLot pooledLot, FarmerProfile contributorFarmer, CropLot cropLot, BigDecimal contributedQuantityKg) {
        this.pooledLot = pooledLot;
        this.contributorFarmer = contributorFarmer;
        this.cropLot = cropLot;
        this.contributedQuantityKg = contributedQuantityKg;
    }
}
