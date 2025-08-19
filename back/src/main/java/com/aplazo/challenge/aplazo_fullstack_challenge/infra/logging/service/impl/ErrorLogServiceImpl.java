package com.aplazo.challenge.aplazo_fullstack_challenge.infra.logging.service.impl;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.aplazo.challenge.aplazo_fullstack_challenge.infra.logging.entity.ErrorLogEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.infra.logging.repository.ErrorLogRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.infra.logging.service.ErrorLogService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ErrorLogServiceImpl implements ErrorLogService {
    private final ErrorLogRepository errorLogRepository;

    @Override
    public void logError(String code, String error, String message, String path) {
        ErrorLogEntity log = ErrorLogEntity.builder()
                .code(code)
                .error(error)
                .message(message)
                .path(path)
                .timestamp(Instant.now().getEpochSecond())
                .build();

        errorLogRepository.save(log);
    }
}
