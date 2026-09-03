package com.agribid.nexus.exception;

public abstract class AgriBidException extends RuntimeException {
    protected AgriBidException(String message) {
        super(message);
    }
}