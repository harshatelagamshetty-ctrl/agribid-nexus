package com.agribid.nexus.domain.crop;

import com.agribid.nexus.domain.auction.BidListing;
import com.agribid.nexus.domain.user.FarmerProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The physical harvest asset. Deliberately decoupled from BidListing
 * (the market event) so a lot's provenance, grading history, and
 * pest-tag associations survive across multiple re-listing cycles.
 */
@Entity
@Table(name = "crop_lots")
@Getter
@Setter
@NoArgsConstructor
public class CropLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id", nullable = false)
    private FarmerProfile owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quality_grade_id")
    private QualityGrade qualityGrade; // set post-AI vision inspection

    /**
     * Many-to-Many: a single lot can carry multiple concurrent risk
     * markers detected by the vision pipeline.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "crop_lot_pest_tag",
            joinColumns = @JoinColumn(name = "crop_lot_id"),
            inverseJoinColumns = @JoinColumn(name = "pest_tag_id")
    )
    private Set<PestTag> pestTags = new HashSet<>();

    @Column(name = "quantity_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityKg;

    @Column(name = "image_url")
    private String imageUrl; // populated via File Upload module

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LotStatus status = LotStatus.DRAFT;

    /**
     * One-to-Many: allows a lot to be relisted across auction cycles
     * without losing its grading/provenance history.
     */
    @OneToMany(mappedBy = "cropLot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BidListing> listings = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public CropLot(FarmerProfile owner, Category category, BigDecimal quantityKg, String imageUrl) {
        this.owner = owner;
        this.category = category;
        this.quantityKg = quantityKg;
        this.imageUrl = imageUrl;
    }

    public void applyGrading(QualityGrade grade, Set<PestTag> detectedTags) {
        this.qualityGrade = grade;
        this.pestTags = detectedTags;
        this.status = LotStatus.GRADED;
    }
}