package com.agribid.nexus.ai.regional;

import com.agribid.nexus.domain.contract.Order;
import com.agribid.nexus.domain.crop.CropLot;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.repository.CropLotRepository;
import com.agribid.nexus.repository.ForwardContractRepository;
import com.agribid.nexus.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A public, read-only chain-of-custody view built entirely from data
 * already persisted elsewhere — no new capture, no new entity beyond
 * this composition. Reachable via a QR code on physical packaging in
 * the intended real-world flow, though generating and printing that
 * QR code is outside this backend's own scope.
 *
 * Privacy design, stated directly: this returns the farmer's
 * DISTRICT, never their exact field GPS coordinates — a buyer or
 * anyone scanning a package learns real provenance without learning
 * exactly where a specific farmer's field is.
 */
@Service
public class ProvenanceService {

    private final CropLotRepository cropLotRepository;
    private final OrderRepository orderRepository;

    public ProvenanceService(CropLotRepository cropLotRepository, OrderRepository orderRepository) {
        this.cropLotRepository = cropLotRepository;
        this.orderRepository = orderRepository;
    }

    public Map<String, Object> getCropPassport(Long cropLotId) {
        CropLot lot = cropLotRepository.findById(cropLotId)
                .orElseThrow(() -> new ResourceNotFoundException("Crop lot not found: " + cropLotId));

        Optional<Order> order = orderRepository.findByCropLotId(cropLotId);

        var chain = new java.util.ArrayList<Map<String, Object>>();
        chain.add(Map.of("stage", "HARVESTED", "detail", "Lot created: " + lot.getCategory().getName() + ", " + lot.getQuantityKg() + " kg"));
        if (lot.getVideoUrl() != null) {
            chain.add(Map.of("stage", "VIDEO_VERIFIED", "detail", "Video walkthrough submitted " + lot.getCapturedAt()));
        }
        if (lot.getQualityGrade() != null) {
            chain.add(Map.of("stage", "GRADED", "detail", "AI grade: " + lot.getQualityGrade().getGradeLabel()));
        }
        order.ifPresent(o -> {
            chain.add(Map.of("stage", "CONTRACTED", "detail", "Locked at Rs." + o.getContract().getLockedPrice() + "/kg"));
            o.getFulfillments().forEach(f ->
                    chain.add(Map.of("stage", "FULFILLMENT_" + f.getStatus(),
                            "detail", f.getTrancheQuantityKg() + " kg tranche, status " + f.getStatus())));
        });

        // Map.of() throws NullPointerException on any null value —
        // district has no required-field validation at registration
        // (see RegisterRequest.java), so a real farmer account can
        // genuinely have a null district.
        Map<String, Object> passport = new java.util.LinkedHashMap<>();
        passport.put("cropLotId", lot.getId());
        passport.put("category", lot.getCategory().getName());
        passport.put("originDistrict", lot.getOwner().getDistrict()); // district only — never exact field GPS
        passport.put("qualityGrade", lot.getQualityGrade() != null ? lot.getQualityGrade().getGradeLabel() : "Not yet graded");
        passport.put("lowInputBadge", Boolean.TRUE.equals(lot.getSelfReportedLowWaterUsage()) && Boolean.TRUE.equals(lot.getSelfReportedLowPesticideUsage()));
        passport.put("custodyChain", chain);
        passport.put("note", "Origin is shown at district level only — exact field location is never exposed in a public passport view.");
        return passport;
    }
}
