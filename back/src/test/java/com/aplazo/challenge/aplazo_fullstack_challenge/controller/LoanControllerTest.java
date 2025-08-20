package com.aplazo.challenge.aplazo_fullstack_challenge.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.Installment;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.LoanRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.LoanResponse;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.PaymentPlan;
import com.aplazo.challenge.aplazo_fullstack_challenge.enums.InstallmentStatus;
import com.aplazo.challenge.aplazo_fullstack_challenge.enums.LoanStatus;
import com.aplazo.challenge.aplazo_fullstack_challenge.exception.GlobalExceptionHandler;
import com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception.CustomerNotFoundException;
import com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception.LoanNotFoundException;
import com.aplazo.challenge.aplazo_fullstack_challenge.infra.logging.service.ErrorLogService;
import com.aplazo.challenge.aplazo_fullstack_challenge.services.LoanService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("Loan Controller Tests")
class LoanControllerTest {

        private MockMvc mockMvc;

        @Mock
        private LoanService loanService;

        @Mock
        private ErrorLogService errorLogService;

        @InjectMocks
        private LoanController loanController;

        private GlobalExceptionHandler globalExceptionHandler;

        private ObjectMapper objectMapper;

        private LoanRequest loanRequest;
        private LoanResponse loanResponse;
        private UUID loanId;
        private UUID customerId;

        @BeforeEach
        void setUp() {
                globalExceptionHandler = new GlobalExceptionHandler(errorLogService);
                mockMvc = MockMvcBuilders.standaloneSetup(loanController)
                                .setControllerAdvice(globalExceptionHandler)
                                .build();
                objectMapper = new ObjectMapper();
                objectMapper.findAndRegisterModules();

                loanId = UUID.randomUUID();
                customerId = UUID.randomUUID();

                loanRequest = new LoanRequest();
                loanRequest.setCustomerId(customerId);
                loanRequest.setAmount(new BigDecimal("1000.00"));

                // Create installments for payment plan
                List<Installment> installments = new ArrayList<>();
                LocalDate today = LocalDate.now();
                BigDecimal installmentAmount = new BigDecimal("210.00");

                for (int i = 0; i < 5; i++) {
                        installments.add(Installment.builder()
                                        .amount(installmentAmount)
                                        .scheduledPaymentDate(today.plusWeeks(2L * (i + 1)))
                                        .status(i == 0 ? InstallmentStatus.NEXT : InstallmentStatus.PENDING)
                                        .build());
                }

                PaymentPlan paymentPlan = PaymentPlan.builder()
                                .commissionAmount(new BigDecimal("50.00"))
                                .installments(installments)
                                .build();

                loanResponse = LoanResponse.builder()
                                .id(loanId)
                                .customerId(customerId)
                                .amount(new BigDecimal("1000.00"))
                                .status(LoanStatus.ACTIVE)
                                .createdAt(Instant.now())
                                .paymentPlan(paymentPlan)
                                .build();
        }

        @Test
        @DisplayName("POST /loans should create loan and return 201 with Location header")
        @WithMockUser
        void shouldCreateLoanAndReturn201WithLocationHeader() throws Exception {
                // Given
                when(loanService.createLoan(any(LoanRequest.class))).thenReturn(loanResponse);

                // When & Then
                mockMvc.perform(post("/loans")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loanRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(header().string("Location", "/v1/loans/" + loanId))
                                .andExpect(jsonPath("$.id").value(loanId.toString()))
                                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                                .andExpect(jsonPath("$.amount").value(1000.00))
                                .andExpect(jsonPath("$.status").value("ACTIVE"))
                                .andExpect(jsonPath("$.paymentPlan").exists())
                                .andExpect(jsonPath("$.paymentPlan.commissionAmount").value(50.00))
                                .andExpect(jsonPath("$.paymentPlan.installments").isArray())
                                .andExpect(jsonPath("$.paymentPlan.installments").hasJsonPath());
        }

        @Test
        @DisplayName("GET /loans/{id} should return loan details")
        @WithMockUser
        void shouldGetLoanByIdAndReturnDetails() throws Exception {
                // Given
                when(loanService.getLoanById(loanId)).thenReturn(loanResponse);

                // When & Then
                mockMvc.perform(get("/loans/{loanId}", loanId))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.id").value(loanId.toString()))
                                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                                .andExpect(jsonPath("$.amount").value(1000.00))
                                .andExpect(jsonPath("$.status").value("ACTIVE"))
                                .andExpect(jsonPath("$.paymentPlan.installments[0].status").value("NEXT"))
                                .andExpect(jsonPath("$.paymentPlan.installments[1].status").value("PENDING"));
        }

        @Test
        @DisplayName("POST /loans should return 404 when customer not found")
        @WithMockUser
        void shouldReturn404WhenCustomerNotFound() throws Exception {
                // Given
                when(loanService.createLoan(any(LoanRequest.class)))
                                .thenThrow(new CustomerNotFoundException("Customer not found"));

                // When & Then
                mockMvc.perform(post("/loans")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loanRequest)))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /loans/{id} should return 404 when loan not found")
        @WithMockUser
        void shouldReturn404WhenLoanNotFound() throws Exception {
                // Given
                UUID nonExistentId = UUID.randomUUID();
                when(loanService.getLoanById(nonExistentId))
                                .thenThrow(new LoanNotFoundException("Loan not found"));

                // When & Then
                mockMvc.perform(get("/loans/{loanId}", nonExistentId))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("POST /loans should return 400 for invalid request")
        @WithMockUser
        void shouldReturn400ForInvalidLoanRequest() throws Exception {
                // Given - Invalid loan request (missing required fields)
                LoanRequest invalidRequest = new LoanRequest();
                // customerId and amount are null (required)

                // When & Then
                mockMvc.perform(post("/loans")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /loans should return 400 for negative amount")
        @WithMockUser
        void shouldReturn400ForNegativeAmount() throws Exception {
                // Given - Loan request with negative amount
                LoanRequest negativeAmountRequest = new LoanRequest();
                negativeAmountRequest.setCustomerId(customerId);
                negativeAmountRequest.setAmount(new BigDecimal("-100.00"));

                // When & Then
                mockMvc.perform(post("/loans")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(negativeAmountRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /loans/{id} should return 500 for invalid UUID")
        @WithMockUser
        void shouldReturn500ForInvalidUUID() throws Exception {
                // When & Then
                mockMvc.perform(get("/loans/{loanId}", "invalid-uuid"))
                                .andExpect(status().isInternalServerError());
        }

        // Authorization tests are covered in integration tests
        // Unit tests focus on business logic only
}
