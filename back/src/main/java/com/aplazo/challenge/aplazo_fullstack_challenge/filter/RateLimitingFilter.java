package com.aplazo.challenge.aplazo_fullstack_challenge.filter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.aplazo.challenge.aplazo_fullstack_challenge.config.RateLimitProperties;
import com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception.RateLimitExceededException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitProperties rateLimitProperties;

    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    public RateLimitingFilter(RateLimitProperties rateLimitProperties) {
        this.rateLimitProperties = rateLimitProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIP(request);
        long currentTime = Instant.now().getEpochSecond();

        RequestCounter counter = requestCounts.computeIfAbsent(clientIp, ip -> new RequestCounter(0, currentTime));
        synchronized (counter) {
            if (currentTime - counter.timestamp >= 60) {
                // Resetear contador cada minuto
                counter.count = 1;
                counter.timestamp = currentTime;
            } else {
                counter.count++;
                if (counter.count > rateLimitProperties.getMaxRequestsPerMinute()) {
                    throw new RateLimitExceededException("Too many requests from IP: " + clientIp);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    // Clase interna para mantener contador y timestamp
    private static class RequestCounter {
        int count;
        long timestamp;

        RequestCounter(int count, long timestamp) {
            this.count = count;
            this.timestamp = timestamp;
        }
    }
}