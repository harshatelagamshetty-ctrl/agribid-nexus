package com.agribid.nexus.controller;

import com.agribid.nexus.ai.regional.OutbreakDetectionService;
import com.agribid.nexus.domain.crop.Category;
import com.agribid.nexus.domain.regional.RegionalSignal;
import com.agribid.nexus.dto.response.PestSignalResponse;
import com.agribid.nexus.dto.response.RegionalPriceBenchmarkResponse;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.repository.CategoryRepository;
import com.agribid.nexus.repository.RegionalSignalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * AgriPulse's public surface. Every response here is built only from
 * RegionalSignal rows that RegionalSignalAggregationService already
 * trust-filtered at write time — nothing in this controller
 * re-checks evidence tiers, because by the time data reaches this
 * table it has already passed that filter.
 */
@RestController
@RequestMapping("/api/v1/regional-intelligence")
@RequiredArgsConstructor
public class RegionalIntelligenceController {

    private final RegionalSignalRepository regionalSignalRepository;
    private final CategoryRepository categoryRepository;
    private final OutbreakDetectionService outbreakDetectionService;

    @GetMapping("/price-benchmark")
    public ResponseEntity<RegionalPriceBenchmarkResponse> getPriceBenchmark(
            @RequestParam String district, @RequestParam String categoryCode) {
        Category category = categoryRepository.findByCode(categoryCode)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown category code: " + categoryCode));
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        RegionalSignal signal = regionalSignalRepository
                .findByDistrictAndCategoryIdAndWeekStart(district, category.getId(), weekStart)
                .orElse(null);

        if (signal == null || signal.getSettledTransactionCount() == 0) {
            return ResponseEntity.ok(new RegionalPriceBenchmarkResponse(
                    district, categoryCode, weekStart, null, 0,
                    "No settled transactions recorded for this district/category this week yet."));
        }

        return ResponseEntity.ok(new RegionalPriceBenchmarkResponse(
                district, categoryCode, weekStart, signal.getAvgSettledPricePerKg(), signal.getSettledTransactionCount(),
                "Averaged from " + signal.getSettledTransactionCount() + " real settled contract(s) this week."));
    }

    @GetMapping("/pest-signals")
    public ResponseEntity<PestSignalResponse> getPestSignals(
            @RequestParam String district, @RequestParam String categoryCode) {
        Category category = categoryRepository.findByCode(categoryCode)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown category code: " + categoryCode));

        List<PestSignalResponse.PestSignalItem> items = outbreakDetectionService
                .getActiveSignals(district, category.getId())
                .stream()
                .map(sig -> new PestSignalResponse.PestSignalItem(sig.pestCode(), sig.reportCount(), sig.weekStart()))
                .toList();

        return ResponseEntity.ok(new PestSignalResponse(
                district, categoryCode, items,
                "A simple threshold rule over verified submissions, not a trained epidemiological model. "
                        + "Treat as an early, informal signal worth checking in person, not a diagnosis."));
    }

    @GetMapping("/supply-outlook")
    public ResponseEntity<?> getSupplyOutlook(@RequestParam String district, @RequestParam String categoryCode) {
        Category category = categoryRepository.findByCode(categoryCode)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown category code: " + categoryCode));
        List<RegionalSignal> recent = regionalSignalRepository
                .findByDistrictAndCategoryIdOrderByWeekStartDesc(district, category.getId())
                .stream().limit(6).toList();

        BigDecimal totalQty = recent.stream()
                .map(RegionalSignal::getTotalVerifiedQuantityKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalSubmissions = recent.stream().mapToInt(RegionalSignal::getVerifiedSubmissionCount).sum();

        return ResponseEntity.ok(new java.util.LinkedHashMap<>() {{
            put("district", district);
            put("categoryCode", categoryCode);
            put("weeksIncluded", recent.size());
            put("totalVerifiedQuantityKg", totalQty);
            put("totalVerifiedSubmissions", totalSubmissions);
            put("note", "Built only from HIGH/MEDIUM-evidence verified submissions — never raw, unverified listings.");
        }});
    }
}
