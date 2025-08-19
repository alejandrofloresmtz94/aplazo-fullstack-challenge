package com.aplazo.challenge.aplazo_fullstack_challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class ErrorResponse {
    String code;
    String error;
    long timestamp;
    String message;
    String path;
}
