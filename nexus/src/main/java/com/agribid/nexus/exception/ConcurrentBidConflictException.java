package com.agribid.nexus.exception;

/**
 * Thrown by BidServiceImpl when Hibernate's optimistic lock check
 * (the @Version column on BidListing) detects the client's bid was
 * computed against auction state that has since moved on. Surfaced
 * as a 409 so the client can safely retry against fresh state.
 */
public class ConcurrentBidConflictException extends AgriBidException {
    public ConcurrentBidConflictException(String message) {
        super(message);
    }
}