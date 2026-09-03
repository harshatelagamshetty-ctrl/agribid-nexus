package com.agribid.nexus.service;

import com.agribid.nexus.dto.response.FpoPayoutResponse;
import com.agribid.nexus.dto.response.FpoPoolResponse;
import com.agribid.nexus.security.UserPrincipal;

import java.math.BigDecimal;
import java.util.List;

public interface FpoPoolingService {

    FpoPoolResponse createPool(String categoryCode, BigDecimal targetQuantityKg, UserPrincipal coordinator);

    List<FpoPoolResponse> listOpenPoolsForMyFpo(UserPrincipal farmer);

    FpoPoolResponse contribute(Long poolId, Long cropLotId, BigDecimal quantityKg, UserPrincipal contributor);

    FpoPoolResponse aggregate(Long poolId, UserPrincipal coordinator);

    FpoPayoutResponse getPayoutBreakdown(Long poolId);
}
