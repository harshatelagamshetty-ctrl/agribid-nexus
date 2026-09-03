package com.agribid.nexus.repository;

import com.agribid.nexus.ai.evidence.model.ReviewStatus;
import com.agribid.nexus.domain.crop.CropLotEvidenceReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CropLotEvidenceReportRepository extends JpaRepository<CropLotEvidenceReport, Long> {
    Optional<CropLotEvidenceReport> findByCropLotId(Long cropLotId);
    Page<CropLotEvidenceReport> findByReviewStatus(ReviewStatus reviewStatus, Pageable pageable);
    List<CropLotEvidenceReport> findByCropLot_Owner_Id(Long farmerId);
}
