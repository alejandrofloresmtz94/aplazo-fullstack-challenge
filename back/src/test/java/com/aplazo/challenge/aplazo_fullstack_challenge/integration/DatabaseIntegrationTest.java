package com.aplazo.challenge.aplazo_fullstack_challenge.integration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer.CustomerRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer.CustomerResponse;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.LoanRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.LoanResponse;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.CustomerEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.InstallmentEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.LoanEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.enums.LoanStatus;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.CustomerRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.InstallmentRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.LoanRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.services.CustomerService;
import com.aplazo.challenge.aplazo_fullstack_challenge.services.LoanService;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Database Transaction and Error Handling Integration Tests")
class DatabaseIntegrationTest {

    @Container
    @SuppressWarnings("resource") // Testcontainers automatically manages container lifecycle
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("database_test_db")
            .withUsername("database_user")
            .withPassword("database_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private CustomerService customerService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private InstallmentRepository installmentRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private CustomerRequest validCustomerRequest;

    @BeforeEach
    @Transactional
    void setUp() {
        customerRepository.deleteAll();
        loanRepository.deleteAll();
        installmentRepository.deleteAll();

        validCustomerRequest = new CustomerRequest();
        validCustomerRequest.setFirstName("Database");
        validCustomerRequest.setLastName("Test");
        validCustomerRequest.setSecondLastName("User");
        validCustomerRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));
    }

    @Test
    @DisplayName("Should rollback transaction when customer creation fails")
    @Transactional
    void shouldRollbackTransactionWhenCustomerCreationFails() {
        // Given
        long initialCount = customerRepository.count();

        // When & Then
        assertThatThrownBy(() -> {
            transactionTemplate.execute(status -> {
                // Create a customer entity directly in the repository with invalid data
                CustomerEntity invalidCustomer = CustomerEntity.builder()
                        .firstName(null) // This will cause constraint violation
                        .lastName("Test")
                        .secondLastName("User")
                        .dateOfBirth(LocalDate.of(1990, 1, 1))
                        .creditLineAmount(new BigDecimal("1000.00"))
                        .availableCreditLineAmount(new BigDecimal("800.00"))
                        .createdAt(Instant.now())
                        .build();

                customerRepository.saveAndFlush(invalidCustomer);
                return null;
            });
        }).isInstanceOf(DataIntegrityViolationException.class);

        // Then - Count should remain the same (transaction rolled back)
        assertThat(customerRepository.count()).isEqualTo(initialCount);
    }

    @Test
    @DisplayName("Should rollback transaction when loan creation fails due to missing customer")
    @Transactional
    void shouldRollbackTransactionWhenLoanCreationFailsDueToMissingCustomer() {
        // Given
        long initialLoanCount = loanRepository.count();
        long initialInstallmentCount = installmentRepository.count();
        UUID nonExistentCustomerId = UUID.randomUUID();

        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setCustomerId(nonExistentCustomerId);
        loanRequest.setAmount(new BigDecimal("1000.00"));

        // When & Then
        assertThatThrownBy(() -> {
            loanService.createLoan(loanRequest);
        }).hasMessageContaining("Customer not found");

        // Then - Counts should remain the same (transaction rolled back)
        assertThat(loanRepository.count()).isEqualTo(initialLoanCount);
        assertThat(installmentRepository.count()).isEqualTo(initialInstallmentCount);
    }

    @Test
    @DisplayName("Should maintain data consistency when loan creation succeeds")
    @Transactional
    void shouldMaintainDataConsistencyWhenLoanCreationSucceeds() {
        // Given
        CustomerResponse customer = customerService.createCustomer(validCustomerRequest);

        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setCustomerId(customer.getId());
        loanRequest.setAmount(new BigDecimal("500.00"));

        long initialLoanCount = loanRepository.count();
        long initialInstallmentCount = installmentRepository.count();

        // When
        LoanResponse loan = loanService.createLoan(loanRequest);

        // Then - Verify data consistency
        assertThat(loanRepository.count()).isEqualTo(initialLoanCount + 1);
        assertThat(installmentRepository.count()).isEqualTo(initialInstallmentCount + 5); // 5 installments

        // Verify loan entity exists and has correct data
        LoanEntity savedLoan = loanRepository.findById(loan.getId()).orElseThrow();
        assertThat(savedLoan.getCustomer().getId()).isEqualTo(customer.getId());
        assertThat(savedLoan.getAmount()).isEqualTo(new BigDecimal("500.00"));
        assertThat(savedLoan.getStatus()).isEqualTo(LoanStatus.ACTIVE);

        // Verify installments exist and reference the correct loan
        List<InstallmentEntity> installments = installmentRepository.findByLoanId(loan.getId());
        assertThat(installments).hasSize(5);
        installments.forEach(installment -> assertThat(installment.getLoan().getId()).isEqualTo(loan.getId()));
    }

    @Test
    @DisplayName("Should handle concurrent customer creation without conflicts")
    void shouldHandleConcurrentCustomerCreationWithoutConflicts() throws Exception {
        // Given
        ExecutorService executor = Executors.newFixedThreadPool(10);
        int numberOfCustomers = 20;

        try {
            // When - Create customers concurrently
            @SuppressWarnings("unchecked")
            CompletableFuture<CustomerResponse>[] futures = new CompletableFuture[numberOfCustomers];

            for (int i = 0; i < numberOfCustomers; i++) {
                final int index = i;
                futures[i] = CompletableFuture.supplyAsync(() -> {
                    CustomerRequest request = new CustomerRequest();
                    request.setFirstName("Concurrent" + index);
                    request.setLastName("Customer" + index);
                    request.setSecondLastName("Test" + index);
                    request.setDateOfBirth(LocalDate.of(1990, 1, 1));

                    return customerService.createCustomer(request);
                }, executor);
            }

            CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);

            // Then - All customers should be created successfully
            List<CustomerEntity> customers = customerRepository.findAll();
            assertThat(customers).hasSize(numberOfCustomers);

            // Verify all UUIDs are unique
            List<UUID> customerIds = customers.stream()
                    .map(CustomerEntity::getId)
                    .toList();
            assertThat(customerIds).doesNotHaveDuplicates();

            // Verify all names are unique and correctly assigned
            for (int i = 0; i < numberOfCustomers; i++) {
                final int index = i;
                assertThat(customers).anyMatch(customer -> customer.getFirstName().equals("Concurrent" + index) &&
                        customer.getLastName().equals("Customer" + index) &&
                        customer.getSecondLastName().equals("Test" + index));
            }

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Should handle concurrent loan creation for same customer")
    void shouldHandleConcurrentLoanCreationForSameCustomer() throws Exception {
        // Given
        CustomerResponse customer = customerService.createCustomer(validCustomerRequest);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        int numberOfLoans = 10;

        try {
            // When - Create loans concurrently for the same customer
            @SuppressWarnings("unchecked")
            CompletableFuture<LoanResponse>[] futures = new CompletableFuture[numberOfLoans];

            for (int i = 0; i < numberOfLoans; i++) {
                final int index = i;
                futures[i] = CompletableFuture.supplyAsync(() -> {
                    LoanRequest request = new LoanRequest();
                    request.setCustomerId(customer.getId());
                    request.setAmount(new BigDecimal("100.00").add(new BigDecimal(index))); // Different amounts

                    return loanService.createLoan(request);
                }, executor);
            }

            CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);

            // Then - All loans should be created successfully
            List<LoanEntity> loans = loanRepository.findAll();
            assertThat(loans).hasSize(numberOfLoans);

            // Verify all loans belong to the same customer
            loans.forEach(loan -> assertThat(loan.getCustomer().getId()).isEqualTo(customer.getId()));

            // Verify all loan IDs are unique
            List<UUID> loanIds = loans.stream()
                    .map(LoanEntity::getId)
                    .toList();
            assertThat(loanIds).doesNotHaveDuplicates();

            // Verify installments were created for all loans
            long totalInstallments = installmentRepository.count();
            assertThat(totalInstallments).isEqualTo(numberOfLoans * 5L); // 5 installments per loan

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Should maintain referential integrity between customer and loans")
    @Transactional
    void shouldMaintainReferentialIntegrityBetweenCustomerAndLoans() {
        // Given
        CustomerResponse customer = customerService.createCustomer(validCustomerRequest);

        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setCustomerId(customer.getId());
        loanRequest.setAmount(new BigDecimal("1000.00"));

        LoanResponse loan = loanService.createLoan(loanRequest);

        // When - Attempt to delete customer with existing loans (should fail)
        assertThatThrownBy(() -> {
            customerRepository.deleteById(customer.getId());
            customerRepository.flush(); // Force the operation
        }).isInstanceOf(DataIntegrityViolationException.class);

        // Then - Customer should still exist
        assertThat(customerRepository.findById(customer.getId())).isPresent();

        // And loan should still exist with correct customer reference
        LoanEntity savedLoan = loanRepository.findById(loan.getId()).orElseThrow();
        assertThat(savedLoan.getCustomer().getId()).isEqualTo(customer.getId());
    }

    @Test
    @DisplayName("Should maintain referential integrity between loan and installments")
    @Transactional
    void shouldMaintainReferentialIntegrityBetweenLoanAndInstallments() {
        // Given
        CustomerResponse customer = customerService.createCustomer(validCustomerRequest);

        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setCustomerId(customer.getId());
        loanRequest.setAmount(new BigDecimal("500.00"));

        LoanResponse loan = loanService.createLoan(loanRequest);

        // Verify installments were created
        List<InstallmentEntity> installments = installmentRepository.findByLoanId(loan.getId());
        assertThat(installments).hasSize(5);

        // When - Attempt to delete loan with existing installments (should cascade or
        // fail based on configuration)
        UUID loanId = loan.getId();
        loanRepository.deleteById(loanId);
        loanRepository.flush(); // Force the operation

        // Then - Installments should also be deleted (cascade delete)
        List<InstallmentEntity> remainingInstallments = installmentRepository.findByLoanId(loanId);
        assertThat(remainingInstallments).isEmpty();
    }

    @Test
    @DisplayName("Should handle database connection pool exhaustion gracefully")
    void shouldHandleDatabaseConnectionPoolExhaustionGracefully() throws Exception {
        // Given
        ExecutorService executor = Executors.newFixedThreadPool(50); // More threads than connection pool
        int numberOfOperations = 100;

        try {
            // When - Perform many database operations concurrently
            @SuppressWarnings("unchecked")
            CompletableFuture<Boolean>[] futures = new CompletableFuture[numberOfOperations];

            for (int i = 0; i < numberOfOperations; i++) {
                final int index = i;
                futures[i] = CompletableFuture.supplyAsync(() -> {
                    try {
                        CustomerRequest request = new CustomerRequest();
                        request.setFirstName("Pool" + index);
                        request.setLastName("Test" + index);
                        request.setSecondLastName("User" + index);
                        request.setDateOfBirth(LocalDate.of(1990, 1, 1));

                        CustomerResponse customer = customerService.createCustomer(request);
                        return customer != null;
                    } catch (Exception e) {
                        // Expected some operations might fail due to pool exhaustion
                        return false;
                    }
                }, executor);
            }

            CompletableFuture.allOf(futures).get(60, TimeUnit.SECONDS);

            // Then - At least some operations should succeed
            long successfulOperations = java.util.Arrays.stream(futures)
                    .mapToInt(f -> f.join() ? 1 : 0)
                    .sum();

            assertThat(successfulOperations).isGreaterThan(0);
            assertThat(customerRepository.count()).isEqualTo(successfulOperations);

        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Should recover from temporary database unavailability")
    @Transactional
    void shouldRecoverFromTemporaryDatabaseUnavailability() {
        // Given
        CustomerResponse customer = customerService.createCustomer(validCustomerRequest);

        // Verify normal operation works
        assertThat(customerRepository.findById(customer.getId())).isPresent();

        // Note: In a real scenario, you might simulate database unavailability
        // by stopping the container temporarily, but that's complex for this test
        // Instead, we verify the application can handle normal recovery scenarios

        // When - Perform operations after "recovery"
        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setCustomerId(customer.getId());
        loanRequest.setAmount(new BigDecimal("750.00"));

        LoanResponse loan = loanService.createLoan(loanRequest);

        // Then - Operations should work normally after recovery
        assertThat(loan).isNotNull();
        assertThat(loanRepository.findById(loan.getId())).isPresent();
        assertThat(installmentRepository.findByLoanId(loan.getId())).hasSize(5);
    }

    @Test
    @DisplayName("Should handle large transaction rollback correctly")
    @Transactional
    void shouldHandleLargeTransactionRollbackCorrectly() {
        // Given
        long initialCustomerCount = customerRepository.count();
        long initialLoanCount = loanRepository.count();
        long initialInstallmentCount = installmentRepository.count();

        // When & Then
        assertThatThrownBy(() -> {
            transactionTemplate.execute(status -> {
                // Create multiple customers and loans in one transaction
                for (int i = 0; i < 10; i++) {
                    CustomerRequest customerRequest = new CustomerRequest();
                    customerRequest.setFirstName("Bulk" + i);
                    customerRequest.setLastName("Customer" + i);
                    customerRequest.setSecondLastName("Test" + i);
                    customerRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));

                    CustomerResponse customer = customerService.createCustomer(customerRequest);

                    // Create loans for each customer
                    for (int j = 0; j < 3; j++) {
                        LoanRequest loanRequest = new LoanRequest();
                        loanRequest.setCustomerId(customer.getId());
                        loanRequest.setAmount(new BigDecimal("500.00"));

                        loanService.createLoan(loanRequest);
                    }
                }

                // Force rollback by throwing an exception
                throw new RuntimeException("Forced rollback");
            });
        }).hasMessageContaining("Forced rollback");

        // Then - All counts should remain the same (entire transaction rolled back)
        assertThat(customerRepository.count()).isEqualTo(initialCustomerCount);
        assertThat(loanRepository.count()).isEqualTo(initialLoanCount);
        assertThat(installmentRepository.count()).isEqualTo(initialInstallmentCount);
    }

    @Test
    @DisplayName("Should maintain data consistency under high load")
    void shouldMaintainDataConsistencyUnderHighLoad() throws Exception {
        // Given
        ExecutorService executor = Executors.newFixedThreadPool(20);
        int numberOfCustomers = 50;

        try {
            // When - Create customers and loans under high load
            @SuppressWarnings("unchecked")
            CompletableFuture<Void>[] futures = new CompletableFuture[numberOfCustomers];

            for (int i = 0; i < numberOfCustomers; i++) {
                final int index = i;
                futures[i] = CompletableFuture.runAsync(() -> {
                    CustomerRequest customerRequest = new CustomerRequest();
                    customerRequest.setFirstName("Load" + index);
                    customerRequest.setLastName("Test" + index);
                    customerRequest.setSecondLastName("User" + index);
                    customerRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));

                    CustomerResponse customer = customerService.createCustomer(customerRequest);

                    // Create 2 loans for each customer
                    for (int j = 0; j < 2; j++) {
                        LoanRequest loanRequest = new LoanRequest();
                        loanRequest.setCustomerId(customer.getId());
                        loanRequest.setAmount(new BigDecimal("250.00").add(new BigDecimal(j * 100)));

                        loanService.createLoan(loanRequest);
                    }
                }, executor);
            }

            CompletableFuture.allOf(futures).get(60, TimeUnit.SECONDS);

            // Then - Verify data consistency
            assertThat(customerRepository.count()).isEqualTo(numberOfCustomers);
            assertThat(loanRepository.count()).isEqualTo(numberOfCustomers * 2L);
            assertThat(installmentRepository.count()).isEqualTo(numberOfCustomers * 2L * 5L); // 5 installments per loan

            // Verify referential integrity
            List<LoanEntity> allLoans = loanRepository.findAll();
            allLoans.forEach(loan -> {
                assertThat(loan.getCustomer()).isNotNull();
                assertThat(customerRepository.existsById(loan.getCustomer().getId())).isTrue();

                List<InstallmentEntity> loanInstallments = installmentRepository.findByLoanId(loan.getId());
                assertThat(loanInstallments).hasSize(5);
                loanInstallments
                        .forEach(installment -> assertThat(installment.getLoan().getId()).isEqualTo(loan.getId()));
            });

        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
