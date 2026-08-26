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
    private QualityGrade qualityGrade;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "crop_lot_pest_tag",
            joinColumns = @JoinColumn(name = "crop_lot_id"),
            inverseJoinColumns = @JoinColumn(name = "pest_tag_id")
    )
    private Set<PestTag> pestTags = new HashSet<>();

    @Column(name = "quantity_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityKg;

    /**
     * Replaces the old single-photo imageUrl. Populated only via
     * CropLotServiceImpl.attachVideo(), which enforces capture
     * freshness and requires GPS coordinates alongside the file —
     * grading can't proceed on a video with no location/timestamp
     * evidence.
     */
    @Column(name = "video_url")
    private String videoUrl;

    /**
     * GPS coordinates captured at the moment of recording, submitted
     * by the client alongside the video upload — not derived from
     * the video file itself (video files don't reliably carry GPS
     * EXIF the way photos do). Trust in these values is only as
     * strong as the client honestly reporting them; this is a
     * deterrent layer, not a cryptographic guarantee.
     */
    @Column(name = "capture_latitude")
    private Double captureLatitude;

    @Column(name = "capture_longitude")
    private Double captureLongitude;

    /**
     * Client-reported capture timestamp. CropLotServiceImpl rejects
     * uploads where this is more than a few minutes old, closing off
     * "reuse an old, better-looking video" as a strategy.
     */
    @Column(name = "captured_at")
    private Instant capturedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LotStatus status = LotStatus.DRAFT;

    @OneToMany(mappedBy = "cropLot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BidListing> listings = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public CropLot(FarmerProfile owner, Category category, BigDecimal quantityKg) {
        this.owner = owner;
        this.category = category;
        this.quantityKg = quantityKg;
    }

    public void applyGrading(QualityGrade grade, Set<PestTag> detectedTags) {
        this.qualityGrade = grade;
        this.pestTags = detectedTags;
        this.status = LotStatus.GRADED;
    }
}