package com.agribid.nexus.domain.crop;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single AI-detected risk marker (blight, aphid damage, fungal
 * spotting, etc). Modeled as a genuine entity — rather than a
 * denormalized comma-separated string on CropLot — so each tag is
 * independently queryable/indexable for regional pest-outbreak
 * analytics.
 */
@Entity
@Table(name = "pest_tags")
@Getter
@Setter
@NoArgsConstructor
public class PestTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // e.g. "BLIGHT", "APHID"

    @Column(nullable = false)
    private String label;

    private String severityDefault;

    public PestTag(String code, String label, String severityDefault) {
        this.code = code;
        this.label = label;
        this.severityDefault = severityDefault;
    }
}