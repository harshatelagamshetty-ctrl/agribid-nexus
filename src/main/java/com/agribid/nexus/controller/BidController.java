package com.agribid.nexus.controller;

import com.agribid.nexus.dto.request.BidRequest;
import com.agribid.nexus.dto.response.BidResponse;
import com.agribid.nexus.dto.response.BidStreamPage;
import com.agribid.nexus.security.UserPrincipal;
import com.agribid.nexus.service.BidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Every endpoint here sits behind the KycAuthorizationManager wired
 * into SecurityConfig for the exact URI pattern
 * /api/v1/listings/*\/bids — an unverified distributor's request
 * never reaches this class at all.
 */
@RestController
@RequestMapping("/api/v1/listings/{listingId}/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @PostMapping
    public ResponseEntity<BidResponse> placeBid(
            @PathVariable Long listingId,
            @Valid @RequestBody BidRequest request,
            @AuthenticationPrincipal UserPrincipal distributor) {
        // path listingId always wins over the body, same rationale as
        // BidListingController.publish()
        BidRequest safeRequest = new BidRequest(listingId, request.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(bidService.placeBid(safeRequest, distributor));
    }

    /**
     * Keyset-paginated live bid stream (see repository/BidRepository
     * and service/impl/BidServiceImpl for the OFFSET-avoidance
     * rationale). cursor is the nextCursor from the previous
     * response's BidStreamPage; omit it to fetch the most recent page.
     */
    @GetMapping
    public ResponseEntity<BidStreamPage> streamBids(
            @PathVariable Long listingId,
            @RequestParam(required = false) Instant cursor,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(bidService.streamBids(listingId, cursor, size));
    }
}