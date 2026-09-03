package com.agribid.nexus.repository;

import com.agribid.nexus.domain.contract.ForwardContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ForwardContractRepository extends JpaRepository<ForwardContract, Long> {
    Optional<ForwardContract> findBySourceListingId(Long listingId);

    @Query("SELECT c FROM ForwardContract c WHERE c.sourceListing.cropLot.owner.id = :farmerId ORDER BY c.deliveryDeadline DESC")
    List<ForwardContract> findByFarmerId(Long farmerId);
}