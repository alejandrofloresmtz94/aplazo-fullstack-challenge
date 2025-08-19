package com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception;

public class JwtDecoderInitializationException extends RuntimeException {

    public JwtDecoderInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

}
