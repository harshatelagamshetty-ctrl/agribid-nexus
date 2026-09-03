package com.agribid.nexus.integration;

/**
 * Deliberately maps to 501 Not Implemented, not 500 — this tells an
 * API consumer plainly "this feature is real but requires
 * configuration we don't have," which is a different, more honest
 * signal than an unexpected server error.
 */
public class IntegrationNotConfiguredException extends RuntimeException {
    public IntegrationNotConfiguredException(String message) {
        super(message);
    }
}
