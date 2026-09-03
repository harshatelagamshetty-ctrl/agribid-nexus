package com.agribid.nexus.domain.regional;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One row per (district, category, week). This is a denormalized
 * read-model, deliberately — it exists purely so regional queries
 * (price benchmark, pest signal, supply outlook) are fast lookups
 * against a small aggregate table, not expensive scans across every
 * CropLotEvidenceReport and BidListing in the system every time a
 * farmer opens a listing.
 *
 * Populated incrementally by RegionalSignalAggregationService,
 * triggered only by HIGH/MEDIUM evidence submissions and settled
 * transactions — this is the entire trust-filtering mechanism
 * AgriPulse is built on: a LOW-evidence or NEEDS_REVIEW submission
 * never reaches this table, so it can never distort what other
 * farmers see.
 */
@Entity
@Table(name = "regional_signals", uniqueConstraints = @UniqueConstraint(columnNames = {"district", "category_id", "week_start"}))
@Getter
@Setter
@NoArgsConstructor
public class RegionalSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String district;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private com.agribid.nexus.domain.crop.Category category;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "verified_submission_count", nullable = false)
    private int verifiedSubmissionCount = 0;

    @Column(name = "avg_quality_score")
    private Double avgQualityScore;

    @Column(name = "total_verified_quantity_kg", precision = 14, scale = 2)
    private BigDecimal totalVerifiedQuantityKg = BigDecimal.ZERO;

    @Column(name = "avg_settled_price_per_kg", precision = 12, scale = 2)
    private BigDecimal avgSettledPricePerKg;

    @Column(name = "settled_transaction_count", nullable = false)
    private int settledTransactionCount = 0;

    /**
     * Comma-separated pest tag codes seen this week in this district
     * for this category, one entry per distinct verified submission
     * that reported it — deliberately simple storage (see
     * OutbreakDetectionService for why a real join table wasn't
     * necessary for the threshold rule this backs).
     */
    @Column(name = "pest_tag_occurrences", length = 2000)
    private String pestTagOccurrences = "";

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt = Instant.now();

    public RegionalSignal(String district, com.agribid.nexus.domain.crop.Category category, LocalDate weekStart) {
        this.district = district;
        this.category = category;
        this.weekStart = weekStart;
    }
}
