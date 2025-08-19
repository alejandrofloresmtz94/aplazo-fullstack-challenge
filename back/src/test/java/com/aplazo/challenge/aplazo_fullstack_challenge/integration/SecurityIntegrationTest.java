package com.aplazo.challenge.aplazo_fullstack_challenge.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer.CustomerRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.LoanRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.CustomerRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.LoanRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Security and JWT Integration Tests")
class SecurityIntegrationTest {

    @Container
    @SuppressWarnings("resource") // Testcontainers automatically manages container lifecycle
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("security_test_db")
            .withUsername("security_user")
            .withPassword("security_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private CustomerRequest validCustomerRequest;
    private String validCustomerId;
    private String validJwtToken;

    @BeforeEach
    @Transactional
    void setUp() throws Exception {
        customerRepository.deleteAll();
        loanRepository.deleteAll();
        
        validCustomerRequest = new CustomerRequest();
        validCustomerRequest.setFirstName("Security");
        validCustomerRequest.setLastName("Test");
        validCustomerRequest.setSecondLastName("User");
        validCustomerRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));

        // Create a customer and get JWT token
        MvcResult result = mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCustomerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        validJwtToken = result.getResponse().getHeader("X-Auth-Token");
        
        String responseBody = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseBody);
        validCustomerId = responseJson.get("id").asText();
    }

    @Test
    @DisplayName("Should generate valid JWT token on customer creation")
    @Transactional
    void shouldGenerateValidJwtTokenOnCustomerCreation() throws Exception {
        // Given
        CustomerRequest newCustomerRequest = new CustomerRequest();
        newCustomerRequest.setFirstName("JWT");
        newCustomerRequest.setLastName("Test");
        newCustomerRequest.setSecondLastName("Customer");
        newCustomerRequest.setDateOfBirth(LocalDate.of(1985, 6, 15));

        // When & Then
        MvcResult result = mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newCustomerRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Auth-Token"))
                .andReturn();

        String jwtToken = result.getResponse().getHeader("X-Auth-Token");
        
        // Verify JWT token format (should have 3 parts separated by dots)
        assertThat(jwtToken).isNotNull();
        assertThat(jwtToken).isNotEmpty();
        assertThat(jwtToken.split("\\.")).hasSize(3);
        
        // Verify JWT token is not expired and contains customer ID
        String responseBody = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseBody);
        String customerId = responseJson.get("id").asText();
        
        // The JWT should be valid for the customer ID
        assertThat(jwtService.extractSubject(jwtToken)).isEqualTo(customerId);
        assertThat(jwtService.isTokenExpired(jwtToken)).isFalse();
    }

    @Test
    @DisplayName("Should allow access to customer endpoint with valid JWT")
    @Transactional
    void shouldAllowAccessToCustomerEndpointWithValidJwt() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/customers/{customerId}", validCustomerId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(validCustomerId));
    }

    @Test
    @DisplayName("Should deny access to customer endpoint without JWT")
    @Transactional
    void shouldDenyAccessToCustomerEndpointWithoutJwt() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/customers/{customerId}", validCustomerId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should deny access with invalid JWT")
    @Transactional
    void shouldDenyAccessWithInvalidJwt() throws Exception {
        // Given - Invalid JWT token
        String invalidToken = "invalid.jwt.token";

        // When & Then
        mockMvc.perform(get("/v1/customers/{customerId}", validCustomerId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should deny access with malformed Authorization header")
    @Transactional
    void shouldDenyAccessWithMalformedAuthorizationHeader() throws Exception {
        // Given - Malformed authorization header (missing Bearer prefix)
        // When & Then
        mockMvc.perform(get("/v1/customers/{customerId}", validCustomerId)
                .header(HttpHeaders.AUTHORIZATION, validJwtToken)) // Missing "Bearer " prefix
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should deny access with expired JWT")
    @Transactional
    void shouldDenyAccessWithExpiredJwt() throws Exception {
        // Given - Create an expired token (expires immediately)
        String expiredToken = jwtService.generateToken(validCustomerId, 0); // 0 seconds expiration
        
        // Wait a moment to ensure token is expired
        Thread.sleep(100);

        // When & Then
        mockMvc.perform(get("/v1/customers/{customerId}", validCustomerId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow loan creation with valid JWT")
    @WithMockUser
    @Transactional
    void shouldAllowLoanCreationWithValidJwt() throws Exception {
        // Given
        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setCustomerId(UUID.fromString(validCustomerId));
        loanRequest.setAmount(new BigDecimal("500.00"));

        // When & Then
        mockMvc.perform(post("/v1/loans")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerId").value(validCustomerId))
                .andExpect(jsonPath("$.amount").value(500.00));
    }

    @Test
    @DisplayName("Should deny loan creation without JWT")
    @Transactional
    void shouldDenyLoanCreationWithoutJwt() throws Exception {
        // Given
        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setCustomerId(UUID.fromString(validCustomerId));
        loanRequest.setAmount(new BigDecimal("500.00"));

        // When & Then
        mockMvc.perform(post("/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should deny loan retrieval without JWT")
    @Transactional
    void shouldDenyLoanRetrievalWithoutJwt() throws Exception {
        // Given
        UUID randomLoanId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(get("/v1/loans/{loanId}", randomLoanId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should validate JWT token signature")
    @Transactional
    void shouldValidateJwtTokenSignature() throws Exception {
        // Given - Token with tampered signature (change last character)
        String tamperedToken = validJwtToken.substring(0, validJwtToken.length() - 1) + "X";

        // When & Then
        mockMvc.perform(get("/v1/customers/{customerId}", validCustomerId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should extract correct customer ID from JWT")
    @Transactional
    void shouldExtractCorrectCustomerIdFromJwt() {
        // When
        String extractedCustomerId = jwtService.extractSubject(validJwtToken);

        // Then
        assertThat(extractedCustomerId).isEqualTo(validCustomerId);
    }

    @Test
    @DisplayName("Should validate token expiration correctly")
    @Transactional
    void shouldValidateTokenExpirationCorrectly() {
        // Given - Create tokens with different expiration times
        String validToken = jwtService.generateToken(validCustomerId, 3600); // 1 hour
        String expiredToken = jwtService.generateToken(validCustomerId, 0); // Expired immediately

        // When & Then
        assertThat(jwtService.isTokenExpired(validToken)).isFalse();
        assertThat(jwtService.isTokenExpired(expiredToken)).isTrue();
    }

    @Test
    @DisplayName("Should handle JWT token with invalid format")
    @Transactional
    void shouldHandleJwtTokenWithInvalidFormat() {
        // Given - Token with invalid format (not 3 parts)
        String invalidFormatToken = "invalid.token";

        // When & Then
        assertThatThrownBy(() -> jwtService.extractSubject(invalidFormatToken))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should generate unique JWT tokens for different customers")
    @Transactional
    void shouldGenerateUniqueJwtTokensForDifferentCustomers() throws Exception {
        // Given - Create another customer
        CustomerRequest anotherCustomerRequest = new CustomerRequest();
        anotherCustomerRequest.setFirstName("Another");
        anotherCustomerRequest.setLastName("Customer");
        anotherCustomerRequest.setSecondLastName("Test");
        anotherCustomerRequest.setDateOfBirth(LocalDate.of(1988, 3, 20));

        // When
        MvcResult result = mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(anotherCustomerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String anotherJwtToken = result.getResponse().getHeader("X-Auth-Token");

        // Then - Tokens should be different
        assertThat(anotherJwtToken).isNotEqualTo(validJwtToken);
        
        // And should contain different customer IDs
        String anotherCustomerId = jwtService.extractSubject(anotherJwtToken);
        assertThat(anotherCustomerId).isNotEqualTo(validCustomerId);
    }

    @Test
    @DisplayName("Should return 401 with proper error structure for unauthorized requests")
    @Transactional
    void shouldReturn401WithProperErrorStructureForUnauthorizedRequests() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/customers/{customerId}", validCustomerId))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("APZ000007"))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").exists());
    }

    @Test
    @DisplayName("Should handle concurrent JWT token generation")
    @Transactional
    void shouldHandleConcurrentJwtTokenGeneration() throws Exception {
        // Given - Multiple customer requests
        CustomerRequest request1 = createCustomerRequest("Concurrent1", "User1", "Test1");
        CustomerRequest request2 = createCustomerRequest("Concurrent2", "User2", "Test2");
        CustomerRequest request3 = createCustomerRequest("Concurrent3", "User3", "Test3");

        // When - Create customers concurrently (simulated)
        MvcResult result1 = mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult result2 = mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult result3 = mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isCreated())
                .andReturn();

        // Then - All should have unique valid tokens
        String token1 = result1.getResponse().getHeader("X-Auth-Token");
        String token2 = result2.getResponse().getHeader("X-Auth-Token");
        String token3 = result3.getResponse().getHeader("X-Auth-Token");

        assertThat(token1).isNotEqualTo(token2).isNotEqualTo(token3);
        assertThat(token2).isNotEqualTo(token3);

        // All tokens should be valid
        assertThat(jwtService.isTokenExpired(token1)).isFalse();
        assertThat(jwtService.isTokenExpired(token2)).isFalse();
        assertThat(jwtService.isTokenExpired(token3)).isFalse();
    }

    @Test
    @DisplayName("Should validate JWT token claims correctly")
    @Transactional
    void shouldValidateJwtTokenClaimsCorrectly() {
        // Given
        String customerId = UUID.randomUUID().toString();
        long expirationSeconds = 3600;

        // When
        String token = jwtService.generateToken(customerId, expirationSeconds);

        // Then
        assertThat(jwtService.extractSubject(token)).isEqualTo(customerId);
        assertThat(jwtService.isTokenExpired(token)).isFalse();
        
        // Token should contain the customer ID as subject
        String extractedSubject = jwtService.extractSubject(token);
        assertThat(extractedSubject).isEqualTo(customerId);
    }

    private CustomerRequest createCustomerRequest(String firstName, String lastName, String secondLastName) {
        CustomerRequest request = new CustomerRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setSecondLastName(secondLastName);
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        return request;
    }
}
