package com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(String message) {
        super(message);
    }
}
