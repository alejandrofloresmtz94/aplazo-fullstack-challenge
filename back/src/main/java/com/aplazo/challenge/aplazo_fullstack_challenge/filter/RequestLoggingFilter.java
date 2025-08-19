package com.aplazo.challenge.aplazo_fullstack_challenge.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();

        log.info("Incoming request: {} {}", method, uri);

        filterChain.doFilter(request, response);

        long duration = System.currentTimeMillis() - startTime;
        int status = response.getStatus();

        log.info("Completed request: {} {} with status {} in {}ms", method, uri, status, duration);
    }

}
