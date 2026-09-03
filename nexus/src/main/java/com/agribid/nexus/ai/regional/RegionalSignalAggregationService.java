package com.agribid.nexus.ai.regional;

import com.agribid.nexus.domain.crop.CropLot;
import com.agribid.nexus.domain.crop.CropLotEvidenceReport;
import com.agribid.nexus.domain.regional.RegionalSignal;
import com.agribid.nexus.repository.RegionalSignalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The entire trust-filtering mechanism AgriPulse is built on lives
 * in exactly one place: the caller. This class has no evidence-tier
 * logic of its own — it trusts that whoever invokes
 * recordVerifiedSubmission() has already checked the evidence report
 * is HIGH or MEDIUM. See EvidenceAssessmentService.assess(), which
 * is the actual call site that decides whether a submission is
 * trustworthy enough to reach this aggregator at all. A LOW or
 * NEEDS_REVIEW submission simply never calls this class — it cannot
 * distort the regional signal by construction, not by a runtime
 * check that could be bypassed or forgotten.
 */
@Service
public class RegionalSignalAggregationService {

    private static final java.util.Map<String, Double> GRADE_SCORE = java.util.Map.of(
            "A", 3.0, "B", 2.0, "C", 1.0
    );

    private final RegionalSignalRepository regionalSignalRepository;

    public RegionalSignalAggregationService(RegionalSignalRepository regionalSignalRepository) {
        this.regionalSignalRepository = regionalSignalRepository;
    }

    @Transactional
    public void recordVerifiedSubmission(CropLot lot, CropLotEvidenceReport report) {
        String district = lot.getOwner().getDistrict();
        if (district == null || lot.getCategory() == null) {
            return; // nothing meaningful to aggregate without both
        }

        RegionalSignal signal = currentWeekSignal(district, lot.getCategory().getId());
        signal.setVerifiedSubmissionCount(signal.getVerifiedSubmissionCount() + 1);

        if (lot.getQualityGrade() != null) {
            Double newScore = GRADE_SCORE.getOrDefault(lot.getQualityGrade().getGradeLabel(), null);
            if (newScore != null) {
                double priorTotal = (signal.getAvgQualityScore() == null ? 0 : signal.getAvgQualityScore())
                        * (signal.getVerifiedSubmissionCount() - 1);
                signal.setAvgQualityScore((priorTotal + newScore) / signal.getVerifiedSubmissionCount());
            }
        }

        signal.setTotalVerifiedQuantityKg(signal.getTotalVerifiedQuantityKg().add(lot.getQuantityKg()));

        for (String pestCode : lot.getPestTags().stream().map(t -> t.getCode()).toList()) {
            appendPestOccurrence(signal, pestCode);
        }

        signal.setLastUpdatedAt(java.time.Instant.now());
        regionalSignalRepository.save(signal);
    }

    /**
     * Called separately from recordVerifiedSubmission — price data
     * only exists once a listing actually settles (a bid is accepted
     * into a contract), which happens well after evidence
     * verification and grading. Keeping these as two independent
     * update paths avoids forcing a fake, premature price into the
     * signal at submission time.
     */
    @Transactional
    public void recordSettledPrice(CropLot lot, BigDecimal settledPricePerKg) {
        String district = lot.getOwner().getDistrict();
        if (district == null || lot.getCategory() == null || settledPricePerKg == null) {
            return;
        }
        RegionalSignal signal = currentWeekSignal(district, lot.getCategory().getId());
        BigDecimal priorTotal = (signal.getAvgSettledPricePerKg() == null ? BigDecimal.ZERO : signal.getAvgSettledPricePerKg())
                .multiply(BigDecimal.valueOf(signal.getSettledTransactionCount()));
        signal.setSettledTransactionCount(signal.getSettledTransactionCount() + 1);
        signal.setAvgSettledPricePerKg(
                priorTotal.add(settledPricePerKg).divide(BigDecimal.valueOf(signal.getSettledTransactionCount()), 2, RoundingMode.HALF_UP)
        );
        signal.setLastUpdatedAt(java.time.Instant.now());
        regionalSignalRepository.save(signal);
    }

    private RegionalSignal currentWeekSignal(String district, Long categoryId) {
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return regionalSignalRepository.findByDistrictAndCategoryIdAndWeekStart(district, categoryId, weekStart)
                .orElseGet(() -> {
                    RegionalSignal fresh = new RegionalSignal();
                    fresh.setDistrict(district);
                    fresh.setWeekStart(weekStart);
                    return fresh;
                });
    }

    /**
     * Comma-separated storage, not a join table — deliberately. This
     * only ever needs to answer "how many distinct verified
     * submissions reported pest X this week in this district," which
     * a simple delimited string plus a split-and-count at read time
     * (see OutbreakDetectionService) answers correctly without the
     * schema overhead of a full child table for what is, at this
     * scale, a small, bounded weekly list.
     */
    private void appendPestOccurrence(RegionalSignal signal, String pestCode) {
        String current = signal.getPestTagOccurrences();
        String updated = (current == null || current.isBlank()) ? pestCode : current + "," + pestCode;
        if (updated.length() <= 2000) {
            signal.setPestTagOccurrences(updated);
        }
        // silently stops appending past the column limit rather than
        // throwing — a week with an extremely high pest-report volume
        // degrades to "count is a floor, not exact" rather than
        // failing the whole submission it's attached to
    }

    static Set<String> distinctPestCodes(String occurrences) {
        if (occurrences == null || occurrences.isBlank()) return new LinkedHashSet<>();
        return new LinkedHashSet<>(Arrays.asList(occurrences.split(",")));
    }
}
