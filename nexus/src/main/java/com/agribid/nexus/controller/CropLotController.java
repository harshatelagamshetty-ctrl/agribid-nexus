package com.agribid.nexus.controller;

import com.agribid.nexus.ai.evidence.LivenessChallengeService;
import com.agribid.nexus.ai.pricing.ReservePriceAdvisorService;
import com.agribid.nexus.ai.pricing.model.ReservePriceSuggestion;
import com.agribid.nexus.ai.vision.CropGradingService;
import com.agribid.nexus.dto.mapper.CropLotEvidenceReportMapper;
import com.agribid.nexus.dto.mapper.CropLotMapper;
import com.agribid.nexus.dto.request.CropLotCreateRequest;
import com.agribid.nexus.dto.response.CropLotEvidenceReportResponse;
import com.agribid.nexus.dto.response.CropLotResponse;
import com.agribid.nexus.dto.response.VideoChallengeResponse;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.repository.CropLotEvidenceReportRepository;
import com.agribid.nexus.security.UserPrincipal;
import com.agribid.nexus.service.CropLotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crop-lots")
@RequiredArgsConstructor
public class CropLotController {

    private final CropLotService cropLotService;
    private final CropGradingService cropGradingService;
    private final ReservePriceAdvisorService reservePriceAdvisorService;
    private final CropLotEvidenceReportRepository evidenceReportRepository;
    private final LivenessChallengeService livenessChallengeService;

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<CropLotResponse> create(
            @Valid @RequestBody CropLotCreateRequest request,
            @AuthenticationPrincipal UserPrincipal farmer) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cropLotService.createLot(request, farmer));
    }

    /**
     * Issues a short-lived (10 minute), randomly generated liveness
     * challenge — a spoken code or hand gesture the farmer must
     * perform while recording. This is the actual defense against a
     * pre-recorded or AI-generated video: neither could have
     * anticipated an instruction that didn't exist until this call
     * returns. Verified against the submitted video inside
     * EvidenceAssessmentService when /video is called next.
     */
    @PostMapping(value = "/{lotId}/video-challenge")
    @PreAuthorize("hasRole('FARMER') and @lotSecurity.isOwner(#lotId, principal)")
    public ResponseEntity<VideoChallengeResponse> issueVideoChallenge(@PathVariable Long lotId) {
        var issued = livenessChallengeService.issueChallenge(lotId);
        return ResponseEntity.ok(new VideoChallengeResponse(issued.displayInstruction(), issued.expiresAt()));
    }

    /**
     * Replaces the old /image endpoint. capturedAt is submitted as
     * an ISO-8601 string form field alongside the file and GPS
     * coordinates — Spring binds it straight to Instant. fieldId
     * must reference a Field this farmer already registered via
     * POST /api/v1/fields. Calling /video-challenge first is
     * optional, deliberately: a farmer without a stable connection
     * or an older phone can skip straight to this endpoint — the
     * evidence report still gets produced, just without the
     * liveness signal contributing to it (see
     * EvidenceAssessmentService).
     *
     * gpsTrack is also optional: a delimited string of GPS samples
     * taken periodically WHILE recording (format documented on
     * GpsTrackCodec), used by SpatialCoverageChecker to confirm the
     * farmer actually moved across a meaningful portion of the real
     * field rather than filming one stationary spot. Omitting it
     * degrades that one signal to NOT_AVAILABLE, capping the
     * submission at MEDIUM evidence rather than HIGH — never treated
     * as suspicious on its own.
     */
    @PostMapping(value = "/{lotId}/video", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('FARMER') and @lotSecurity.isOwner(#lotId, principal)")
    public ResponseEntity<CropLotResponse> attachVideo(
            @PathVariable Long lotId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("fieldId") Long fieldId,
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam("capturedAt") java.time.Instant capturedAt,
            @RequestParam(value = "gpsTrack", required = false) String gpsTrack,
            @RequestParam(value = "offlineCapture", defaultValue = "false") boolean offlineCapture,
            @RequestParam(value = "offlineIdempotencyKey", required = false) String offlineIdempotencyKey,
            @AuthenticationPrincipal UserPrincipal farmer) {
        return ResponseEntity.ok(cropLotService.attachVideo(lotId, file, fieldId, latitude, longitude, capturedAt, gpsTrack, offlineCapture, offlineIdempotencyKey, farmer));
    }

    /**
     * The full explainable evidence report — every signal, laid out
     * individually, plus the composed overall verdict. This is the
     * exact answer to "why did you verify this crop?": no field on
     * this response is a black-box number, every one traces to one
     * specific, named check in EvidenceAssessmentService.
     */
    @GetMapping("/{lotId}/evidence-report")
    @PreAuthorize("hasRole('FARMER') and @lotSecurity.isOwner(#lotId, principal)")
    public ResponseEntity<CropLotEvidenceReportResponse> getEvidenceReport(@PathVariable Long lotId) {
        var report = evidenceReportRepository.findByCropLotId(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("No evidence report yet for crop lot " + lotId + " — attach a video first"));
        return ResponseEntity.ok(CropLotEvidenceReportMapper.toResponse(report));
    }

    /**
     * Ownership is enforced entirely by @PreAuthorize's
     * @lotSecurity.isOwner(#lotId, principal) check below — nothing
     * else in this method needs to re-verify it. An earlier version
     * of this endpoint also called cropLotService.gradeLot() purely
     * to re-check ownership and then discarded the result; that was
     * a redundant DB fetch checking the same thing @PreAuthorize
     * already guarantees, so it was removed.
     */
    @PostMapping("/{lotId}/grade")
    @PreAuthorize("hasRole('FARMER') and @lotSecurity.isOwner(#lotId, principal)")
    public ResponseEntity<CropLotResponse> gradeLot(@PathVariable Long lotId) {
        var gradedLot = cropGradingService.gradeCropLot(lotId);
        return ResponseEntity.ok(CropLotMapper.toResponse(gradedLot));
    }

    @GetMapping("/{lotId}/reserve-price-suggestion")
    @PreAuthorize("hasRole('FARMER') and @lotSecurity.isOwner(#lotId, principal)")
    public ResponseEntity<ReservePriceSuggestion> suggestReservePrice(@PathVariable Long lotId) {
        return ResponseEntity.ok(reservePriceAdvisorService.suggestReservePrice(lotId));
    }

    @GetMapping("/{lotId}")
    public ResponseEntity<CropLotResponse> getLot(@PathVariable Long lotId) {
        return ResponseEntity.ok(cropLotService.getLot(lotId));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<Page<CropLotResponse>> getMyLots(
            @AuthenticationPrincipal UserPrincipal farmer,
            @org.springframework.data.web.PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(cropLotService.getLotsForFarmer(farmer.getId(), pageable));
    }
}