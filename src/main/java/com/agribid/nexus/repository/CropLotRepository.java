package com.agribid.nexus.repository;

import com.agribid.nexus.domain.crop.CropLot;
import com.agribid.nexus.domain.crop.LotStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CropLotRepository extends JpaRepository<CropLot, Long> {
    Page<CropLot> findByOwnerIdAndStatus(Long ownerId, LotStatus status, Pageable pageable);
    Page<CropLot> findByOwnerId(Long ownerId, Pageable pageable);
}