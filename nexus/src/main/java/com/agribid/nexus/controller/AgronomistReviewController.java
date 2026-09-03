package com.agribid.nexus.controller;

import com.agribid.nexus.dto.mapper.CropLotEvidenceReportMapper;
import com.agribid.nexus.dto.response.CropLotEvidenceReportResponse;
import com.agribid.nexus.security.UserPrincipal;
import com.agribid.nexus.service.impl.AgronomistReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * The actual, previously-missing use of ROLE_AGRONOMIST: a lot whose
 * evidence report came out NEEDS_REVIEW or LOW is blocked from
 * listing (see BidListingServiceImpl.publishListing) until an
 * agronomist reviews it here. A farmer whose evidence checks out
 * cleanly never touches this flow at all.
 */
@RestController
@RequestMapping("/api/v1/agronomist")
@RequiredArgsConstructor
public class AgronomistReviewController {

    private final AgronomistReviewService agronomistReviewService;

    public record ReviewDecisionRequest(boolean approve, String note) {}

    @GetMapping("/review-queue")
    @PreAuthorize("hasRole('AGRONOMIST')")
    public ResponseEntity<Page<CropLotEvidenceReportResponse>> getReviewQueue(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(agronomistReviewService.getReviewQueue(pageable)
                .map(CropLotEvidenceReportMapper::toResponse));
    }

    @PostMapping("/evidence-reports/{reportId}/review")
    @PreAuthorize("hasRole('AGRONOMIST')")
    public ResponseEntity<CropLotEvidenceReportResponse> review(
            @PathVariable Long reportId,
            @RequestBody ReviewDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal agronomist) {
        var report = agronomistReviewService.recordDecision(reportId, request.approve(), request.note(), agronomist);
        return ResponseEntity.ok(CropLotEvidenceReportMapper.toResponse(report));
    }
}
