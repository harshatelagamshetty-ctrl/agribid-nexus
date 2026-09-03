package com.agribid.nexus.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FpoPoolCreateRequest(

        @NotBlank(message = "categoryCode is required")
        String categoryCode,

        @NotNull(message = "targetQuantityKg is required")
        @DecimalMin(value = "0.01", message = "targetQuantityKg must be positive")
        BigDecimal targetQuantityKg

) {
}
