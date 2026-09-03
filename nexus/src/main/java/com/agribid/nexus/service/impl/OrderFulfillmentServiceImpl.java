package com.agribid.nexus.service.impl;

import com.agribid.nexus.domain.contract.Order;
import com.agribid.nexus.domain.contract.OrderFulfillment;
import com.agribid.nexus.dto.mapper.ForwardContractMapper;
import com.agribid.nexus.dto.response.OrderFulfillmentResponse;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.repository.OrderFulfillmentRepository;
import com.agribid.nexus.repository.OrderRepository;
import com.agribid.nexus.security.UserPrincipal;
import com.agribid.nexus.service.OrderFulfillmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Role authorization for fulfillment endpoints (which distributor
 * roles may record deliveries against which contracts) is enforced
 * at the SecurityConfig route-matcher layer, not re-derived here —
 * this service assumes the caller has already cleared that gate and
 * focuses purely on the tranche-delivery state machine.
 */
@Service
@RequiredArgsConstructor
public class OrderFulfillmentServiceImpl implements OrderFulfillmentService {

    private final OrderFulfillmentRepository orderFulfillmentRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderFulfillmentResponse recordFulfillment(Long orderId, BigDecimal trancheQuantityKg, UserPrincipal actor) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        OrderFulfillment fulfillment = new OrderFulfillment(order, trancheQuantityKg);
        orderFulfillmentRepository.save(fulfillment);
        return ForwardContractMapper.toResponse(fulfillment);
    }

    @Override
    @Transactional
    public OrderFulfillmentResponse markDelivered(Long fulfillmentId, UserPrincipal actor) {
        OrderFulfillment fulfillment = orderFulfillmentRepository.findById(fulfillmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Fulfillment not found: " + fulfillmentId));

        fulfillment.markDelivered();
        return ForwardContractMapper.toResponse(fulfillment);
    }

    @Override
    public Page<OrderFulfillmentResponse> getFulfillmentsForOrder(Long orderId, Pageable pageable) {
        return orderFulfillmentRepository.findByOrderId(orderId, pageable)
            .map(ForwardContractMapper::toResponse);
    }
}
