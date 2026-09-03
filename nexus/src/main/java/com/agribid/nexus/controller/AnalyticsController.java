package com.agribid.nexus.controller;

import com.agribid.nexus.ai.regional.AnalyticsService;
import com.agribid.nexus.ai.regional.ReputationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ReputationService reputationService;

    @GetMapping("/platform-transparency")
    public ResponseEntity<Map<String, Object>> getPlatformTransparency() {
        return ResponseEntity.ok(analyticsService.getPlatformTransparencyMetrics());
    }

    @GetMapping("/crop-lots/{lotId}/risk-view")
    @PreAuthorize("hasRole('AGRONOMIST') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSubmissionRiskView(@PathVariable Long lotId) {
        return ResponseEntity.ok(analyticsService.getSubmissionRiskView(lotId, reputationService));
    }

    @GetMapping("/crop-lots/{lotId}/audit-trail")
    public ResponseEntity<Map<String, Object>> getAuditTrail(@PathVariable Long lotId) {
        return ResponseEntity.ok(analyticsService.exportAuditTrail(lotId));
    }
}
