package com.agribid.nexus.ai.pricing.model;

import java.math.BigDecimal;
import java.util.List;

public record ReservePriceSuggestion(
    BigDecimal recommendedPricePerKg,
    String rationale,
    List<String> citedSources
) {
}