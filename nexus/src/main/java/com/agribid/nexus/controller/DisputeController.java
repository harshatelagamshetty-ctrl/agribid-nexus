package com.agribid.nexus.controller;

import com.agribid.nexus.domain.contract.Dispute;
import com.agribid.nexus.security.UserPrincipal;
import com.agribid.nexus.service.impl.DisputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;
    private final com.agribid.nexus.ai.regional.DisputeSuggestionService disputeSuggestionService;

    public record RaiseDisputeRequest(String reason) {}
    public record DisputeDecisionRequest(boolean approve, String note) {}

    @GetMapping("/api/v1/agronomist/disputes/{disputeId}/suggestion")
    @PreAuthorize("hasRole('AGRONOMIST')")
    public ResponseEntity<com.agribid.nexus.ai.regional.DisputeSuggestionService.DisputeSuggestion> getSuggestion(@PathVariable Long disputeId) {
        return ResponseEntity.ok(disputeSuggestionService.getSuggestion(disputeId));
    }

    @PostMapping("/api/v1/orders/{orderId}/disputes")
    public ResponseEntity<Dispute> raiseDispute(
            @PathVariable Long orderId, @RequestBody RaiseDisputeRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(disputeService.raiseDispute(orderId, request.reason(), user));
    }

    @GetMapping("/api/v1/agronomist/disputes")
    @PreAuthorize("hasRole('AGRONOMIST')")
    public ResponseEntity<Page<Dispute>> getPendingDisputes(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(disputeService.getPendingQueue(pageable));
    }

    @PostMapping("/api/v1/agronomist/disputes/{disputeId}/decision")
    @PreAuthorize("hasRole('AGRONOMIST')")
    public ResponseEntity<Dispute> decideDispute(
            @PathVariable Long disputeId, @RequestBody DisputeDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal agronomist) {
        return ResponseEntity.ok(disputeService.recordDecision(disputeId, request.approve(), request.note(), agronomist));
    }
}
