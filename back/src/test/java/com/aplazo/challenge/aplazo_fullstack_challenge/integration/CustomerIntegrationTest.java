package com.aplazo.challenge.aplazo_fullstack_challenge.integration;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer.CustomerRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.CustomerEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.CustomerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Customer API Integration Tests")
class CustomerIntegrationTest {

    @Container
    @SuppressWarnings("resource") // Testcontainers automatically manages container lifecycle
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("integration_test_db")
            .withUsername("integration_user")
            .withPassword("integration_pass");

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
    private ObjectMapper objectMapper;

    private CustomerRequest validCustomerRequest;

    @BeforeEach
    @Transactional
    void setUp() {
        customerRepository.deleteAll();

        validCustomerRequest = new CustomerRequest();
        validCustomerRequest.setFirstName("Juan");
        validCustomerRequest.setLastName("Pérez");
        validCustomerRequest.setSecondLastName("López");
        validCustomerRequest.setDateOfBirth(LocalDate.of(1990, 5, 15));
    }

    @Test
    @DisplayName("Should create customer successfully and return JWT token")
    @Transactional
    void shouldCreateCustomerSuccessfullyAndReturnJwtToken() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCustomerRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.creditLineAmount").value(1.00))
                .andExpect(jsonPath("$.availableCreditLineAmount").value(0.00))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(header().exists("Location"))
                .andExpect(header().exists("X-Auth-Token"))
                .andReturn();

        // Verify JWT token is present and not empty
        String jwtToken = result.getResponse().getHeader("X-Auth-Token");
        assertThat(jwtToken).isNotNull().isNotEmpty();

        // Verify Location header format
        String location = result.getResponse().getHeader("Location");
        assertThat(location).matches("/v1/customers/[a-fA-F0-9\\-]{36}");

        // Verify customer is persisted in database
        String responseBody = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseBody);
        String customerId = responseJson.get("id").asText();

        List<CustomerEntity> customers = customerRepository.findAll();
        assertThat(customers).hasSize(1);
        assertThat(customers.get(0).getId().toString()).hasToString(customerId);
        assertThat(customers.get(0).getFirstName()).isEqualTo("Juan");
        assertThat(customers.get(0).getLastName()).isEqualTo("Pérez");
        assertThat(customers.get(0).getSecondLastName()).isEqualTo("López");
    }

    @Test
    @DisplayName("Should retrieve customer by ID with authentication")
    @WithMockUser
    @Transactional
    void shouldRetrieveCustomerByIdWithAuthentication() throws Exception {
        // Given - Create customer first
        MvcResult createResult = mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCustomerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseBody);
        String customerId = responseJson.get("id").asText();

        // When & Then
        mockMvc.perform(get("/v1/customers/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(customerId))
                .andExpect(jsonPath("$.creditLineAmount").value(1.00))
                .andExpect(jsonPath("$.availableCreditLineAmount").value(0.00))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("Should return 401 when accessing customer without authentication")
    @Transactional
    void shouldReturn401WhenAccessingCustomerWithoutAuthentication() throws Exception {
        // Given
        UUID randomCustomerId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(get("/v1/customers/{customerId}", randomCustomerId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 404 when customer not found")
    @WithMockUser
    @Transactional
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(get("/v1/customers/{customerId}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("APZ000005"))
                .andExpect(jsonPath("$.error").value("CUSTOMER_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should return 400 for invalid UUID format")
    @WithMockUser
    @Transactional
    void shouldReturn400ForInvalidUuidFormat() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/customers/{customerId}", "invalid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("APZ000004"))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("Should return 400 for customer under 18")
    @Transactional
    void shouldReturn400ForCustomerUnder18() throws Exception {
        // Given - Customer under 18
        CustomerRequest underageRequest = new CustomerRequest();
        underageRequest.setFirstName("Minor");
        underageRequest.setLastName("Customer");
        underageRequest.setSecondLastName("Test");
        underageRequest.setDateOfBirth(LocalDate.now().minusYears(15)); // 15 years old

        // When & Then
        mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(underageRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("APZ000002"))
                .andExpect(jsonPath("$.error").value("INVALID_CUSTOMER_REQUEST"));
    }

    @Test
    @DisplayName("Should return 400 for customer over 65")
    @Transactional
    void shouldReturn400ForCustomerOver65() throws Exception {
        // Given - Customer over 65
        CustomerRequest elderlyRequest = new CustomerRequest();
        elderlyRequest.setFirstName("Senior");
        elderlyRequest.setLastName("Customer");
        elderlyRequest.setSecondLastName("Test");
        elderlyRequest.setDateOfBirth(LocalDate.now().minusYears(70)); // 70 years old

        // When & Then
        mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(elderlyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("APZ000002"))
                .andExpect(jsonPath("$.error").value("INVALID_CUSTOMER_REQUEST"));
    }

    @Test
    @DisplayName("Should accept customer exactly 18 years old")
    @Transactional
    void shouldAcceptCustomerExactly18YearsOld() throws Exception {
        // Given - Customer exactly 18
        CustomerRequest exactAgeRequest = new CustomerRequest();
        exactAgeRequest.setFirstName("Young");
        exactAgeRequest.setLastName("Adult");
        exactAgeRequest.setSecondLastName("Test");
        exactAgeRequest.setDateOfBirth(LocalDate.now().minusYears(18)); // Exactly 18

        // When & Then
        mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exactAgeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Should accept customer exactly 65 years old")
    @Transactional
    void shouldAcceptCustomerExactly65YearsOld() throws Exception {
        // Given - Customer exactly 65
        CustomerRequest exactAgeRequest = new CustomerRequest();
        exactAgeRequest.setFirstName("Senior");
        exactAgeRequest.setLastName("Adult");
        exactAgeRequest.setSecondLastName("Test");
        exactAgeRequest.setDateOfBirth(LocalDate.now().minusYears(65)); // Exactly 65

        // When & Then
        mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exactAgeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Should return 400 for missing required fields")
    @Transactional
    void shouldReturn400ForMissingRequiredFields() throws Exception {
        // Given - Request with missing fields
        CustomerRequest invalidRequest = new CustomerRequest();
        invalidRequest.setFirstName("John");
        // Missing lastName, secondLastName, dateOfBirth

        // When & Then
        mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should return 400 for null request body")
    @Transactional
    void shouldReturn400ForNullRequestBody() throws Exception {
        // When & Then
        mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for malformed JSON")
    @Transactional
    void shouldReturn400ForMalformedJson() throws Exception {
        // When & Then
        mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid-json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle special characters in names correctly")
    @Transactional
    void shouldHandleSpecialCharactersInNamesCorrectly() throws Exception {
        // Given - Customer with special characters
        CustomerRequest specialCharRequest = new CustomerRequest();
        specialCharRequest.setFirstName("José María");
        specialCharRequest.setLastName("García-Rodríguez");
        specialCharRequest.setSecondLastName("Fernández-Muñoz");
        specialCharRequest.setDateOfBirth(LocalDate.of(1985, 12, 25));

        // When & Then
        mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(specialCharRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        // Verify in database
        List<CustomerEntity> customers = customerRepository.findAll();
        assertThat(customers).hasSize(1);
        assertThat(customers.get(0).getFirstName()).isEqualTo("José María");
        assertThat(customers.get(0).getLastName()).isEqualTo("García-Rodríguez");
        assertThat(customers.get(0).getSecondLastName()).isEqualTo("Fernández-Muñoz");
    }

    @Test
    @DisplayName("Should generate unique UUIDv7 for concurrent customers")
    @Transactional
    void shouldGenerateUniqueUuidV7ForConcurrentCustomers() throws Exception {
        // Given - Multiple customer requests
        CustomerRequest request1 = createValidCustomerRequest("Customer1", "LastName1", "SecondLast1");
        CustomerRequest request2 = createValidCustomerRequest("Customer2", "LastName2", "SecondLast2");
        CustomerRequest request3 = createValidCustomerRequest("Customer3", "LastName3", "SecondLast3");

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

        // Then - Verify unique IDs
        String id1 = extractCustomerIdFromResponse(result1);
        String id2 = extractCustomerIdFromResponse(result2);
        String id3 = extractCustomerIdFromResponse(result3);

        assertThat(id1).isNotEqualTo(id2).isNotEqualTo(id3);
        assertThat(id2).isNotEqualTo(id3);

        // Verify all are UUIDv7
        assertThat(UUID.fromString(id1).version()).isEqualTo(7);
        assertThat(UUID.fromString(id2).version()).isEqualTo(7);
        assertThat(UUID.fromString(id3).version()).isEqualTo(7);
    }

    @Test
    @DisplayName("Should persist customer with correct timestamps")
    @Transactional
    void shouldPersistCustomerWithCorrectTimestamps() throws Exception {
        // When
        MvcResult result = mockMvc.perform(post("/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCustomerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // Then
        String customerId = extractCustomerIdFromResponse(result);
        CustomerEntity customer = customerRepository.findById(UUID.fromString(customerId)).orElseThrow();

        assertThat(customer.getCreatedAt()).isNotNull();
        assertThat(customer.getCreatedAt()).isBefore(Instant.now().plusSeconds(1));
        assertThat(customer.getCreatedAt()).isAfter(Instant.now().minusSeconds(60));
    }

    private CustomerRequest createValidCustomerRequest(String firstName, String lastName, String secondLastName) {
        CustomerRequest request = new CustomerRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setSecondLastName(secondLastName);
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        return request;
    }

    private String extractCustomerIdFromResponse(MvcResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseBody);
        return responseJson.get("id").asText();
    }
}
