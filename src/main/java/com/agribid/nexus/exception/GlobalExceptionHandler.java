package com.agribid.nexus.exception;

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

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockFailure(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Auction State Changed — Please Retry");
        pd.setDetail("Your bid was computed against stale auction state. Refresh and resubmit.");
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

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {

        ex.printStackTrace();   // Print the full stack trace

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle(ex.getClass().getSimpleName());
        pd.setDetail(ex.getMessage());

        return pd;
    }
}