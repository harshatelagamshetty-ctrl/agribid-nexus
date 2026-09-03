package com.agribid.nexus.domain.crop;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // e.g. "TOMATO", "WHEAT"

    @Column(nullable = false)
    private String name;

    private String description;

    /**
     * Typical harvest window for this crop, as a month number (1-12).
     * Deliberately a simple month range rather than requiring the
     * farmer to declare a sowing date — that would add a required
     * input with its own failure modes (farmers misremembering or
     * guessing). This instead asks a lighter, still-useful question:
     * "is this category typically harvested around now?" Nullable
     * because not every category needs this check to be meaningful
     * (e.g. crops harvested year-round) — a null value means the
     * seasonality check reports UNKNOWN rather than a false failure.
     */
    @Column(name = "typical_harvest_start_month")
    private Integer typicalHarvestStartMonth;

    @Column(name = "typical_harvest_end_month")
    private Integer typicalHarvestEndMonth;

    public Category(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    /**
     * Handles wrap-around windows (e.g. a harvest season spanning
     * Nov-Feb, where start > end numerically) correctly.
     */
    public boolean isWithinHarvestSeason(int month) {
        if (typicalHarvestStartMonth == null || typicalHarvestEndMonth == null) {
            return true; // no window configured — nothing to contradict
        }
        if (typicalHarvestStartMonth <= typicalHarvestEndMonth) {
            return month >= typicalHarvestStartMonth && month <= typicalHarvestEndMonth;
        }
        return month >= typicalHarvestStartMonth || month <= typicalHarvestEndMonth;
    }
}