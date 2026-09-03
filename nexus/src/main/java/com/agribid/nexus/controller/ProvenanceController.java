package com.agribid.nexus.controller;

import com.agribid.nexus.ai.regional.ProvenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Deliberately public — the real-world use case is a buyer or
 * end-consumer scanning a QR code on packaging, who is not logged
 * into the platform at all.
 */
@RestController
@RequestMapping("/api/v1/provenance")
@RequiredArgsConstructor
public class ProvenanceController {

    private final ProvenanceService provenanceService;

    @GetMapping("/crop-lots/{cropLotId}")
    public ResponseEntity<Map<String, Object>> getCropPassport(@PathVariable Long cropLotId) {
        return ResponseEntity.ok(provenanceService.getCropPassport(cropLotId));
    }
}
