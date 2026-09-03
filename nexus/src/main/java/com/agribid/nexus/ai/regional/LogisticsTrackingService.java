package com.agribid.nexus.ai.regional;

import com.agribid.nexus.domain.contract.OrderFulfillment;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.repository.OrderFulfillmentRepository;
import com.agribid.nexus.util.GeoUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * ETA here is a real, honest straight-line calculation — distance
 * remaining divided by an assumed average road speed — not a
 * traffic-aware routing engine. Stated plainly rather than implying
 * more precision than this system actually has.
 */
@Service
public class LogisticsTrackingService {

    private static final double ASSUMED_AVG_SPEED_KMH = 35.0;

    private final OrderFulfillmentRepository fulfillmentRepository;

    public LogisticsTrackingService(OrderFulfillmentRepository fulfillmentRepository) {
        this.fulfillmentRepository = fulfillmentRepository;
    }

    @Transactional
    public OrderFulfillment updatePosition(Long fulfillmentId, Double latitude, Double longitude) {
        OrderFulfillment fulfillment = fulfillmentRepository.findById(fulfillmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Fulfillment not found: " + fulfillmentId));
        fulfillment.updatePosition(latitude, longitude);
        return fulfillmentRepository.save(fulfillment);
    }

    public Map<String, Object> getEta(Long fulfillmentId) {
        OrderFulfillment fulfillment = fulfillmentRepository.findById(fulfillmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Fulfillment not found: " + fulfillmentId));

        if (fulfillment.getCurrentLatitude() == null || fulfillment.getDestinationWarehouse() == null
                || fulfillment.getDestinationWarehouse().getLatitude() == null) {
            return Map.of(
                    "fulfillmentId", fulfillmentId, "etaAvailable", false,
                    "note", "No current position and/or destination warehouse coordinates on file yet."
            );
        }

        double distanceKm = GeoUtils.haversineKm(
                fulfillment.getCurrentLatitude(), fulfillment.getCurrentLongitude(),
                fulfillment.getDestinationWarehouse().getLatitude(), fulfillment.getDestinationWarehouse().getLongitude());
        double etaHours = distanceKm / ASSUMED_AVG_SPEED_KMH;

        return Map.of(
                "fulfillmentId", fulfillmentId, "etaAvailable", true,
                "remainingDistanceKm", Math.round(distanceKm * 10) / 10.0,
                "estimatedHoursRemaining", Math.round(etaHours * 10) / 10.0,
                "positionUpdatedAt", fulfillment.getPositionUpdatedAt(),
                "note", "Straight-line distance divided by an assumed average road speed of " + ASSUMED_AVG_SPEED_KMH
                        + " km/h — not a traffic-aware routing estimate."
        );
    }
}
