package com.aplazo.challenge.aplazo_fullstack_challenge.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer.CustomerRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer.CustomerResponse;
import com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception.CustomerNotFoundException;
import com.aplazo.challenge.aplazo_fullstack_challenge.security.JwtService;
import com.aplazo.challenge.aplazo_fullstack_challenge.services.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("Customer Controller Tests")
class CustomerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CustomerService customerService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private CustomerController customerController;

    private ObjectMapper objectMapper;

    private CustomerRequest customerRequest;
    private CustomerResponse customerResponse;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(customerController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        customerId = UUID.randomUUID();

        customerRequest = new CustomerRequest();
        customerRequest.setFirstName("Juan");
        customerRequest.setLastName("Pérez");
        customerRequest.setSecondLastName("López");
        customerRequest.setDateOfBirth(java.time.LocalDate.of(1990, 5, 15));

        customerResponse = CustomerResponse.builder()
                .id(customerId)
                .creditLineAmount(new BigDecimal("1.00"))
                .availableCreditLineAmount(new BigDecimal("0.00"))
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("POST /customers should create customer and return 201 with headers")
    void shouldCreateCustomerAndReturn201WithHeaders() throws Exception {
        // Given
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";
        when(customerService.createCustomer(any(CustomerRequest.class))).thenReturn(customerResponse);
        when(jwtService.generateToken(customerId.toString(), 1800)).thenReturn(token);

        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Location", "/v1/customers/" + customerId))
                .andExpect(header().string("X-Auth-Token", token))
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.creditLineAmount").value(1.00))
                .andExpect(jsonPath("$.availableCreditLineAmount").value(0.00));
    }

    @Test
    @DisplayName("GET /customers/{id} should return customer without headers")
    void shouldGetCustomerByIdWithoutHeaders() throws Exception {
        // Given
        when(customerService.getCustomerById(customerId)).thenReturn(customerResponse);

        // When & Then
        mockMvc.perform(get("/customers/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.creditLineAmount").value(1.00))
                .andExpect(jsonPath("$.availableCreditLineAmount").value(0.00));
    }

    @Test
    @DisplayName("GET /customers/{id} should return 404 when customer not found")
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(customerService.getCustomerById(nonExistentId))
                .thenThrow(new CustomerNotFoundException("Customer not found"));

        // When & Then
        mockMvc.perform(get("/customers/{customerId}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /customers should return 400 for invalid request")
    void shouldReturn400ForInvalidRequest() throws Exception {
        // Given - Invalid customer request (missing required fields)
        CustomerRequest invalidRequest = new CustomerRequest();
        // firstName is null (required)

        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /customers should return 400 for invalid age")
    void shouldReturn400ForInvalidAge() throws Exception {
        // Given - Customer under 18
        CustomerRequest underageRequest = new CustomerRequest();
        underageRequest.setFirstName("Juan");
        underageRequest.setLastName("Pérez");
        underageRequest.setSecondLastName("López");
        underageRequest.setDateOfBirth(java.time.LocalDate.of(2010, 1, 1)); // Under 18

        // When & Then
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(underageRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /customers/{id} should return 400 for invalid UUID")
    void shouldReturn400ForInvalidUUID() throws Exception {
        // When & Then
        mockMvc.perform(get("/customers/{customerId}", "invalid-uuid"))
                .andExpect(status().isBadRequest());
    }
}
