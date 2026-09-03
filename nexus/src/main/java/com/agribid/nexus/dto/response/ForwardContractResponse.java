package com.agribid.nexus.dto.response;

import com.agribid.nexus.domain.contract.ContractStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ForwardContractResponse(
    Long id,
    Long sourceListingId,
    BigDecimal lockedPrice,
    LocalDate deliveryDeadline,
    ContractStatus status
) {
}