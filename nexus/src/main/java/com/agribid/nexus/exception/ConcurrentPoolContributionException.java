package com.agribid.nexus.exception;

/**
 * Thrown by FpoPoolingServiceImpl when Hibernate's optimistic lock
 * check (the @Version column on FpoPooledLot) detects the client's
 * contribution or aggregation call was computed against pool state
 * that has since moved on. Surfaced as a 409 so the client can
 * safely retry against fresh state — same contract as
 * ConcurrentBidConflictException on the bidding side.
 */
public class ConcurrentPoolContributionException extends AgriBidException {
    public ConcurrentPoolContributionException(String message) {
        super(message);
    }
}
