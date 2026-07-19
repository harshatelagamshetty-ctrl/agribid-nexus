package com.agribid.nexus.repository;

import com.agribid.nexus.domain.user.FarmerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FarmerProfileRepository extends JpaRepository<FarmerProfile, Long> {
    Optional<FarmerProfile> findByEmail(String email);
}