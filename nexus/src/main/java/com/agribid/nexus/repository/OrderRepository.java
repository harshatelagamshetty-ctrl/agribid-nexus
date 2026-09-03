package com.agribid.nexus.repository;

import com.agribid.nexus.domain.contract.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByContractId(Long contractId);

    @Query("SELECT o FROM Order o WHERE o.contract.sourceListing.cropLot.id = :cropLotId")
    Optional<Order> findByCropLotId(Long cropLotId);
}