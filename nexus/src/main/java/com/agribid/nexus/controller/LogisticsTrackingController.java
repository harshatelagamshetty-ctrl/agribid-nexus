package com.agribid.nexus.controller;

import com.agribid.nexus.ai.regional.LogisticsTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/fulfillments")
@RequiredArgsConstructor
public class LogisticsTrackingController {

    private final LogisticsTrackingService logisticsTrackingService;

    public record PositionUpdateRequest(Double latitude, Double longitude) {}

    @PostMapping("/{fulfillmentId}/position")
    public ResponseEntity<?> updatePosition(@PathVariable Long fulfillmentId, @RequestBody PositionUpdateRequest request) {
        return ResponseEntity.ok(logisticsTrackingService.updatePosition(fulfillmentId, request.latitude(), request.longitude()));
    }

    @GetMapping("/{fulfillmentId}/eta")
    public ResponseEntity<Map<String, Object>> getEta(@PathVariable Long fulfillmentId) {
        return ResponseEntity.ok(logisticsTrackingService.getEta(fulfillmentId));
    }
}
