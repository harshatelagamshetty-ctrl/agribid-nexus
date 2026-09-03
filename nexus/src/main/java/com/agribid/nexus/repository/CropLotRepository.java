package com.agribid.nexus.repository;

import com.agribid.nexus.domain.crop.CropLot;
import com.agribid.nexus.domain.crop.LotStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CropLotRepository extends JpaRepository<CropLot, Long> {
    java.util.Optional<CropLot> findByOfflineIdempotencyKey(String offlineIdempotencyKey);
    Page<CropLot> findByOwnerIdAndStatus(Long ownerId, LotStatus status, Pageable pageable);
    Page<CropLot> findByOwnerId(Long ownerId, Pageable pageable);

    /**
     * Backs duplicate-video detection in EvidenceAssessmentService —
     * checks whether this exact file hash was ever used for a
     * DIFFERENT lot (excludes the current lot's own row, since
     * re-assessing the same lot shouldn't flag itself).
     */
    boolean existsByVideoHashAndIdNot(String videoHash, Long id);
}