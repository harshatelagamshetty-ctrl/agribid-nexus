package com.agribid.nexus.service.impl;

import com.agribid.nexus.domain.contract.ContractStatus;
import com.agribid.nexus.domain.contract.ForwardContract;
import com.agribid.nexus.domain.contract.Order;
import com.agribid.nexus.dto.mapper.ForwardContractMapper;
import com.agribid.nexus.dto.response.ForwardContractResponse;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.repository.ForwardContractRepository;
import com.agribid.nexus.repository.OrderRepository;
import com.agribid.nexus.service.ForwardContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ForwardContractServiceImpl implements ForwardContractService {

    private final ForwardContractRepository forwardContractRepository;
    private final OrderRepository orderRepository;

    @Override
    public ForwardContractResponse getContract(Long contractId) {
        return ForwardContractMapper.toResponse(findContractOrThrow(contractId));
    }

    @Override
    @Transactional
    public Long createOrder(Long contractId) {
        ForwardContract contract = findContractOrThrow(contractId);

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new IllegalStateException(
                "Contract " + contractId + " must be ACTIVE to create an order (current status: " + contract.getStatus() + ")");
        }

        // OneToOne unique constraint on Order.contract_id makes a second
        // createOrder() call for the same contract fail at the DB level,
        // mirroring the same atomicity pattern used for listing -> contract
        // conversion in BidListingServiceImpl.
        if (orderRepository.findByContractId(contractId).isPresent()) {
            throw new IllegalStateException("Contract " + contractId + " already has an order");
        }

        Order order = new Order(contract);
        orderRepository.save(order);
        return order.getId();
    }

    private ForwardContract findContractOrThrow(Long contractId) {
        return forwardContractRepository.findById(contractId)
            .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));
    }
}
