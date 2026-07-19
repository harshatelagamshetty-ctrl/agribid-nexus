package com.agribid.nexus.ai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Grounds the negotiation co-pilot's tax statements in real
 * computation rather than a plausible-sounding guess. Rates below
 * are simplified placeholders for the mandi/APMC transaction cess —
 * replace with a proper rate table (or a call to a government rate
 * API) before this goes anywhere near a real transaction.
 */
@Component
public class TaxCalculationTool {

    private static final Map<String, BigDecimal> STATE_MANDI_TAX_RATES = Map.of(
        "MAHARASHTRA", new BigDecimal("0.010"),
        "PUNJAB", new BigDecimal("0.020"),
        "UTTAR PRADESH", new BigDecimal("0.015"),
        "KARNATAKA", new BigDecimal("0.012")
    );

    private static final BigDecimal DEFAULT_RATE = new BigDecimal("0.010");

    @Tool(description = "Calculate the applicable mandi transaction tax for a given bid amount and Indian state")
    public BigDecimal calculateMandiTax(
            @ToolParam(description = "The bid or transaction amount in rupees") BigDecimal amount,
            @ToolParam(description = "The Indian state where the transaction occurs, e.g. MAHARASHTRA") String state) {

        BigDecimal rate = STATE_MANDI_TAX_RATES.getOrDefault(
            state == null ? "" : state.trim().toUpperCase(), DEFAULT_RATE);

        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}