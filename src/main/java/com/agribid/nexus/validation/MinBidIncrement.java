package com.agribid.nexus.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rejects bids under the mandated tick size at the DTO boundary,
 * before the transactional core is ever burdened with invalid
 * contention. Applied at the class level of BidRequest since the
 * check needs both the submitted amount and the target listing's
 * current highest bid.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MinBidIncrementValidator.class)
public @interface MinBidIncrement {
    String message() default "Bid must exceed the current highest bid by the minimum tick size";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
