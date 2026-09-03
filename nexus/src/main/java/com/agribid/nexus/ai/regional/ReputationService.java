package com.agribid.nexus.ai.regional;

import com.agribid.nexus.ai.evidence.model.OverallEvidence;
import com.agribid.nexus.domain.contract.FulfillmentStatus;
import com.agribid.nexus.domain.contract.OrderFulfillment;
import com.agribid.nexus.repository.CropLotEvidenceReportRepository;
import com.agribid.nexus.repository.OrderFulfillmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Both scores here are deliberately simple ratios over real,
 * already-persisted history — not a trained model, not a black-box
 * weighting formula. Explainability was chosen over sophistication
 * on purpose: a farmer or distributor questioning their own score
 * can be given the exact numerator and denominator behind it.
 */
@Service
public class ReputationService {

    private final CropLotEvidenceReportRepository evidenceReportRepository;
    private final OrderFulfillmentRepository orderFulfillmentRepository;

    public ReputationService(CropLotEvidenceReportRepository evidenceReportRepository,
                              OrderFulfillmentRepository orderFulfillmentRepository) {
        this.evidenceReportRepository = evidenceReportRepository;
        this.orderFulfillmentRepository = orderFulfillmentRepository;
    }

    public record FarmerTrustScore(
            Long farmerId, int totalSubmissions, int highOrMediumCount,
            int lowOrNeedsReviewCount, double trustRatio) {}

    public record DistributorReliabilityScore(
            Long distributorId, int totalWonOrders, int deliveredOnTimeCount,
            int deliveredLateCount, int pendingCount, double onTimeRatio) {}

    public FarmerTrustScore getFarmerTrustScore(Long farmerId) {
        var reports = evidenceReportRepository.findByCropLot_Owner_Id(farmerId);
        long goodCount = reports.stream()
                .filter(r -> r.getOverallEvidence() == OverallEvidence.HIGH || r.getOverallEvidence() == OverallEvidence.MEDIUM)
                .count();
        long poorCount = reports.size() - goodCount;
        double ratio = reports.isEmpty() ? 0.0 : (double) goodCount / reports.size();
        return new FarmerTrustScore(farmerId, reports.size(), (int) goodCount, (int) poorCount, ratio);
    }

    public DistributorReliabilityScore getDistributorReliabilityScore(Long distributorId) {
        List<OrderFulfillment> fulfillments = orderFulfillmentRepository.findByWinningDistributorId(distributorId);

        int onTime = 0, late = 0, pending = 0;
        for (OrderFulfillment f : fulfillments) {
            if (f.getStatus() != FulfillmentStatus.DELIVERED) {
                pending++;
                continue;
            }
            var deadline = f.getOrder().getContract().getDeliveryDeadline();
            boolean wasOnTime = f.getDeliveredAt() != null
                    && !f.getDeliveredAt().atZone(java.time.ZoneOffset.UTC).toLocalDate().isAfter(deadline);
            if (wasOnTime) onTime++; else late++;
        }
        int decided = onTime + late;
        double ratio = decided == 0 ? 0.0 : (double) onTime / decided;
        return new DistributorReliabilityScore(distributorId, fulfillments.size(), onTime, late, pending, ratio);
    }
}
