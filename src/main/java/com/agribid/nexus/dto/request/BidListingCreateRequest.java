package com.agribid.nexus.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record BidListingCreateRequest(

        @NotNull(message = "cropLotId is required")
        Long cropLotId,

        @NotNull(message = "reservePrice is required")
        @DecimalMin(value = "0.01", message = "reservePrice must be positive")
        BigDecimal reservePrice,

        @NotNull(message = "auctionCloseTime is required")
        @Future(message = "auctionCloseTime must be in the future")
        Instant auctionCloseTime

) {
}