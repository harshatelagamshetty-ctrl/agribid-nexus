package com.agribid.nexus.controller;

import com.agribid.nexus.ai.regional.ReputationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reputation")
@RequiredArgsConstructor
public class ReputationController {

    private final ReputationService reputationService;

    @GetMapping("/farmers/{farmerId}")
    public ResponseEntity<ReputationService.FarmerTrustScore> getFarmerTrustScore(@PathVariable Long farmerId) {
        return ResponseEntity.ok(reputationService.getFarmerTrustScore(farmerId));
    }

    @GetMapping("/distributors/{distributorId}")
    public ResponseEntity<ReputationService.DistributorReliabilityScore> getDistributorReliability(@PathVariable Long distributorId) {
        return ResponseEntity.ok(reputationService.getDistributorReliabilityScore(distributorId));
    }
}
