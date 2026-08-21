package com.agribid.nexus.ai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Backs Gemini's live-MSP grounding during negotiation. Queried
 * directly via JdbcTemplate against a small reference table
 * (msp_rates: crop_code, region, price_per_kg, effective_date)
 * rather than a full JPA entity/repository stack, since this is
 * read-only reference data seeded by a Flyway migration, not part of
 * the transactional auction domain model.
 */
@Component
public class MspLookupTool {

    private final JdbcTemplate jdbcTemplate;

    public MspLookupTool(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns null when no MSP rate is on record for this exact
     * crop/region combination, rather than throwing. Real MSP data
     * will never cover every crop x every region — using
     * queryForObject() here (as an earlier version of this method
     * did) throws EmptyResultDataAccessException on a zero-row match,
     * which propagates straight through Gemini's tool-calling loop
     * and breaks the ENTIRE negotiation response, not just this one
     * fact. A null return lets the model say "I don't have MSP data
     * for that combination" instead of the whole request failing.
     */
    @Tool(description = "Fetch the current government Minimum Support Price (MSP) per kg for a crop in a given region. Returns null if no rate is on record for that exact crop/region combination — if so, say so rather than guessing a price.")
    public BigDecimal getLiveMSP(
            @ToolParam(description = "Crop category code, e.g. WHEAT, TOMATO") String cropCode,
            @ToolParam(description = "Region or state name, e.g. PUNJAB") String region) {

        List<BigDecimal> results = jdbcTemplate.query(
                """
                SELECT price_per_kg FROM msp_rates
                WHERE crop_code = ? AND region = ?
                ORDER BY effective_date DESC
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getBigDecimal("price_per_kg"),
                cropCode == null ? null : cropCode.trim().toUpperCase(),
                region == null ? null : region.trim().toUpperCase()
        );

        return results.isEmpty() ? null : results.get(0);
    }
}