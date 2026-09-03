package com.agribid.nexus.controller;

import com.agribid.nexus.dto.response.ForwardContractResponse;
import com.agribid.nexus.service.ForwardContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ForwardContractController {

    private final ForwardContractService forwardContractService;

    @GetMapping("/{contractId}")
    public ResponseEntity<ForwardContractResponse> getContract(@PathVariable Long contractId) {
        return ResponseEntity.ok(forwardContractService.getContract(contractId));
    }

    @PostMapping("/{contractId}/orders")
    public ResponseEntity<Map<String, Long>> createOrder(@PathVariable Long contractId) {
        Long orderId = forwardContractService.createOrder(contractId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("orderId", orderId));
    }
}