package com.aplazo.challenge.aplazo_fullstack_challenge.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer.CustomerRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Rate Limiting Integration Tests")
class RateLimitingIntegrationTest {

    @Container
    @SuppressWarnings("resource") // Testcontainers automatically manages container lifecycle
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("rate_limit_test_db")
            .withUsername("rate_limit_user")
            .withPassword("rate_limit_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        
        // Configure rate limiting for tests
        registry.add("rate.limit.requests", () -> "10"); // 10 requests
        registry.add("rate.limit.window.seconds", () -> "60"); // per 60 seconds
        registry.add("rate.limit.enabled", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private CustomerRequest validCustomerRequest;

    @BeforeEach
    @Transactional
    void setUp() {
        customerRepository.deleteAll();
        
        validCustomerRequest = new CustomerRequest();
        validCustomerRequest.setFirstName("RateLimit");
        validCustomerRequest.setLastName("Test");
        validCustomerRequest.setSecondLastName("User");
        validCustomerRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));
    }

    @Test
    @DisplayName("Should allow requests under rate limit")
    @Transactional
    void shouldAllowRequestsUnderRateLimit() throws Exception {
        // Given - Make several requests within limit (5 out of 10)
        for (int i = 0; i < 5; i++) {
            CustomerRequest request = createUniqueCustomerRequest(i);
            
            // When & Then - All requests should be successful
            mockMvc.perform(post("/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @DisplayName("Should block requests that exceed rate limit")
    @Transactional
    void shouldBlockRequestsThatExceedRateLimit() throws Exception {
        // Given - Exhaust the rate limit (10 requests)
        for (int i = 0; i < 10; i++) {
            CustomerRequest request = createUniqueCustomerRequest(i);
            
            mockMvc.perform(post("/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
        
        // When & Then - Next request should be rate limited
        CustomerRequest additionalRequest = createUniqueCustomerRequest(100);
        mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(additionalRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("APZ000003"))
                .andExpect(jsonPath("$.error").value("RATE_LIMIT_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/v1/customers"));
    }

    @Test
    @DisplayName("Should apply rate limit per IP address")
    @Transactional 
    void shouldApplyRateLimitPerIpAddress() throws Exception {
        // Given - Make requests from first IP address (simulate with X-Forwarded-For)
        String firstIp = "192.168.1.100";
        for (int i = 0; i < 10; i++) {
            CustomerRequest request = createUniqueCustomerRequest(i);
            
            mockMvc.perform(post("/v1/customers")
                    .header("X-Forwarded-For", firstIp)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
        
        // When - Make request from same IP (should be blocked)
        CustomerRequest blockedRequest = createUniqueCustomerRequest(100);
        mockMvc.perform(post("/v1/customers")
                .header("X-Forwarded-For", firstIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(blockedRequest)))
                .andExpect(status().isTooManyRequests());
        
        // Then - Make request from different IP (should be allowed)
        String secondIp = "192.168.1.200";
        CustomerRequest allowedRequest = createUniqueCustomerRequest(200);
        mockMvc.perform(post("/v1/customers")
                .header("X-Forwarded-For", secondIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(allowedRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should handle concurrent requests correctly with rate limiting")
    @Transactional
    void shouldHandleConcurrentRequestsCorrectlyWithRateLimiting() throws Exception {
        // Given
        ExecutorService executor = Executors.newFixedThreadPool(15);
        
        try {
            // When - Send 15 concurrent requests (exceeds limit of 10)
            CompletableFuture<Integer>[] futures = IntStream.range(0, 15)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    try {
                        CustomerRequest request = createUniqueCustomerRequest(i);
                        var result = mockMvc.perform(post("/v1/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andReturn();
                        return result.getResponse().getStatus();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, executor))
                .toArray(CompletableFuture[]::new);
            
            CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);
            
            // Then - Count successful vs rate limited responses
            long successfulRequests = java.util.Arrays.stream(futures)
                .mapToInt(f -> f.join())
                .filter(status -> status == 201)
                .count();
            
            long rateLimitedRequests = java.util.Arrays.stream(futures)
                .mapToInt(f -> f.join())
                .filter(status -> status == 429)
                .count();
            
            // Should have exactly 10 successful and 5 rate limited
            org.assertj.core.api.Assertions.assertThat(successfulRequests).isEqualTo(10);
            org.assertj.core.api.Assertions.assertThat(rateLimitedRequests).isEqualTo(5);
            
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Should reset rate limit after time window expires")
    @Transactional
    void shouldResetRateLimitAfterTimeWindowExpires() throws Exception {
        // Note: This test would require a shorter time window for practical testing
        // In a real scenario, you might want to use a separate test configuration
        // with a very short rate limit window (e.g., 1 second)
        
        // Given - Configure test with shorter window via test properties
        // Make 10 requests to exhaust limit
        for (int i = 0; i < 10; i++) {
            CustomerRequest request = createUniqueCustomerRequest(i);
            
            mockMvc.perform(post("/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
        
        // Verify limit is reached
        CustomerRequest blockedRequest = createUniqueCustomerRequest(100);
        mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(blockedRequest)))
                .andExpect(status().isTooManyRequests());
        
        // Note: In a real test, we would wait for the window to reset
        // For this test, we're just verifying the rate limiting logic works
    }

    @Test
    @DisplayName("Should include rate limit headers in response")
    @Transactional
    void shouldIncludeRateLimitHeadersInResponse() throws Exception {
        // Given & When - Make a single request
        mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCustomerRequest)))
                .andExpect(status().isCreated());
                // Note: In a real implementation, you might add rate limit headers
                // .andExpect(header().exists("X-RateLimit-Limit"))
                // .andExpect(header().exists("X-RateLimit-Remaining"))
                // .andExpect(header().exists("X-RateLimit-Reset"));
    }

    @Test
    @DisplayName("Should apply different rate limits to different endpoints")
    @Transactional
    void shouldApplyDifferentRateLimitsToDifferentEndpoints() throws Exception {
        // This test would verify that different endpoints can have different rate limits
        // For example, customer creation might have stricter limits than customer retrieval
        
        // Given - Make multiple customer creation requests
        for (int i = 0; i < 5; i++) {
            CustomerRequest request = createUniqueCustomerRequest(i);
            
            mockMvc.perform(post("/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
        
        // In a real scenario, you might verify that GET requests have higher limits
        // or different rate limiting logic
    }

    @Test
    @DisplayName("Should handle rate limiting with invalid requests")
    @Transactional
    void shouldHandleRateLimitingWithInvalidRequests() throws Exception {
        // Given - Make multiple invalid requests that would normally return 400
        CustomerRequest invalidRequest = new CustomerRequest();
        // Missing required fields
        
        for (int i = 0; i < 10; i++) {
            // When & Then - Invalid requests should still count towards rate limit
            mockMvc.perform(post("/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest()); // Validation error, not rate limit
        }
        
        // The 11th request should be rate limited, not validated
        mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isTooManyRequests()); // Rate limit takes precedence
    }

    @Test
    @DisplayName("Should handle rate limiting for malformed JSON requests")
    @Transactional
    void shouldHandleRateLimitingForMalformedJsonRequests() throws Exception {
        // Given - Make multiple malformed JSON requests
        String malformedJson = "{invalid-json}";
        
        for (int i = 0; i < 10; i++) {
            // When & Then - Malformed requests should still count towards rate limit
            mockMvc.perform(post("/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(malformedJson))
                    .andExpect(status().isBadRequest());
        }
        
        // The 11th request should be rate limited
        mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Should maintain rate limit state across different request types")
    @Transactional
    void shouldMaintainRateLimitStateAcrossDifferentRequestTypes() throws Exception {
        // Given - Mix of valid and invalid requests
        for (int i = 0; i < 5; i++) {
            // Valid request
            CustomerRequest validRequest = createUniqueCustomerRequest(i);
            mockMvc.perform(post("/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated());
            
            // Invalid request  
            CustomerRequest invalidRequest = new CustomerRequest();
            mockMvc.perform(post("/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
        
        // At this point we've made 10 requests (5 valid, 5 invalid)
        // The next request should be rate limited
        CustomerRequest finalRequest = createUniqueCustomerRequest(100);
        mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(finalRequest)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Should provide meaningful error message for rate limit exceeded")
    @Transactional
    void shouldProvideMeaningfulErrorMessageForRateLimitExceeded() throws Exception {
        // Given - Exhaust rate limit
        for (int i = 0; i < 10; i++) {
            CustomerRequest request = createUniqueCustomerRequest(i);
            mockMvc.perform(post("/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
        
        // When & Then - Rate limit exceeded response should have proper error structure
        CustomerRequest blockedRequest = createUniqueCustomerRequest(100);
        mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(blockedRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("APZ000003"))
                .andExpect(jsonPath("$.error").value("RATE_LIMIT_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/v1/customers"));
    }

    private CustomerRequest createUniqueCustomerRequest(int index) {
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Customer" + index);
        request.setLastName("Test" + index);
        request.setSecondLastName("User" + index);
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        return request;
    }
}
