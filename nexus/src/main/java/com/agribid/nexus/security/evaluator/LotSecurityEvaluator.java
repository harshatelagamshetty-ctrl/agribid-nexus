package com.agribid.nexus.security.evaluator;

import com.agribid.nexus.repository.CropLotRepository;
import com.agribid.nexus.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Backs the @PreAuthorize("@lotSecurity.isOwner(#lotId, principal)")
 * SpEL expression used on crop-lot mutation endpoints. Ownership is
 * checked at the method-security boundary — before the method body
 * runs — so a farmer can never publish or modify a lot they don't
 * own, regardless of what the service layer does afterward.
 */
@Component("lotSecurity")
@RequiredArgsConstructor
public class LotSecurityEvaluator {

    private final CropLotRepository cropLotRepository;

    public boolean isOwner(Long lotId, UserPrincipal principal) {
        if (principal == null) {
            return false;
        }
        return cropLotRepository.findById(lotId)
                .map(lot -> lot.getOwner().getId().equals(principal.getId()))
                .orElse(false);
    }
}