package com.agribid.nexus.domain.user;

import com.agribid.nexus.domain.crop.CropLot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "farmer_profiles")
@DiscriminatorValue("FARMER")
@Getter
@Setter
@NoArgsConstructor
public class FarmerProfile extends User {

    @Column(name = "fpo_affiliation")
    private String fpoAffiliation;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String state;

    /**
     * One-to-Many: a single farmer produces multiple discrete,
     * independently gradeable harvest lots across a growing season.
     * cascade = ALL + orphanRemoval so a lot removed from a farmer's
     * collection is deleted, never left as an orphaned row.
     */
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CropLot> cropLots = new ArrayList<>();

    public FarmerProfile(String email, String passwordHash, String district, String state) {
        super(email, passwordHash, Role.FARMER);
        this.district = district;
        this.state = state;
    }
}