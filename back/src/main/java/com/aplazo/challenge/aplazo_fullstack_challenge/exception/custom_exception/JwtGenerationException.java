package com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception;

public class JwtGenerationException extends RuntimeException {

    public JwtGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
