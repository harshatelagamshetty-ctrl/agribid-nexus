package com.agribid.nexus.controller;

import com.agribid.nexus.integration.ExternalIntegrationGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
public class ExternalIntegrationController {

    private final ExternalIntegrationGateway gateway;

    @PostMapping("/escrow/{contractId}")
    public ResponseEntity<ExternalIntegrationGateway.EscrowResult> initiateEscrow(
            @PathVariable Long contractId, @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(gateway.initiateEscrow(contractId, amount));
    }

    @GetMapping("/government-mandi-price")
    public ResponseEntity<ExternalIntegrationGateway.MandiPriceLookup> getGovernmentMandiPrice(
            @RequestParam String cropCode, @RequestParam String market) {
        return ResponseEntity.ok(gateway.lookupGovernmentMandiPrice(cropCode, market));
    }

    @GetMapping("/kcc-eligibility/{farmerId}")
    public ResponseEntity<ExternalIntegrationGateway.KccEligibilitySignal> getKccSignpost(@PathVariable Long farmerId) {
        return ResponseEntity.ok(gateway.getKccEligibilitySignpost(farmerId));
    }
}
