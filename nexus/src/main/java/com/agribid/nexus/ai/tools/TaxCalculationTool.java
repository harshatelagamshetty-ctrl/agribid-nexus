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

    private static final Map<String, BigDecimal> STATE_MANDI_TAX_RATES = Map.ofEntries(
            Map.entry("ANDHRA PRADESH", new BigDecimal("0.010")),
            Map.entry("ARUNACHAL PRADESH", new BigDecimal("0.005")),
            Map.entry("ASSAM", new BigDecimal("0.008")),
            Map.entry("BIHAR", new BigDecimal("0.012")),
            Map.entry("CHHATTISGARH", new BigDecimal("0.010")),
            Map.entry("GOA", new BigDecimal("0.005")),
            Map.entry("GUJARAT", new BigDecimal("0.009")),
            Map.entry("HARYANA", new BigDecimal("0.020")),
            Map.entry("HIMACHAL PRADESH", new BigDecimal("0.006")),
            Map.entry("JHARKHAND", new BigDecimal("0.011")),
            Map.entry("KARNATAKA", new BigDecimal("0.012")),
            Map.entry("KERALA", new BigDecimal("0.008")),
            Map.entry("MADHYA PRADESH", new BigDecimal("0.015")),
            Map.entry("MAHARASHTRA", new BigDecimal("0.010")),
            Map.entry("MANIPUR", new BigDecimal("0.005")),
            Map.entry("MEGHALAYA", new BigDecimal("0.005")),
            Map.entry("MIZORAM", new BigDecimal("0.005")),
            Map.entry("NAGALAND", new BigDecimal("0.005")),
            Map.entry("ODISHA", new BigDecimal("0.010")),
            Map.entry("PUNJAB", new BigDecimal("0.020")),
            Map.entry("RAJASTHAN", new BigDecimal("0.012")),
            Map.entry("SIKKIM", new BigDecimal("0.005")),
            Map.entry("TAMIL NADU", new BigDecimal("0.010")),
            Map.entry("TELANGANA", new BigDecimal("0.010")),
            Map.entry("TRIPURA", new BigDecimal("0.005")),
            Map.entry("UTTAR PRADESH", new BigDecimal("0.015")),
            Map.entry("UTTARAKHAND", new BigDecimal("0.008")),
            Map.entry("WEST BENGAL", new BigDecimal("0.010"))
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