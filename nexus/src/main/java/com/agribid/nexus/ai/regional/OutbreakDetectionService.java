package com.agribid.nexus.ai.regional;

import com.agribid.nexus.domain.regional.RegionalSignal;
import com.agribid.nexus.repository.RegionalSignalRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deliberately a simple, explainable threshold rule — NOT a trained
 * epidemiological or machine-learning model. Claiming otherwise
 * would be exactly the kind of fabricated capability this project's
 * standing rule forbids. What this actually does: count distinct
 * verified (already trust-filtered, see RegionalSignalAggregationService)
 * submissions reporting the same pest tag in a district within the
 * last two weeks, and flag when that count crosses a stated,
 * adjustable threshold. Every number behind a flag is traceable and
 * inspectable — nothing here is a black-box score.
 */
@Service
public class OutbreakDetectionService {

    private static final int MIN_DISTINCT_REPORTS_TO_FLAG = 3;
    private static final int LOOKBACK_WEEKS = 2;

    private final RegionalSignalRepository regionalSignalRepository;

    public OutbreakDetectionService(RegionalSignalRepository regionalSignalRepository) {
        this.regionalSignalRepository = regionalSignalRepository;
    }

    public record PestSignal(String pestCode, int reportCount, String district, java.time.LocalDate weekStart) {}

    public List<PestSignal> getActiveSignals(String district, Long categoryId) {
        LocalDate cutoff = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .minusWeeks(LOOKBACK_WEEKS);

        List<RegionalSignal> recent = regionalSignalRepository
                .findByDistrictAndCategoryIdOrderByWeekStartDesc(district, categoryId)
                .stream()
                .filter(sig -> !sig.getWeekStart().isBefore(cutoff))
                .toList();

        Map<String, Integer> counts = new java.util.HashMap<>();
        java.util.Map<String, LocalDate> latestWeek = new java.util.HashMap<>();
        for (RegionalSignal sig : recent) {
            Set<String> codes = RegionalSignalAggregationService.distinctPestCodes(sig.getPestTagOccurrences());
            for (String code : codes) {
                counts.merge(code, 1, Integer::sum);
                latestWeek.merge(code, sig.getWeekStart(), (a, b) -> a.isAfter(b) ? a : b);
            }
        }

        return counts.entrySet().stream()
                .filter(e -> e.getValue() >= MIN_DISTINCT_REPORTS_TO_FLAG)
                .map(e -> new PestSignal(e.getKey(), e.getValue(), district, latestWeek.get(e.getKey())))
                .collect(Collectors.toList());
    }
}
