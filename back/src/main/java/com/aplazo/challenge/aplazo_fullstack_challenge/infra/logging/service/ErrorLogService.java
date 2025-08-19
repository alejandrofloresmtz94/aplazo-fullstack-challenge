package com.aplazo.challenge.aplazo_fullstack_challenge.infra.logging.service;

public interface ErrorLogService {
    void logError(String code, String error, String message, String path);
}
