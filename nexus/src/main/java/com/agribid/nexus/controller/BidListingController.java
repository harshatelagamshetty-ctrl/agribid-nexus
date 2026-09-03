package com.agribid.nexus.controller;

import com.agribid.nexus.dto.request.BidListingCreateRequest;
import com.agribid.nexus.dto.request.ListingFilterRequest;
import com.agribid.nexus.dto.response.BidListingResponse;
import com.agribid.nexus.dto.response.ForwardContractResponse;
import com.agribid.nexus.domain.auction.AuctionStatus;
import com.agribid.nexus.security.UserPrincipal;
import com.agribid.nexus.service.BidListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequiredArgsConstructor
public class BidListingController {

    private final BidListingService bidListingService;

    /**
     * lotId comes from the path, not the request body, specifically
     * so @lotSecurity.isOwner(#lotId, principal) can enforce ownership
     * at the method-security boundary. The path lotId always wins
     * over anything the client puts in the body — we rebuild the
     * request DTO here rather than trusting request.cropLotId(), so
     * there's no way for a client to pass a lotId in the URL that
     * differs from the one actually listed.
     */
    @PostMapping("/api/v1/crop-lots/{lotId}/listings")
    @PreAuthorize("hasRole('FARMER') and @lotSecurity.isOwner(#lotId, principal)")
    public ResponseEntity<BidListingResponse> publish(
            @PathVariable Long lotId,
            @Valid @RequestBody BidListingCreateRequest request,
            @AuthenticationPrincipal UserPrincipal farmer) {
        BidListingCreateRequest safeRequest = new BidListingCreateRequest(
                lotId, request.reservePrice(), request.auctionCloseTime());
        return ResponseEntity.status(HttpStatus.CREATED).body(bidListingService.publishListing(safeRequest, farmer));
    }

    /**
     * Public read endpoint (see SecurityConfig: GET /api/v1/listings/**
     * is permitAll) — browsing the live auction board doesn't require
     * authentication, only bidding does.
     */
    @GetMapping("/api/v1/listings")
    public ResponseEntity<Page<BidListingResponse>> search(
            @RequestParam(required = false) String cropTypeCode,
            @RequestParam(required = false) BigDecimal minQuantityKg,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Instant closingBefore,
            @RequestParam(required = false) AuctionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ListingFilterRequest filter = new ListingFilterRequest(
                cropTypeCode, minQuantityKg, district, closingBefore, status, page, size);
        return ResponseEntity.ok(bidListingService.search(filter));
    }

    @GetMapping("/api/v1/listings/{listingId}")
    public ResponseEntity<BidListingResponse> getListing(@PathVariable Long listingId) {
        return ResponseEntity.ok(bidListingService.getListing(listingId));
    }

    @PostMapping("/api/v1/listings/{listingId}/contract")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ForwardContractResponse> convertToContract(
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserPrincipal farmer) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bidListingService.convertToContract(listingId, farmer));
    }
}