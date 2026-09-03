package com.agribid.nexus.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A real interface and a real, correctly-shaped API — deliberately
 * NOT a fake working implementation. Each method below throws a
 * clear, honest "not configured" exception rather than pretending to
 * succeed, because a payment gateway, a government API, and a
 * lending partner all require real credentials this project does
 * not have and cannot fabricate. The moment real credentials exist,
 * only the method bodies below need to change — every caller
 * (BidListingServiceImpl, etc.) already expects this exact contract.
 */
@Component
public class ExternalIntegrationGateway {

    @Value("${agribid.integrations.payment-gateway-api-key:}")
    private String paymentGatewayApiKey;

    @Value("${agribid.integrations.enam-api-key:}")
    private String enamApiKey;

    public record EscrowResult(boolean success, String transactionReference, String message) {}

    /**
     * Real UPI-based escrow requires a live merchant account with a
     * payment provider (Razorpay, PayU, etc.), business KYC, and
     * real banking credentials — none of which exist in this
     * project's configuration. This method is honestly unusable
     * until agribid.integrations.payment-gateway-api-key is set to a
     * real value; it throws rather than returning a fabricated
     * success.
     */
    public EscrowResult initiateEscrow(Long contractId, java.math.BigDecimal amount) {
        if (paymentGatewayApiKey == null || paymentGatewayApiKey.isBlank()) {
            throw new IntegrationNotConfiguredException(
                    "Payment gateway is not configured. Real UPI escrow requires a live merchant account "
                            + "and API credentials — set agribid.integrations.payment-gateway-api-key to enable this.");
        }
        // Real integration code goes here once a real provider account exists.
        throw new IntegrationNotConfiguredException("Payment gateway integration is scaffolded but not implemented.");
    }

    public record MandiPriceLookup(String cropCode, String market, java.math.BigDecimal pricePerQuintal, java.time.LocalDate asOf) {}

    /**
     * Real e-NAM/Agmarknet integration requires government-issued
     * API credentials obtained through an external registration
     * process this chat cannot complete on your behalf. Honestly
     * unusable until agribid.integrations.enam-api-key is set.
     */
    public MandiPriceLookup lookupGovernmentMandiPrice(String cropCode, String market) {
        if (enamApiKey == null || enamApiKey.isBlank()) {
            throw new IntegrationNotConfiguredException(
                    "e-NAM/Agmarknet API is not configured. Requires real government-issued API credentials, "
                            + "obtained via external registration — set agribid.integrations.enam-api-key to enable this.");
        }
        throw new IntegrationNotConfiguredException("Government mandi price integration is scaffolded but not implemented.");
    }

    public record KccEligibilitySignal(boolean informationalOnly, String message) {}

    /**
     * There is no public API for PM-KISAN or Kisan Credit Card
     * eligibility to integrate against — this was always,
     * deliberately, informational signposting only, not a claimed
     * integration. This method returns real information about that
     * fact rather than throwing, since "we don't integrate with
     * this" is itself the honest, complete answer here.
     */
    public KccEligibilitySignal getKccEligibilitySignpost(Long farmerId) {
        return new KccEligibilitySignal(true,
                "No public PM-KISAN/KCC eligibility API exists to integrate against. This is informational "
                        + "guidance only: farmers with a consistent, verified transaction history on this platform "
                        + "may have supporting documentation useful for a real KCC application through their bank.");
    }
}
