package com.agribid.nexus.service;

import com.agribid.nexus.dto.response.ForwardContractResponse;

public interface ForwardContractService {

    ForwardContractResponse getContract(Long contractId);

    /**
     * Creates the executable Order for an ACTIVE contract. Kept
     * separate from BidListingService.convertToContract() so a
     * contract can exist before its order is created (e.g. pending
     * a farmer's delivery-window confirmation).
     */
    Long createOrder(Long contractId);
}
