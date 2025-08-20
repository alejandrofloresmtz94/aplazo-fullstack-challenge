package com.aplazo.challenge.aplazo_fullstack_challenge.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.LoanRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.LoanResponse;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.CustomerEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.InstallmentEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.LoanEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.enums.LoanStatus;
import com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception.CustomerNotFoundException;
import com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception.LoanNotFoundException;
import com.aplazo.challenge.aplazo_fullstack_challenge.mapper.LoanMapper;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.CustomerRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.LoanRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.services.InstallmentService;
import com.aplazo.challenge.aplazo_fullstack_challenge.services.impl.LoanServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("Loan Service Tests")
class LoanServiceTest {

        @Mock
        private LoanRepository loanRepository;

        @Mock
        private InstallmentService installmentService;

        @Mock
        private CustomerRepository customerRepository;

        @Mock
        private LoanMapper loanMapper;

        @InjectMocks
        private LoanServiceImpl loanService;

        private LoanRequest loanRequest;
        private CustomerEntity customerEntity;
        private LoanEntity loanEntity;
        private LoanResponse loanResponse;
        private List<InstallmentEntity> installments;
        private UUID customerId;
        private UUID loanId;

        @BeforeEach
        void setUp() {
                customerId = UUID.randomUUID();
                loanId = UUID.randomUUID();

                loanRequest = new LoanRequest();
                loanRequest.setCustomerId(customerId);
                loanRequest.setAmount(new BigDecimal("1000.00"));

                customerEntity = CustomerEntity.builder()
                                .id(customerId)
                                .firstName("Juan")
                                .lastName("Pérez")
                                .creditLineAmount(new BigDecimal("1.00"))
                                .availableCreditLineAmount(new BigDecimal("0.00"))
                                .build();

                loanEntity = LoanEntity.builder()
                                .id(loanId)
                                .customer(customerEntity)
                                .amount(new BigDecimal("1000.00"))
                                .status(LoanStatus.ACTIVE)
                                .commissionAmount(new BigDecimal("50.00"))
                                .createdAt(Instant.now())
                                .build();

                installments = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                        installments.add(InstallmentEntity.builder()
                                        .id(UUID.randomUUID())
                                        .loan(loanEntity)
                                        .amount(new BigDecimal("210.00"))
                                        .build());
                }

                loanResponse = LoanResponse.builder()
                                .id(loanId)
                                .customerId(customerId)
                                .amount(new BigDecimal("1000.00"))
                                .status(LoanStatus.ACTIVE)
                                .createdAt(Instant.now())
                                .build();
        }

        @Test
        @DisplayName("Should create loan successfully")
        void shouldCreateLoanSuccessfully() {
                // Given
                when(customerRepository.findById(customerId)).thenReturn(Optional.of(customerEntity));
                when(loanMapper.toEntity(loanRequest)).thenReturn(loanEntity);
                when(loanRepository.save(any(LoanEntity.class))).thenReturn(loanEntity);
                when(installmentService.generateInstallments(any(LoanEntity.class), any(BigDecimal.class),
                                any(BigDecimal.class)))
                                .thenReturn(installments);
                when(installmentService.saveInstallments(anyList())).thenReturn(installments);
                when(loanMapper.toResponse(any(LoanEntity.class))).thenReturn(loanResponse);

                // When
                LoanResponse result = loanService.createLoan(loanRequest);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getId()).isEqualTo(loanId);
                assertThat(result.getCustomerId()).isEqualTo(customerId);
                assertThat(result.getAmount()).isEqualTo(new BigDecimal("1000.00"));
                assertThat(result.getStatus()).isEqualTo(LoanStatus.ACTIVE);

                verify(customerRepository).findById(customerId);
                verify(loanMapper).toEntity(loanRequest);
                verify(loanRepository).save(any(LoanEntity.class));
                verify(installmentService).generateInstallments(any(LoanEntity.class), any(BigDecimal.class),
                                any(BigDecimal.class));
                verify(installmentService).saveInstallments(anyList());
                verify(loanMapper).toResponse(any(LoanEntity.class));
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException when customer not found")
        void shouldThrowCustomerNotFoundExceptionWhenCustomerNotFound() {
                // Given
                when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> loanService.createLoan(loanRequest))
                                .isInstanceOf(CustomerNotFoundException.class)
                                .hasMessage("Customer not found");

                verify(customerRepository).findById(customerId);
        }

        @Test
        @DisplayName("Should get loan by id successfully")
        void shouldGetLoanByIdSuccessfully() {
                // Given
                when(loanRepository.findById(loanId)).thenReturn(Optional.of(loanEntity));
                when(loanMapper.toResponse(loanEntity)).thenReturn(loanResponse);

                // When
                LoanResponse result = loanService.getLoanById(loanId);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getId()).isEqualTo(loanId);

                verify(loanRepository).findById(loanId);
                verify(loanMapper).toResponse(loanEntity);
        }

        @Test
        @DisplayName("Should throw LoanNotFoundException when loan not found")
        void shouldThrowLoanNotFoundExceptionWhenLoanNotFound() {
                // Given
                when(loanRepository.findById(loanId)).thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> loanService.getLoanById(loanId))
                                .isInstanceOf(LoanNotFoundException.class)
                                .hasMessageContaining("Préstamo no encontrado con ID: " + loanId);

                verify(loanRepository).findById(loanId);
        }

        @Test
        @DisplayName("Should set loan status to ACTIVE when creating")
        void shouldSetLoanStatusToActiveWhenCreating() {
                // Given
                when(customerRepository.findById(customerId)).thenReturn(Optional.of(customerEntity));
                when(loanMapper.toEntity(loanRequest)).thenReturn(loanEntity);
                when(loanRepository.save(any(LoanEntity.class))).thenReturn(loanEntity);
                when(installmentService.generateInstallments(any(LoanEntity.class), any(BigDecimal.class),
                                any(BigDecimal.class)))
                                .thenReturn(installments);
                when(installmentService.saveInstallments(anyList())).thenReturn(installments);
                when(loanMapper.toResponse(any(LoanEntity.class))).thenReturn(loanResponse);

                // When
                LoanResponse result = loanService.createLoan(loanRequest);

                // Then
                assertThat(result.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        }
}
