package com.agribid.nexus.controller;

import com.agribid.nexus.dto.request.FpoContributionRequest;
import com.agribid.nexus.dto.request.FpoPoolCreateRequest;
import com.agribid.nexus.dto.response.FpoPayoutResponse;
import com.agribid.nexus.dto.response.FpoPoolResponse;
import com.agribid.nexus.security.UserPrincipal;
import com.agribid.nexus.service.FpoPoolingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * FPO bargaining-power aggregation: lets several smallholders in the
 * same farmer-producer organization pool their already-graded lots
 * into one larger listing instead of each selling small volumes
 * individually into a weaker negotiating position.
 */
@RestController
@RequestMapping("/api/v1/fpo-pools")
@RequiredArgsConstructor
public class FpoPoolingController {

    private final FpoPoolingService fpoPoolingService;

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<FpoPoolResponse> createPool(
            @Valid @RequestBody FpoPoolCreateRequest request,
            @AuthenticationPrincipal UserPrincipal coordinator) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fpoPoolingService.createPool(request.categoryCode(), request.targetQuantityKg(), coordinator));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<List<FpoPoolResponse>> listOpenPoolsForMyFpo(@AuthenticationPrincipal UserPrincipal farmer) {
        return ResponseEntity.ok(fpoPoolingService.listOpenPoolsForMyFpo(farmer));
    }

    @PostMapping("/{poolId}/contributions")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<FpoPoolResponse> contribute(
            @PathVariable Long poolId,
            @Valid @RequestBody FpoContributionRequest request,
            @AuthenticationPrincipal UserPrincipal contributor) {
        return ResponseEntity.ok(fpoPoolingService.contribute(poolId, request.cropLotId(), request.quantityKg(), contributor));
    }

    @PostMapping("/{poolId}/aggregate")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<FpoPoolResponse> aggregate(
            @PathVariable Long poolId,
            @AuthenticationPrincipal UserPrincipal coordinator) {
        return ResponseEntity.ok(fpoPoolingService.aggregate(poolId, coordinator));
    }

    @GetMapping("/{poolId}/payout")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    public ResponseEntity<FpoPayoutResponse> getPayout(@PathVariable Long poolId) {
        return ResponseEntity.ok(fpoPoolingService.getPayoutBreakdown(poolId));
    }
}
