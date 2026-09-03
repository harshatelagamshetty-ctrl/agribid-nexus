package com.agribid.nexus.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CropLotCreateRequest(

        @NotBlank(message = "categoryCode is required")
        String categoryCode,

        @NotNull(message = "quantityKg is required")
        @DecimalMin(value = "1.0", message = "quantityKg must be at least 1")
        BigDecimal quantityKg,

        // Both optional and self-reported — see CropLot.java for why
        // these are never treated as verified facts
        Boolean selfReportedLowWaterUsage,
        Boolean selfReportedLowPesticideUsage

        // imageUrl is deliberately excluded here — it's populated by the
        // File Upload endpoint in a separate multipart request, then
        // attached to the lot via CropLotService.attachImage(...)
) {
}