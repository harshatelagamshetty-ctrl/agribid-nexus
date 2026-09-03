package com.agribid.nexus.domain.user;

import com.agribid.nexus.domain.auction.Bid;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "distributor_profiles")
@DiscriminatorValue("DISTRIBUTOR")
@Getter
@Setter
@NoArgsConstructor
public class DistributorProfile extends User {

    @Column(name = "business_license_number", nullable = false, unique = true)
    private String businessLicenseNumber;

    @Column(name = "warehouse_region")
    private String warehouseRegion;

    @OneToMany(mappedBy = "bidder", fetch = FetchType.LAZY)
    private List<Bid> bids = new ArrayList<>();

    public DistributorProfile(String email, String passwordHash, String businessLicenseNumber, String warehouseRegion) {
        super(email, passwordHash, Role.DISTRIBUTOR);
        this.businessLicenseNumber = businessLicenseNumber;
        this.warehouseRegion = warehouseRegion;
    }
}