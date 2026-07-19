package com.agribid.nexus.repository;

import com.agribid.nexus.domain.logistics.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    List<Warehouse> findByRegion(String region);
}