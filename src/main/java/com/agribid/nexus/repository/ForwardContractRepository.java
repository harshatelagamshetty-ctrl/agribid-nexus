package com.agribid.nexus.repository;

import com.agribid.nexus.domain.contract.ForwardContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForwardContractRepository extends JpaRepository<ForwardContract, Long> {
    Optional<ForwardContract> findBySourceListingId(Long listingId);
}