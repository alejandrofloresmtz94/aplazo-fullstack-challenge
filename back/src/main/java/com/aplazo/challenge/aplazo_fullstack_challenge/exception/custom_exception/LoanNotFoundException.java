package com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception;

public class LoanNotFoundException extends RuntimeException {
    public LoanNotFoundException(String message) {
        super(message);
    }
}