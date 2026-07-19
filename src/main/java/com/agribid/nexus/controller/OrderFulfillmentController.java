package com.agribid.nexus.controller;

import com.agribid.nexus.dto.response.OrderFulfillmentResponse;
import com.agribid.nexus.security.UserPrincipal;
import com.agribid.nexus.service.OrderFulfillmentService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
public class OrderFulfillmentController {

    private final OrderFulfillmentService orderFulfillmentService;

    public record FulfillmentRequest(
            @NotNull @DecimalMin(value = "0.01") BigDecimal trancheQuantityKg
    ) {}

    @PostMapping("/api/v1/orders/{orderId}/fulfillments")
    public ResponseEntity<OrderFulfillmentResponse> recordFulfillment(
            @PathVariable Long orderId,
            @jakarta.validation.Valid @RequestBody FulfillmentRequest request,
            @AuthenticationPrincipal UserPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderFulfillmentService.recordFulfillment(orderId, request.trancheQuantityKg(), actor));
    }

    @PatchMapping("/api/v1/fulfillments/{fulfillmentId}/deliver")
    public ResponseEntity<OrderFulfillmentResponse> markDelivered(
            @PathVariable Long fulfillmentId,
            @AuthenticationPrincipal UserPrincipal actor) {
        return ResponseEntity.ok(orderFulfillmentService.markDelivered(fulfillmentId, actor));
    }

    @GetMapping("/api/v1/orders/{orderId}/fulfillments")
    public ResponseEntity<Page<OrderFulfillmentResponse>> getFulfillments(
            @PathVariable Long orderId,
            Pageable pageable) {
        return ResponseEntity.ok(orderFulfillmentService.getFulfillmentsForOrder(orderId, pageable));
    }
}