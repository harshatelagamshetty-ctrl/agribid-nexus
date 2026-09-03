package com.agribid.nexus.service.impl;

import com.agribid.nexus.ai.evidence.model.ReviewStatus;
import com.agribid.nexus.domain.crop.CropLotEvidenceReport;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.repository.CropLotEvidenceReportRepository;
import com.agribid.nexus.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * The activation of ROLE_AGRONOMIST for its actual intended purpose:
 * the role existed in the schema (auth, security config) since much
 * earlier in the project but had no endpoint that actually let an
 * agronomist review anything. This closes that gap directly rather
 * than inventing a parallel review mechanism — a NEEDS_REVIEW or LOW
 * evidence report is exactly the trigger this role was always meant
 * to respond to.
 */
@Service
public class AgronomistReviewService {

    private final CropLotEvidenceReportRepository evidenceReportRepository;

    public AgronomistReviewService(CropLotEvidenceReportRepository evidenceReportRepository) {
        this.evidenceReportRepository = evidenceReportRepository;
    }

    public Page<CropLotEvidenceReport> getReviewQueue(Pageable pageable) {
        return evidenceReportRepository.findByReviewStatus(ReviewStatus.PENDING, pageable);
    }

    @Transactional
    public CropLotEvidenceReport recordDecision(Long reportId, boolean approve, String note, UserPrincipal agronomist) {
        CropLotEvidenceReport report = evidenceReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence report not found: " + reportId));

        if (report.getReviewStatus() != ReviewStatus.PENDING) {
            throw new IllegalStateException(
                    "Evidence report " + reportId + " is not pending review (current status: " + report.getReviewStatus() + ")");
        }

        report.setReviewStatus(approve ? ReviewStatus.APPROVED : ReviewStatus.REJECTED);
        report.setReviewedBy(agronomist.getId());
        report.setReviewedAt(Instant.now());
        report.setReviewNote(note);

        return evidenceReportRepository.save(report);
    }
}
