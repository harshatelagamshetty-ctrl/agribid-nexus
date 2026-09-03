package com.agribid.nexus.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

/**
 * Centralized failure handling. Every AgriBidException subtype maps
 * to a typed, RFC-7807-compliant ProblemDetail response so clients
 * (or partner integrations) can programmatically distinguish failure
 * modes instead of parsing error strings.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientBidException.class)
    public ProblemDetail handleInsufficientBid(InsufficientBidException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Bid Below Minimum Increment");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(AuctionClosedException.class)
    public ProblemDetail handleAuctionClosed(AuctionClosedException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.GONE);
        pd.setTitle("Auction Closed");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(DuplicateBidException.class)
    public ProblemDetail handleDuplicateBid(DuplicateBidException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Duplicate Bid");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(ConcurrentBidConflictException.class)
    public ProblemDetail handleConcurrentBidConflict(ConcurrentBidConflictException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Auction State Changed — Please Retry");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(ConcurrentPoolContributionException.class)
    public ProblemDetail handleConcurrentPoolContribution(ConcurrentPoolContributionException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Pool State Changed — Please Retry");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockFailure(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Auction State Changed — Please Retry");
        pd.setDetail("Your bid was computed against stale auction state. Refresh and resubmit.");
        return pd;
    }

    @ExceptionHandler(InvalidPoolStateException.class)
    public ProblemDetail handleInvalidPoolState(InvalidPoolStateException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Invalid FPO Pool State");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Resource Not Found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ProblemDetail handleUnauthorized(UnauthorizedActionException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setTitle("Unauthorized Action");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation Failed");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> Map.of("field", f.getField(), "message", f.getDefaultMessage()))
                .toList();
        pd.setProperty("errors", errors);
        return pd;
    }

    /**
     * This used to unconditionally claim "concurrent update, please
     * retry" for ANY DataIntegrityViolationException — but that
     * exception is Spring's broad parent for every kind of
     * constraint violation (NOT NULL, unique, foreign key), not just
     * the narrow concurrent-insert race this was originally written
     * for. Claiming a specific cause we don't actually know is
     * actively misleading — a NOT NULL violation will fail identically
     * on every retry, so telling the client to retry is actively
     * wrong advice for that case. This now reports honestly instead.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Request Could Not Be Completed");
        pd.setDetail("This request violates a data constraint (for example, a required field was missing, "
                + "or this exact record already exists). It is not necessarily a timing/concurrency issue — "
                + "check the request body before retrying unchanged.");
        return pd;
    }

    @ExceptionHandler(com.agribid.nexus.integration.IntegrationNotConfiguredException.class)
    public ProblemDetail handleIntegrationNotConfigured(com.agribid.nexus.integration.IntegrationNotConfiguredException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_IMPLEMENTED);
        pd.setTitle("Integration Not Configured");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {

        ex.printStackTrace();   // Print the full stack trace

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle(ex.getClass().getSimpleName());
        pd.setDetail(ex.getMessage());

        return pd;
    }
}