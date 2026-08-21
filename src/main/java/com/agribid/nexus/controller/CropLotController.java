package com.agribid.nexus.controller;

import com.agribid.nexus.ai.pricing.ReservePriceAdvisorService;
import com.agribid.nexus.ai.pricing.model.ReservePriceSuggestion;
import com.agribid.nexus.ai.vision.CropGradingService;
import com.agribid.nexus.dto.mapper.CropLotMapper;
import com.agribid.nexus.dto.request.CropLotCreateRequest;
import com.agribid.nexus.dto.response.CropLotResponse;
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

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<CropLotResponse> create(
            @Valid @RequestBody CropLotCreateRequest request,
            @AuthenticationPrincipal UserPrincipal farmer) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cropLotService.createLot(request, farmer));
    }

    /**
     * Ownership is enforced twice here, deliberately: @lotSecurity.isOwner
     * rejects the request at the method-security boundary before this
     * method body runs, and CropLotServiceImpl re-checks ownership
     * again before mutating — defense in depth rather than relying on
     * a single layer to never have a bug.
     */
    @PostMapping(value = "/{lotId}/image", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('FARMER') and @lotSecurity.isOwner(#lotId, principal)")
    public ResponseEntity<CropLotResponse> attachImage(
            @PathVariable Long lotId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal UserPrincipal farmer) {
        return ResponseEntity.ok(cropLotService.attachImage(lotId, file, farmer));
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