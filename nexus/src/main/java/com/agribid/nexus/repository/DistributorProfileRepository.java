package com.agribid.nexus.repository;

import com.agribid.nexus.domain.user.DistributorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DistributorProfileRepository extends JpaRepository<DistributorProfile, Long> {
    Optional<DistributorProfile> findByEmail(String email);
    Optional<DistributorProfile> findByBusinessLicenseNumber(String licenseNumber);
}