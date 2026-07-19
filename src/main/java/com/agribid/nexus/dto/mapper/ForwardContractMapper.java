package com.agribid.nexus.dto.mapper;

import com.agribid.nexus.domain.contract.ForwardContract;
import com.agribid.nexus.domain.contract.OrderFulfillment;
import com.agribid.nexus.dto.response.ForwardContractResponse;
import com.agribid.nexus.dto.response.OrderFulfillmentResponse;

public final class ForwardContractMapper {

    private ForwardContractMapper() {
    }

    public static ForwardContractResponse toResponse(ForwardContract contract) {
        return new ForwardContractResponse(
            contract.getId(),
            contract.getSourceListing().getId(),
            contract.getLockedPrice(),
            contract.getDeliveryDeadline(),
            contract.getStatus()
        );
    }

    public static OrderFulfillmentResponse toResponse(OrderFulfillment fulfillment) {
        return new OrderFulfillmentResponse(
            fulfillment.getId(),
            fulfillment.getOrder().getId(),
            fulfillment.getTrancheQuantityKg(),
            fulfillment.getDeliveredAt(),
            fulfillment.getStatus()
        );
    }
}