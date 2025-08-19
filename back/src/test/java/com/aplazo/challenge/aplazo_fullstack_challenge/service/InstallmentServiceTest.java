package com.aplazo.challenge.aplazo_fullstack_challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aplazo.challenge.aplazo_fullstack_challenge.entity.InstallmentEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.LoanEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.enums.InstallmentStatus;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.InstallmentRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.services.impl.InstallmentServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("Installment Service Tests")
class InstallmentServiceTest {

    @Mock
    private InstallmentRepository installmentRepository;
    
    @InjectMocks
    private InstallmentServiceImpl installmentService;
    
    private LoanEntity loanEntity;
    private BigDecimal loanAmount;
    private BigDecimal commissionAmount;
    
    @BeforeEach
    void setUp() {
        loanEntity = LoanEntity.builder()
            .id(UUID.randomUUID())
            .amount(new BigDecimal("1000.00"))
            .commissionAmount(new BigDecimal("50.00"))
            .build();
            
        loanAmount = new BigDecimal("1000.00");
        commissionAmount = new BigDecimal("50.00");
    }
    
    @Test
    @DisplayName("Should generate 5 installments with correct amounts")
    void shouldGenerate5InstallmentsWithCorrectAmounts() {
        // When
        List<InstallmentEntity> result = installmentService.generateInstallments(loanEntity, loanAmount, commissionAmount);
        
        // Then
        assertThat(result).hasSize(5);
        
        BigDecimal totalAmount = loanAmount.add(commissionAmount);
        BigDecimal expectedInstallmentAmount = totalAmount.divide(new BigDecimal("5"), 2, RoundingMode.HALF_UP);
        
        result.forEach(installment -> {
            assertThat(installment.getAmount()).isEqualTo(expectedInstallmentAmount);
            assertThat(installment.getLoan()).isEqualTo(loanEntity);
        });
    }
    
    @Test
    @DisplayName("Should generate installments with correct payment dates")
    void shouldGenerateInstallmentsWithCorrectPaymentDates() {
        // Given
        LocalDate today = LocalDate.now();
        
        // When
        List<InstallmentEntity> result = installmentService.generateInstallments(loanEntity, loanAmount, commissionAmount);
        
        // Then
        assertThat(result).hasSize(5);
        
        for (int i = 0; i < 5; i++) {
            LocalDate expectedDate = today.plusWeeks(2L * (i + 1));
            assertThat(result.get(i).getScheduledPaymentDate()).isEqualTo(expectedDate);
        }
    }
    
    @Test
    @DisplayName("Should set first installment as NEXT and others as PENDING")
    void shouldSetFirstInstallmentAsNextAndOthersAsPending() {
        // When
        List<InstallmentEntity> result = installmentService.generateInstallments(loanEntity, loanAmount, commissionAmount);
        
        // Then
        assertThat(result.get(0).getStatus()).isEqualTo(InstallmentStatus.NEXT);
        
        for (int i = 1; i < 5; i++) {
            assertThat(result.get(i).getStatus()).isEqualTo(InstallmentStatus.PENDING);
        }
    }
    
    @Test
    @DisplayName("Should save installments successfully")
    void shouldSaveInstallmentsSuccessfully() {
        // Given
        List<InstallmentEntity> installments = installmentService.generateInstallments(loanEntity, loanAmount, commissionAmount);
        when(installmentRepository.saveAll(anyList())).thenReturn(installments);
        
        // When
        List<InstallmentEntity> result = installmentService.saveInstallments(installments);
        
        // Then
        assertThat(result).hasSize(5);
        verify(installmentRepository).saveAll(installments);
    }
    
    @Test
    @DisplayName("Should calculate correct installment amount for edge case")
    void shouldCalculateCorrectInstallmentAmountForEdgeCase() {
        // Given - Amount that doesn't divide evenly
        BigDecimal unevenLoanAmount = new BigDecimal("100.00");
        BigDecimal unevenCommission = new BigDecimal("3.33");
        
        // When
        List<InstallmentEntity> result = installmentService.generateInstallments(loanEntity, unevenLoanAmount, unevenCommission);
        
        // Then
        BigDecimal totalAmount = unevenLoanAmount.add(unevenCommission); // 103.33
        BigDecimal expectedInstallmentAmount = totalAmount.divide(new BigDecimal("5"), 2, RoundingMode.HALF_UP); // 20.67
        
        result.forEach(installment -> {
            assertThat(installment.getAmount()).isEqualTo(expectedInstallmentAmount);
        });
    }
}
