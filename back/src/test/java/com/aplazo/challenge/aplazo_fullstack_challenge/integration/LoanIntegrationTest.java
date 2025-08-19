package com.aplazo.challenge.aplazo_fullstack_challenge.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer.CustomerRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer.CustomerResponse;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.LoanRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.LoanResponse;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.InstallmentEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.enums.InstallmentStatus;
import com.aplazo.challenge.aplazo_fullstack_challenge.enums.LoanStatus;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.InstallmentRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.services.CustomerService;
import com.aplazo.challenge.aplazo_fullstack_challenge.services.LoanService;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Loan Integration Tests")
class LoanIntegrationTest {

    @Container
    @SuppressWarnings("resource") // Testcontainers automatically manages container lifecycle
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("challenge_db")
            .withUsername("challenge_user")
            .withPassword("challenge_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private CustomerService customerService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private InstallmentRepository installmentRepository;

    private CustomerResponse savedCustomer;

    @BeforeEach
    @Transactional
    void setUp() {
        // Create a test customer
        CustomerRequest customerRequest = new CustomerRequest();
        customerRequest.setFirstName("Juan");
        customerRequest.setLastName("Pérez");
        customerRequest.setSecondLastName("López");
        customerRequest.setDateOfBirth(LocalDate.of(1990, 5, 15));

        savedCustomer = customerService.createCustomer(customerRequest);
    }

    @Test
    @DisplayName("Should create customer and loan with installments end-to-end")
    @Transactional
    void shouldCreateCustomerAndLoanWithInstallmentsEndToEnd() {
        // Given
        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setCustomerId(savedCustomer.getId());
        loanRequest.setAmount(new BigDecimal("1000.00"));

        // When
        LoanResponse loanResponse = loanService.createLoan(loanRequest);

        // Then
        assertThat(loanResponse).isNotNull();
        assertThat(loanResponse.getId()).isNotNull();
        assertThat(loanResponse.getCustomerId()).isEqualTo(savedCustomer.getId());
        assertThat(loanResponse.getAmount()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(loanResponse.getStatus()).isEqualTo(LoanStatus.ACTIVE);

        // Verify installments were created
        assertThat(loanResponse.getPaymentPlan()).isNotNull();
        assertThat(loanResponse.getPaymentPlan().getInstallments()).hasSize(5);
        assertThat(loanResponse.getPaymentPlan().getCommissionAmount()).isEqualTo(new BigDecimal("50.00"));

        // Verify installments in database
        List<InstallmentEntity> installments = installmentRepository.findByLoanId(loanResponse.getId());
        assertThat(installments).hasSize(5);

        // Check first installment is NEXT, others are PENDING
        assertThat(installments.get(0).getStatus()).isEqualTo(InstallmentStatus.NEXT);
        for (int i = 1; i < 5; i++) {
            assertThat(installments.get(i).getStatus()).isEqualTo(InstallmentStatus.PENDING);
        }

        // Verify installment amounts
        BigDecimal totalAmount = new BigDecimal("1000.00").add(new BigDecimal("50.00")); // loan + commission
        BigDecimal expectedInstallmentAmount = totalAmount.divide(new BigDecimal("5"), 2,
                java.math.RoundingMode.HALF_UP);

        installments.forEach(installment -> {
            assertThat(installment.getAmount()).isEqualTo(expectedInstallmentAmount);
        });
    }

    @Test
    @DisplayName("Should retrieve loan by id with all data")
    @Transactional
    void shouldRetrieveLoanByIdWithAllData() {
        // Given - Create a loan first
        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setCustomerId(savedCustomer.getId());
        loanRequest.setAmount(new BigDecimal("500.00"));

        LoanResponse createdLoan = loanService.createLoan(loanRequest);

        // When
        LoanResponse retrievedLoan = loanService.getLoanById(createdLoan.getId());

        // Then
        assertThat(retrievedLoan).isNotNull();
        assertThat(retrievedLoan.getId()).isEqualTo(createdLoan.getId());
        assertThat(retrievedLoan.getCustomerId()).isEqualTo(savedCustomer.getId());
        assertThat(retrievedLoan.getAmount()).isEqualTo(new BigDecimal("500.00"));
        assertThat(retrievedLoan.getStatus()).isEqualTo(LoanStatus.ACTIVE);

        // Verify payment plan
        assertThat(retrievedLoan.getPaymentPlan()).isNotNull();
        assertThat(retrievedLoan.getPaymentPlan().getInstallments()).hasSize(5);
        assertThat(retrievedLoan.getPaymentPlan().getCommissionAmount()).isEqualTo(new BigDecimal("25.00")); // 5% of
                                                                                                             // 500
    }

    @Test
    @DisplayName("Should create customer with correct default credit line")
    @Transactional
    void shouldCreateCustomerWithCorrectDefaultCreditLine() {
        // Given
        CustomerRequest customerRequest = new CustomerRequest();
        customerRequest.setFirstName("Ana");
        customerRequest.setLastName("García");
        customerRequest.setSecondLastName("Martín");
        customerRequest.setDateOfBirth(LocalDate.of(1985, 8, 20));

        // When
        CustomerResponse customer = customerService.createCustomer(customerRequest);

        // Then
        assertThat(customer).isNotNull();
        assertThat(customer.getId()).isNotNull();
        assertThat(customer.getCreditLineAmount()).isEqualTo(new BigDecimal("1.00"));
        assertThat(customer.getAvailableCreditLineAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(customer.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should generate UUIDv7 for entities")
    @Transactional
    void shouldGenerateUuidV7ForEntities() {
        // Given
        CustomerRequest customerRequest = new CustomerRequest();
        customerRequest.setFirstName("Test");
        customerRequest.setLastName("User");
        customerRequest.setSecondLastName("UUIDv7");
        customerRequest.setDateOfBirth(LocalDate.of(1995, 1, 1));

        // When
        CustomerResponse customer = customerService.createCustomer(customerRequest);

        // Then
        assertThat(customer.getId()).isNotNull();
        assertThat(customer.getId().version()).isEqualTo(7); // Verify it's UUIDv7
    }

    @Test
    @DisplayName("Should validate installment payment dates are bi-weekly")
    @Transactional
    void shouldValidateInstallmentPaymentDatesAreBiWeekly() {
        // Given
        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setCustomerId(savedCustomer.getId());
        loanRequest.setAmount(new BigDecimal("200.00"));

        // When
        LoanResponse loanResponse = loanService.createLoan(loanRequest);

        // Then
        List<InstallmentEntity> installments = installmentRepository.findByLoanId(loanResponse.getId());
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 5; i++) {
            LocalDate expectedDate = today.plusWeeks(2L * (i + 1));
            assertThat(installments.get(i).getScheduledPaymentDate()).isEqualTo(expectedDate);
        }
    }
}
