package com.aplazo.challenge.aplazo_fullstack_challenge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@ConfigurationProperties(prefix = "rate.limit")
@Getter
public class RateLimitProperties {
    private int maxRequestsPerMinute;

    public void setMaxRequestsPerMinute(int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }
}
