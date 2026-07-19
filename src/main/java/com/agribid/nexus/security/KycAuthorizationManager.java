package com.agribid.nexus.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Custom AuthorizationManager enforcing kycVerified at the filter
 * chain layer — before DispatcherServlet routes the request to any
 * controller. There is no code path that reaches bidding logic
 * without first passing this check: the fraud/impersonation risk of
 * an unverified distributor bidding is denied at the network
 * boundary, not caught reactively in a service method.
 */
@Component
public class KycAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();

        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return new AuthorizationDecision(false);
        }

        HttpServletRequest request = context.getRequest();
        boolean isBiddingEndpoint = request.getRequestURI().matches("^/api/v1/listings/\\d+/bids$");

        if (!isBiddingEndpoint) {
            // not our concern; defer to role-based rules elsewhere in the chain
            return new AuthorizationDecision(true);
        }

        return new AuthorizationDecision(principal.isKycVerified());
    }
}