package com.agribid.nexus.controller;

import com.agribid.nexus.ai.regional.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/farmers/{farmerId}/transaction-statement")
    public ResponseEntity<Map<String, Object>> getTransactionStatement(@PathVariable Long farmerId) {
        return ResponseEntity.ok(reportingService.getFarmerTransactionStatement(farmerId));
    }

    @GetMapping("/price-trend")
    public ResponseEntity<Map<String, Object>> getPriceTrend(
            @RequestParam String district, @RequestParam Long categoryId,
            @RequestParam(defaultValue = "6") int weeks) {
        return ResponseEntity.ok(reportingService.getPriceTrendData(district, categoryId, weeks));
    }

    @GetMapping("/disputes/{disputeId}/standard-format")
    @PreAuthorize("hasRole('AGRONOMIST') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> exportDisputeFormat(@PathVariable Long disputeId) {
        return ResponseEntity.ok(reportingService.exportDisputeInStandardFormat(disputeId));
    }
}
