package com.agribid.nexus.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RegionalPriceBenchmarkResponse(
    String district,
    String categoryCode,
    LocalDate weekStart,
    BigDecimal avgSettledPricePerKg,
    int settledTransactionCount,
    String note
) {
}
