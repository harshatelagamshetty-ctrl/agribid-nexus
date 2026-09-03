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
     * SHA-256 hex digest of the uploaded video file, computed
     * server-side at attach time. Backs duplicate-submission
     * detection: a byte-identical video reused for a different lot
     * is inherently suspicious (there's no legitimate reason two
     * distinct harvest lots would produce an identical file), unlike
     * GPS or timestamp similarity which can have innocent
     * explanations.
     */
    @Column(name = "video_hash", length = 64)
    private String videoHash;

    /**
     * The registered Field this video's GPS is checked against.
     * Nullable so existing lots (captured before Field existed)
     * don't break — new submissions are expected to always supply
     * one via the /video endpoint, and CropLotEvidenceReport records
     * NOT_REGISTERED explicitly when it's missing rather than
     * silently skipping the check.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id")
    private Field field;

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

    /**
     * Optional GPS samples taken periodically WHILE recording, not
     * just the single start point above — see SpatialCoverageChecker
     * for what this defends against. Encoded via GpsTrackCodec, not
     * a real JSON library (see that class's Javadoc for why). Null
     * for clients that don't yet capture a track — never penalized
     * as if that were suspicious.
     */
    @Column(name = "capture_track_encoded", columnDefinition = "TEXT")
    private String captureTrackEncoded;

    /**
     * Both optional and self-reported by the farmer — deliberately
     * not verified by any evidence-engine signal, since there is no
     * honest way to verify actual water/pesticide usage from a video
     * alone. The "Low Input" badge below is presented to buyers as
     * exactly that: a farmer's own declaration, not a certified
     * claim.
     */
    @Column(name = "self_reported_low_water_usage")
    private Boolean selfReportedLowWaterUsage;

    @Column(name = "self_reported_low_pesticide_usage")
    private Boolean selfReportedLowPesticideUsage;

    /**
     * Set only when the client explicitly signals this video was
     * captured offline and queued for later sync — see
     * CropLotServiceImpl.attachVideo(). Never set silently; a normal
     * online submission always has this null/false, and the
     * freshness check stays strict for it. Offline submissions
     * honestly trade some of that strictness for accessibility, and
     * this field makes that trade visible in the evidence report
     * rather than hiding it.
     */
    @Column(name = "was_offline_capture")
    private Boolean wasOfflineCapture;

    /**
     * Client-generated once, at the moment of capture (while still
     * offline) — not server-generated. If the same key arrives twice
     * (a dropped-connection retry after the first sync actually
     * succeeded), the second request is recognized as a duplicate
     * and the original result is returned, not a second lot.
     */
    @Column(name = "offline_idempotency_key", unique = true)
    private String offlineIdempotencyKey;

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