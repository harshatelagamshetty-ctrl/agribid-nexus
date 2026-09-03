package com.agribid.nexus.ai.logistics;

import com.agribid.nexus.ai.logistics.model.RoutePlan;
import com.agribid.nexus.dto.request.RouteOptimizeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Distributor- and admin-facing: a distributor who has won several
 * nearby lots (individually, or via one FPO pooled lot's contributing
 * farmers before pickup) uses this to plan the actual collection run.
 */
@RestController
@RequestMapping("/api/v1/logistics/routes")
@RequiredArgsConstructor
public class RouteOptimizationController {

    private final RouteOptimizationService routeOptimizationService;

    @PostMapping("/optimize")
    @PreAuthorize("hasRole('DISTRIBUTOR') or hasRole('ADMIN')")
    public ResponseEntity<RoutePlan> optimize(@Valid @RequestBody RouteOptimizeRequest request) {
        return ResponseEntity.ok(routeOptimizationService.optimizePickupRoute(request.warehouseId(), request.cropLotIds()));
    }
}
