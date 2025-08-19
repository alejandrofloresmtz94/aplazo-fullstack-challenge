package com.aplazo.challenge.aplazo_fullstack_challenge.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer.CustomerRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.LoanRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.CustomerRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.LoanRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Validation and Edge Case Integration Tests")
class ValidationIntegrationTest {

    @Container
    @SuppressWarnings("resource") // Testcontainers automatically manages container lifecycle
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("validation_test_db")
            .withUsername("validation_user")
            .withPassword("validation_pass");

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
    private ObjectMapper objectMapper;

    private CustomerRequest validCustomerRequest;
    private String validCustomerId;

    @BeforeEach
    @Transactional
    void setUp() throws Exception {
        customerRepository.deleteAll();
        loanRepository.deleteAll();

        // Create a valid customer for loan tests
        validCustomerRequest = new CustomerRequest();
        validCustomerRequest.setFirstName("Valid");
        validCustomerRequest.setLastName("Customer");
        validCustomerRequest.setSecondLastName("Test");
        validCustomerRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));

        MvcResult result = mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCustomerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseBody);
        validCustomerId = responseJson.get("id").asText();
    }

    // Customer Validation Tests

    @Test
    @DisplayName("Should reject customer with null firstName")
    @Transactional
    void shouldRejectCustomerWithNullFirstName() throws Exception {
        // Given
        CustomerRequest request = new CustomerRequest();
        request.setFirstName(null);
        request.setLastName("Test");
        request.setSecondLastName("User");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));

        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("APZ000004"))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("Should reject customer with empty firstName")
    @Transactional
    void shouldRejectCustomerWithEmptyFirstName() throws Exception {
        // Given
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("");
        request.setLastName("Test");
        request.setSecondLastName("User");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));

        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("APZ000002"))
                .andExpect(jsonPath("$.error").value("INVALID_CUSTOMER_REQUEST"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "  ", "\t", "\n" })
    @DisplayName("Should reject customer with blank or whitespace-only names")
    @Transactional
    void shouldRejectCustomerWithBlankOrWhitespaceNames(String invalidName) throws Exception {
        // Given
        CustomerRequest request = new CustomerRequest();
        request.setFirstName(invalidName);
        request.setLastName("Test");
        request.setSecondLastName("User");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));

        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject customer with null dateOfBirth")
    @Transactional
    void shouldRejectCustomerWithNullDateOfBirth() throws Exception {
        // Given
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Test");
        request.setLastName("User");
        request.setSecondLastName("Name");
        request.setDateOfBirth(null);

        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("APZ000004"));
    }

    @Test
    @DisplayName("Should reject customer exactly under 18 years old")
    @Transactional
    void shouldRejectCustomerExactlyUnder18YearsOld() throws Exception {
        // Given - Customer who turns 18 tomorrow
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate dateOfBirth = tomorrow.minusYears(18);

        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Minor");
        request.setLastName("Customer");
        request.setSecondLastName("Test");
        request.setDateOfBirth(dateOfBirth);

        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("APZ000004"))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("Should reject customer exactly over 65 years old")
    @Transactional
    void shouldRejectCustomerExactlyOver65YearsOld() throws Exception {
        // Given - Customer who turned 66 yesterday
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate dateOfBirth = yesterday.minusYears(66);

        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Senior");
        request.setLastName("Customer");
        request.setSecondLastName("Test");
        request.setDateOfBirth(dateOfBirth);

        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("APZ000002"))
                .andExpect(jsonPath("$.error").value("INVALID_CUSTOMER_REQUEST"));
    }

    @Test
    @DisplayName("Should accept customer on their 18th birthday")
    @Transactional
    void shouldAcceptCustomerOnTheir18thBirthday() throws Exception {
        // Given - Customer who turns 18 today
        LocalDate today = LocalDate.now();
        LocalDate dateOfBirth = today.minusYears(18);

        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Young");
        request.setLastName("Adult");
        request.setSecondLastName("Test");
        request.setDateOfBirth(dateOfBirth);

        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Should accept customer on their 65th birthday")
    @Transactional
    void shouldAcceptCustomerOnTheir65thBirthday() throws Exception {
        // Given - Customer who turns 65 today
        LocalDate today = LocalDate.now();
        LocalDate dateOfBirth = today.minusYears(65);

        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Senior");
        request.setLastName("Adult");
        request.setSecondLastName("Test");
        request.setDateOfBirth(dateOfBirth);

        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Should reject customer with future date of birth")
    @Transactional
    void shouldRejectCustomerWithFutureDateOfBirth() throws Exception {
        // Given
        LocalDate futureDate = LocalDate.now().plusDays(1);
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Time");
        request.setLastName("Traveler");
        request.setSecondLastName("Test");
        request.setDateOfBirth(futureDate);

        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("APZ000004"));
    }

    @Test
    @DisplayName("Should accept customer with very long but valid names")
    @Transactional
    void shouldAcceptCustomerWithVeryLongButValidNames() throws Exception {
        // Given
        String longName = "A".repeat(100); // Very long name
        CustomerRequest request = new CustomerRequest();
        request.setFirstName(longName);
        request.setLastName(longName);
        request.setSecondLastName(longName);
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));

        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Should reject customer with special characters in names")
    @Transactional
    void shouldRejectCustomerWithSpecialCharactersInNames() throws Exception {
        // Given
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("José María");
        request.setLastName("García-Rodríguez");
        request.setSecondLastName("Fernández-Muñoz");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));

        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("APZ000004"))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("Should reject customer with numbers in names")
    @Transactional
    void shouldRejectCustomerWithNumbersInNames() throws Exception {
        // Given
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Juan2");
        request.setLastName("García3");
        request.setSecondLastName("López4");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));

        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("APZ000004"))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    // Loan Validation Tests

    @Test
    @DisplayName("Should reject loan with null customerId")
    @WithMockUser
    @Transactional
    void shouldRejectLoanWithNullCustomerId() throws Exception {
        // Given
        LoanRequest request = new LoanRequest();
        request.setCustomerId(null);
        request.setAmount(new BigDecimal("500.00"));

        // When & Then
        mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("APZ000004"))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("Should reject loan with null amount")
    @WithMockUser
    @Transactional
    void shouldRejectLoanWithNullAmount() throws Exception {
        // Given
        LoanRequest request = new LoanRequest();
        request.setCustomerId(UUID.fromString(validCustomerId));
        request.setAmount(null);

        // When & Then
        mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("APZ000004"));
    }

    @Test
    @DisplayName("Should reject loan with zero amount")
    @WithMockUser
    @Transactional
    void shouldRejectLoanWithZeroAmount() throws Exception {
        // Given
        LoanRequest request = new LoanRequest();
        request.setCustomerId(UUID.fromString(validCustomerId));
        request.setAmount(BigDecimal.ZERO);

        // When & Then
        mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("APZ000004"))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("Should reject loan with negative amount")
    @WithMockUser
    @Transactional
    void shouldRejectLoanWithNegativeAmount() throws Exception {
        // Given
        LoanRequest request = new LoanRequest();
        request.setCustomerId(UUID.fromString(validCustomerId));
        request.setAmount(new BigDecimal("-100.00"));

        // When & Then
        mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("APZ000004"));
    }

    @Test
    @DisplayName("Should accept loan with very small positive amount")
    @WithMockUser
    @Transactional
    void shouldAcceptLoanWithVerySmallPositiveAmount() throws Exception {
        // Given
        LoanRequest request = new LoanRequest();
        request.setCustomerId(UUID.fromString(validCustomerId));
        request.setAmount(new BigDecimal("0.01"));

        // When & Then
        mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.amount").value(0.01));
    }

    @Test
    @DisplayName("Should accept loan with very large amount")
    @WithMockUser
    @Transactional
    void shouldAcceptLoanWithVeryLargeAmount() throws Exception {
        // Given
        LoanRequest request = new LoanRequest();
        request.setCustomerId(UUID.fromString(validCustomerId));
        request.setAmount(new BigDecimal("999999.99"));

        // When & Then
        mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.amount").value(999999.99));
    }

    @Test
    @DisplayName("Should reject loan for non-existent customer")
    @WithMockUser
    @Transactional
    void shouldRejectLoanForNonExistentCustomer() throws Exception {
        // Given
        UUID nonExistentCustomerId = UUID.randomUUID();
        LoanRequest request = new LoanRequest();
        request.setCustomerId(nonExistentCustomerId);
        request.setAmount(new BigDecimal("500.00"));

        // When & Then
        mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle precision in loan amounts correctly")
    @WithMockUser
    @Transactional
    void shouldHandlePrecisionInLoanAmountsCorrectly() throws Exception {
        // Given - Amount with many decimal places
        LoanRequest request = new LoanRequest();
        request.setCustomerId(UUID.fromString(validCustomerId));
        request.setAmount(new BigDecimal("123.456789"));

        // When & Then
        mockMvc.perform(post("/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
        // Note: The actual precision handling depends on your business rules
        // You might want to round to 2 decimal places for currency
    }

    // JSON Format Validation Tests

    @Test
    @DisplayName("Should reject completely malformed JSON")
    @Transactional
    void shouldRejectCompletelyMalformedJson() throws Exception {
        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid-json-format}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject JSON with missing closing braces")
    @Transactional
    void shouldRejectJsonWithMissingClosingBraces() throws Exception {
        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Test\",\"lastName\":\"User\""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject JSON with invalid field types")
    @Transactional
    void shouldRejectJsonWithInvalidFieldTypes() throws Exception {
        // When & Then - dateOfBirth as number instead of string
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"firstName\":\"Test\",\"lastName\":\"User\",\"secondLastName\":\"Name\",\"dateOfBirth\":20240101}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle empty JSON object")
    @Transactional
    void shouldHandleEmptyJsonObject() throws Exception {
        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("APZ000004"));
    }

    @Test
    @DisplayName("Should handle null JSON")
    @Transactional
    void shouldHandleNullJson() throws Exception {
        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle array instead of object")
    @Transactional
    void shouldHandleArrayInsteadOfObject() throws Exception {
        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\"firstName\":\"Test\"}]"))
                .andExpect(status().isBadRequest());
    }

    // Content-Type Validation Tests

    @Test
    @DisplayName("Should reject request without Content-Type header")
    @Transactional
    void shouldRejectRequestWithoutContentTypeHeader() throws Exception {
        // When & Then
        mockMvc.perform(post("/customers")
                .content(objectMapper.writeValueAsString(validCustomerRequest)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("Should reject request with wrong Content-Type")
    @Transactional
    void shouldRejectRequestWithWrongContentType() throws Exception {
        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.TEXT_PLAIN)
                .content(objectMapper.writeValueAsString(validCustomerRequest)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("Should accept request with correct Content-Type and charset")
    @Transactional
    void shouldAcceptRequestWithCorrectContentTypeAndCharset() throws Exception {
        // When & Then
        mockMvc.perform(post("/customers")
                .contentType("application/json;charset=UTF-8")
                .content(objectMapper.writeValueAsString(validCustomerRequest)))
                .andExpect(status().isCreated());
    }

    // Edge Cases for Business Logic

    @Test
    @DisplayName("Should handle customer with minimum valid age range")
    @Transactional
    void shouldHandleCustomerWithMinimumValidAgeRange() throws Exception {
        // Given - Customer exactly 18 years and 1 day old
        LocalDate dateOfBirth = LocalDate.now().minusYears(18).minusDays(1);
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Young");
        request.setLastName("Valid");
        request.setSecondLastName("Customer");
        request.setDateOfBirth(dateOfBirth);

        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Should handle customer with maximum valid age range")
    @Transactional
    void shouldHandleCustomerWithMaximumValidAgeRange() throws Exception {
        // Given - Customer exactly 65 years minus 1 day old
        LocalDate dateOfBirth = LocalDate.now().minusYears(65).plusDays(1);
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Senior");
        request.setLastName("Valid");
        request.setSecondLastName("Customer");
        request.setDateOfBirth(dateOfBirth);

        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

}
