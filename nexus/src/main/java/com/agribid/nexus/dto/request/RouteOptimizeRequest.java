package com.agribid.nexus.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RouteOptimizeRequest(

        @NotNull(message = "warehouseId is required")
        Long warehouseId,

        @NotEmpty(message = "cropLotIds must contain at least one crop lot")
        List<Long> cropLotIds

) {
}
