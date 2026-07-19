package com.agribid.nexus.dto.request;

import com.agribid.nexus.validation.MinBidIncrement;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * MinBidIncrement is a class-level constraint: it needs both
 * listingId and amount together to compare the submitted bid against
 * the listing's currentHighestBid, so it can't live on a single field.
 */
@MinBidIncrement
public record BidRequest(

        @NotNull(message = "listingId is required")
        Long listingId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be positive")
        BigDecimal amount

) {
}