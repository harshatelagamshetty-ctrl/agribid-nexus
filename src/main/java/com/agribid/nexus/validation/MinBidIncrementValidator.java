package com.agribid.nexus.validation;

import com.agribid.nexus.dto.request.BidRequest;
import com.agribid.nexus.repository.BidListingRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class MinBidIncrementValidator implements ConstraintValidator<MinBidIncrement, BidRequest> {

    private static final BigDecimal MIN_TICK_SIZE = new BigDecimal("50.00");

    private final BidListingRepository bidListingRepository;

    @Override
    public boolean isValid(BidRequest request, ConstraintValidatorContext context) {
        if (request == null || request.listingId() == null || request.amount() == null) {
            return true; // let @NotNull handle presence checks
        }

        return bidListingRepository.findById(request.listingId())
            .map(listing -> {
                BigDecimal floor = listing.getCurrentHighestBid() != null
                    ? listing.getCurrentHighestBid()
                    : listing.getReservePrice();
                BigDecimal requiredMinimum = floor.add(MIN_TICK_SIZE);
                return request.amount().compareTo(requiredMinimum) >= 0;
            })
            .orElse(true); // unknown listing surfaces as ResourceNotFoundException downstream, not a validation failure
    }
}
