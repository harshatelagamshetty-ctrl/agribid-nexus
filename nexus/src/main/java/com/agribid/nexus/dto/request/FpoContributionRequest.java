package com.agribid.nexus.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FpoContributionRequest(

        @NotNull(message = "cropLotId is required")
        Long cropLotId,

        @NotNull(message = "quantityKg is required")
        @DecimalMin(value = "0.01", message = "quantityKg must be positive")
        BigDecimal quantityKg

) {
}
