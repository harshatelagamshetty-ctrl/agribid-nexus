package com.agribid.nexus.ai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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

    @Tool(description = "Fetch the current government Minimum Support Price (MSP) per kg for a crop in a given region")
    public BigDecimal getLiveMSP(
            @ToolParam(description = "Crop category code, e.g. WHEAT, TOMATO") String cropCode,
            @ToolParam(description = "Region or state name, e.g. PUNJAB") String region) {

        return jdbcTemplate.queryForObject(
            """
            SELECT price_per_kg FROM msp_rates
            WHERE crop_code = ? AND region = ?
            ORDER BY effective_date DESC
            LIMIT 1
            """,
            BigDecimal.class,
            cropCode == null ? null : cropCode.trim().toUpperCase(),
            region == null ? null : region.trim().toUpperCase()
        );
    }
}