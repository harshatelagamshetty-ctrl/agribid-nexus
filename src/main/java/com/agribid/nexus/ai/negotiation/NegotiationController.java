package com.agribid.nexus.ai.negotiation;

import com.agribid.nexus.ai.negotiation.model.NegotiationMessage;
import com.agribid.nexus.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Gated to ROLE_DISTRIBUTOR + the same KYC filter-chain check as
 * bidding (this path also matches the KycAuthorizationManager's
 * regex isn't targeted here — this is a distinct endpoint, so it's
 * gated purely by hasRole('DISTRIBUTOR') via SecurityConfig's
 * /api/v1/bids/** style rule; see note below on the URI choice).
 */
@RestController
@RequestMapping("/api/v1/listings/{listingId}/negotiation")
@RequiredArgsConstructor
public class NegotiationController {

    private final NegotiationChatService negotiationChatService;

    @PostMapping
    @PreAuthorize("hasRole('DISTRIBUTOR')")
    public ResponseEntity<NegotiationMessage> chat(
            @PathVariable Long listingId,
            @RequestBody String message,
            @AuthenticationPrincipal UserPrincipal distributor) {
        return ResponseEntity.ok(negotiationChatService.send(listingId, distributor.getId(), message));
    }
}
