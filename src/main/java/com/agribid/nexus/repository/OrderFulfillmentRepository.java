package com.agribid.nexus.repository;

import com.agribid.nexus.domain.contract.FulfillmentStatus;
import com.agribid.nexus.domain.contract.OrderFulfillment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderFulfillmentRepository extends JpaRepository<OrderFulfillment, Long> {
    Page<OrderFulfillment> findByOrderId(Long orderId, Pageable pageable);
    Page<OrderFulfillment> findByStatus(FulfillmentStatus status, Pageable pageable);
}