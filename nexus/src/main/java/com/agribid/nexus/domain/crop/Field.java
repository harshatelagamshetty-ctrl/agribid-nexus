package com.agribid.nexus.domain.crop;

import com.agribid.nexus.domain.user.FarmerProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Registered once per physical plot, reused across many crop lots.
 * Deliberately a single GPS point + radius, not a polygon — a
 * polygon-capture UI is real additional frontend work (walking the
 * perimeter, drawing on a map) that doesn't materially improve the
 * fraud-resistance story enough to justify the cost at this scope.
 * A radius check answers the actual question that matters here
 * ("is this video's GPS near a field this farmer actually
 * registered?"), not "what is this field's exact shape?"
 */
@Entity
@Table(name = "fields")
@Getter
@Setter
@NoArgsConstructor
public class Field {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id", nullable = false)
    private FarmerProfile owner;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    /**
     * Deliberately generous default (500m) rather than a tight
     * radius: GPS accuracy on consumer phones is commonly 5-50m but
     * can degrade to 100m+ under poor signal, and the field itself
     * has real physical extent. A tight radius produces false
     * MISMATCH flags on legitimate submissions; a generous one still
     * catches "video shot at an entirely different location."
     */
    @Column(name = "radius_meters", nullable = false)
    private Double radiusMeters = 500.0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public Field(FarmerProfile owner, String fieldName, Double latitude, Double longitude, Double radiusMeters) {
        this.owner = owner;
        // A genuinely blank name isn't left as null — that would
        // render as an empty label in any UI listing a farmer's
        // fields. A coordinate-based fallback is still meaningful
        // and distinguishes multiple unnamed fields from each other.
        this.fieldName = (fieldName != null && !fieldName.isBlank())
                ? fieldName
                : "Field near %.4f, %.4f".formatted(latitude, longitude);
        this.latitude = latitude;
        this.longitude = longitude;
        if (radiusMeters != null) {
            this.radiusMeters = radiusMeters;
        }
    }
}
