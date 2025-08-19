package com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
