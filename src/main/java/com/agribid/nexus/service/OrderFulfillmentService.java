package com.agribid.nexus.service;

import com.agribid.nexus.dto.response.OrderFulfillmentResponse;
import com.agribid.nexus.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface OrderFulfillmentService {

    OrderFulfillmentResponse recordFulfillment(Long orderId, BigDecimal trancheQuantityKg, UserPrincipal logisticsPartner);

    OrderFulfillmentResponse markDelivered(Long fulfillmentId, UserPrincipal logisticsPartner);

    Page<OrderFulfillmentResponse> getFulfillmentsForOrder(Long orderId, Pageable pageable);
}
