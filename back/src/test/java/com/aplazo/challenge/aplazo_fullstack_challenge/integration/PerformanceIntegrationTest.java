package com.aplazo.challenge.aplazo_fullstack_challenge.integration;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.CustomerEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.CustomerRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.InstallmentRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.LoanRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.services.CustomerService;
import com.aplazo.challenge.aplazo_fullstack_challenge.services.LoanService;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Performance and Load Integration Tests")
class PerformanceIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceIntegrationTest.class);

    @Container
    @SuppressWarnings("resource") // Testcontainers automatically manages container lifecycle
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("perf_test_db")
            .withUsername("perf_user")
            .withPassword("perf_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // Optimize connection pool for performance tests
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "20");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "10");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "20000");
        registry.add("spring.datasource.hikari.idle-timeout", () -> "300000");
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

    @BeforeEach
    @Transactional
    void setUp() {
        customerRepository.deleteAll();
        loanRepository.deleteAll();
        installmentRepository.deleteAll();
    }

    @Test
    @DisplayName("Should handle high volume customer creation efficiently")
    void shouldHandleHighVolumeCustomerCreationEfficiently() throws Exception {
        // Given
        int numberOfCustomers = 1000;
        int threadPoolSize = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

        // Performance tracking
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);

        try {
            Instant startTime = Instant.now();

            // When - Create customers concurrently
            @SuppressWarnings("unchecked")
            CompletableFuture<Void>[] futures = new CompletableFuture[numberOfCustomers];

            for (int i = 0; i < numberOfCustomers; i++) {
                final int index = i;
                futures[i] = CompletableFuture.runAsync(() -> {
                    Instant requestStart = Instant.now();
                    try {
                        CustomerRequest request = new CustomerRequest();
                        request.setFirstName("PerformanceTest" + index);
                        request.setLastName("Customer" + index);
                        request.setSecondLastName("User" + index);
                        request.setDateOfBirth(LocalDate.of(1990, 1, 1));

                        CustomerResponse response = customerService.createCustomer(request);
                        if (response != null) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        logger.warn("Error creating customer {}: {}", index, e.getMessage());
                    } finally {
                        long responseTime = Duration.between(requestStart, Instant.now()).toMillis();
                        totalResponseTime.addAndGet(responseTime);
                    }
                }, executor);
            }

            CompletableFuture.allOf(futures).get(120, TimeUnit.SECONDS);
            Instant endTime = Instant.now();

            // Then - Verify performance metrics
            long totalDuration = Duration.between(startTime, endTime).toMillis();
            double throughput = (double) numberOfCustomers / (totalDuration / 1000.0);
            double averageResponseTime = (double) totalResponseTime.get() / numberOfCustomers;
            double errorRate = (double) errorCount.get() / numberOfCustomers * 100;

            logger.info("Performance Metrics:");
            logger.info("- Total customers processed: {}", numberOfCustomers);
            logger.info("- Successful operations: {}", successCount.get());
            logger.info("- Failed operations: {}", errorCount.get());
            logger.info("- Total duration: {} ms", totalDuration);
            logger.info("- Throughput: {:.2f} requests/second", throughput);
            logger.info("- Average response time: {:.2f} ms", averageResponseTime);
            logger.info("- Error rate: {:.2f}%", errorRate);

            // Performance assertions
            assertThat(successCount.get()).isGreaterThan((int) (numberOfCustomers * 0.95)); // At least 95% success rate
            assertThat(errorRate).isLessThan(5.0); // Less than 5% error rate
            assertThat(throughput).isGreaterThan(10.0); // At least 10 requests per second
            assertThat(averageResponseTime).isLessThan(5000.0); // Average response time under 5 seconds

            // Verify database consistency
            long actualCustomerCount = customerRepository.count();
            assertThat(actualCustomerCount).isEqualTo(successCount.get());

        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Should handle high volume loan creation efficiently")
    void shouldHandleHighVolumeLoanCreationEfficiently() throws Exception {
        // Given - Create customers first
        int numberOfCustomers = 100;
        int loansPerCustomer = 5;
        int totalLoans = numberOfCustomers * loansPerCustomer;

        List<CustomerResponse> customers = new ArrayList<>();
        for (int i = 0; i < numberOfCustomers; i++) {
            CustomerRequest request = new CustomerRequest();
            request.setFirstName("Loan" + i);
            request.setLastName("Customer" + i);
            request.setSecondLastName("Test" + i);
            request.setDateOfBirth(LocalDate.of(1990, 1, 1));

            customers.add(customerService.createCustomer(request));
        }

        // Performance tracking
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);

        ExecutorService executor = Executors.newFixedThreadPool(20);

        try {
            Instant startTime = Instant.now();

            // When - Create loans concurrently
            @SuppressWarnings("unchecked")
            CompletableFuture<Void>[] futures = new CompletableFuture[totalLoans];
            int futureIndex = 0;

            for (CustomerResponse customer : customers) {
                for (int j = 0; j < loansPerCustomer; j++) {
                    final int loanIndex = j;
                    futures[futureIndex++] = CompletableFuture.runAsync(() -> {
                        Instant requestStart = Instant.now();
                        try {
                            LoanRequest request = new LoanRequest();
                            request.setCustomerId(customer.getId());
                            request.setAmount(new BigDecimal("100.00").add(new BigDecimal(loanIndex * 50)));

                            LoanResponse response = loanService.createLoan(request);
                            if (response != null) {
                                successCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            logger.warn("Error creating loan for customer {}: {}", customer.getId(), e.getMessage());
                        } finally {
                            long responseTime = Duration.between(requestStart, Instant.now()).toMillis();
                            totalResponseTime.addAndGet(responseTime);
                        }
                    }, executor);
                }
            }

            CompletableFuture.allOf(futures).get(180, TimeUnit.SECONDS);
            Instant endTime = Instant.now();

            // Then - Verify performance metrics
            long totalDuration = Duration.between(startTime, endTime).toMillis();
            double throughput = (double) totalLoans / (totalDuration / 1000.0);
            double averageResponseTime = (double) totalResponseTime.get() / totalLoans;
            double errorRate = (double) errorCount.get() / totalLoans * 100;

            logger.info("Loan Creation Performance Metrics:");
            logger.info("- Total loans processed: {}", totalLoans);
            logger.info("- Successful operations: {}", successCount.get());
            logger.info("- Failed operations: {}", errorCount.get());
            logger.info("- Total duration: {} ms", totalDuration);
            logger.info("- Throughput: {:.2f} requests/second", throughput);
            logger.info("- Average response time: {:.2f} ms", averageResponseTime);
            logger.info("- Error rate: {:.2f}%", errorRate);

            // Performance assertions
            assertThat(successCount.get()).isGreaterThan((int) (totalLoans * 0.95)); // At least 95% success rate
            assertThat(errorRate).isLessThan(5.0); // Less than 5% error rate
            assertThat(throughput).isGreaterThan(5.0); // At least 5 requests per second
            assertThat(averageResponseTime).isLessThan(10000.0); // Average response time under 10 seconds

            // Verify database consistency
            long actualLoanCount = loanRepository.count();
            long actualInstallmentCount = installmentRepository.count();
            assertThat(actualLoanCount).isEqualTo(successCount.get());
            assertThat(actualInstallmentCount).isEqualTo(successCount.get() * 5L); // 5 installments per loan

        } finally {
            executor.shutdown();
            executor.awaitTermination(15, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Should maintain performance under sustained load")
    void shouldMaintainPerformanceUnderSustainedLoad() throws Exception {
        // Given
        int loadDurationSeconds = 30;
        int threadPoolSize = 15;
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

        // Performance tracking
        AtomicInteger operationCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> responseTimes = new ArrayList<>();

        try {
            Instant startTime = Instant.now();
            Instant endTime = startTime.plusSeconds(loadDurationSeconds);

            // When - Generate sustained load
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            while (Instant.now().isBefore(endTime)) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    int currentOp = operationCount.incrementAndGet();
                    Instant requestStart = Instant.now();

                    try {
                        if (currentOp % 3 == 0) {
                            // Create loan (requires existing customer)
                            // First ensure we have customers
                            List<CustomerEntity> existingCustomers = customerRepository.findAll();
                            if (!existingCustomers.isEmpty()) {
                                CustomerEntity randomCustomer = existingCustomers
                                        .get(currentOp % existingCustomers.size());

                                LoanRequest loanRequest = new LoanRequest();
                                loanRequest.setCustomerId(randomCustomer.getId());
                                loanRequest.setAmount(new BigDecimal("200.00"));

                                loanService.createLoan(loanRequest);
                            } else {
                                // Create customer instead if none exist
                                createTestCustomer(currentOp);
                            }
                        } else {
                            // Create customer
                            createTestCustomer(currentOp);
                        }
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        logger.warn("Error in sustained load operation {}: {}", currentOp, e.getMessage());
                    } finally {
                        long responseTime = Duration.between(requestStart, Instant.now()).toMillis();
                        synchronized (responseTimes) {
                            responseTimes.add(responseTime);
                        }
                    }
                }, executor);

                futures.add(future);

                // Small delay to control load generation rate
                Thread.sleep(50);
            }

            // Wait for all operations to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(60, TimeUnit.SECONDS);
            Instant actualEndTime = Instant.now();

            // Then - Analyze performance metrics
            long totalDuration = Duration.between(startTime, actualEndTime).toMillis();
            double throughput = (double) operationCount.get() / (totalDuration / 1000.0);
            double errorRate = (double) errorCount.get() / operationCount.get() * 100;

            synchronized (responseTimes) {
                double averageResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
                long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0L);
                long minResponseTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0L);

                logger.info("Sustained Load Performance Metrics:");
                logger.info("- Load duration: {} seconds", loadDurationSeconds);
                logger.info("- Total operations: {}", operationCount.get());
                logger.info("- Successful operations: {}", successCount.get());
                logger.info("- Failed operations: {}", errorCount.get());
                logger.info("- Throughput: {:.2f} requests/second", throughput);
                logger.info("- Average response time: {:.2f} ms", averageResponseTime);
                logger.info("- Min response time: {} ms", minResponseTime);
                logger.info("- Max response time: {} ms", maxResponseTime);
                logger.info("- Error rate: {:.2f}%", errorRate);

                // Performance assertions
                assertThat(operationCount.get()).isGreaterThan(0);
                assertThat(errorRate).isLessThan(10.0); // Less than 10% error rate under sustained load
                assertThat(throughput).isGreaterThan(1.0); // At least 1 request per second
                assertThat(averageResponseTime).isLessThan(15000.0); // Average response time under 15 seconds
                assertThat(maxResponseTime).isLessThan(30000L); // Max response time under 30 seconds
            }

        } finally {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Should handle memory usage efficiently during bulk operations")
    void shouldHandleMemoryUsageEfficientlyDuringBulkOperations() throws Exception {
        // Given
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        int batchSize = 500;
        int numberOfBatches = 10;

        logger.info("Initial memory usage: {} MB", initialMemory / 1024 / 1024);

        try {
            // When - Process multiple batches of operations
            for (int batch = 0; batch < numberOfBatches; batch++) {
                logger.info("Processing batch {} of {}", batch + 1, numberOfBatches);

                List<CompletableFuture<CustomerResponse>> futures = new ArrayList<>();

                // Create a batch of customers
                for (int i = 0; i < batchSize; i++) {
                    final int index = batch * batchSize + i;
                    CompletableFuture<CustomerResponse> future = CompletableFuture.supplyAsync(() -> {
                        CustomerRequest request = new CustomerRequest();
                        request.setFirstName("Batch" + index);
                        request.setLastName("Customer" + index);
                        request.setSecondLastName("Test" + index);
                        request.setDateOfBirth(LocalDate.of(1990, 1, 1));

                        return customerService.createCustomer(request);
                    });
                    futures.add(future);
                }

                // Wait for batch to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);

                // Check memory usage after each batch
                System.gc(); // Suggest garbage collection
                Thread.sleep(1000); // Give GC time to run

                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                long memoryIncrease = currentMemory - initialMemory;
                double memoryIncreaseMB = memoryIncrease / 1024.0 / 1024.0;

                logger.info("Memory usage after batch {}: {} MB (increase: {:.2f} MB)",
                        batch + 1, currentMemory / 1024 / 1024, memoryIncreaseMB);

                // Memory should not grow uncontrollably
                assertThat(memoryIncreaseMB).isLessThan(500.0); // Less than 500MB increase per batch
            }

            // Then - Verify final memory usage is reasonable
            System.gc();
            Thread.sleep(2000);

            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long totalMemoryIncrease = finalMemory - initialMemory;
            double totalMemoryIncreaseMB = totalMemoryIncrease / 1024.0 / 1024.0;

            logger.info("Final memory usage: {} MB (total increase: {:.2f} MB)",
                    finalMemory / 1024 / 1024, totalMemoryIncreaseMB);

            // Verify database state
            long totalCustomers = customerRepository.count();
            assertThat(totalCustomers).isEqualTo(batchSize * numberOfBatches);

            // Memory increase should be reasonable for the amount of data processed
            assertThat(totalMemoryIncreaseMB).isLessThan(1000.0); // Less than 1GB total increase

        } finally {
            // Clean up
            System.gc();
        }
    }

    @Test
    @DisplayName("Should maintain response time consistency under varying load")
    void shouldMaintainResponseTimeConsistencyUnderVaryingLoad() throws Exception {
        // Given
        ExecutorService executor = Executors.newFixedThreadPool(30);
        List<Long> lowLoadResponseTimes = new ArrayList<>();
        List<Long> highLoadResponseTimes = new ArrayList<>();

        try {
            // Phase 1: Low load test
            logger.info("Starting low load phase...");
            for (int i = 0; i < 50; i++) {
                final int index = i;
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    Instant start = Instant.now();
                    try {
                        createTestCustomer(index);
                        long responseTime = Duration.between(start, Instant.now()).toMillis();
                        synchronized (lowLoadResponseTimes) {
                            lowLoadResponseTimes.add(responseTime);
                        }
                    } catch (Exception e) {
                        logger.warn("Error in low load test: {}", e.getMessage());
                    }
                }, executor);

                future.get(10, TimeUnit.SECONDS);
                Thread.sleep(200); // 200ms delay between requests (low load)
            }

            // Phase 2: High load test
            logger.info("Starting high load phase...");
            List<CompletableFuture<Void>> highLoadFutures = new ArrayList<>();

            for (int i = 50; i < 200; i++) {
                final int index = i;
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    Instant start = Instant.now();
                    try {
                        createTestCustomer(index);
                        long responseTime = Duration.between(start, Instant.now()).toMillis();
                        synchronized (highLoadResponseTimes) {
                            highLoadResponseTimes.add(responseTime);
                        }
                    } catch (Exception e) {
                        logger.warn("Error in high load test: {}", e.getMessage());
                    }
                }, executor);
                highLoadFutures.add(future);
            }

            CompletableFuture.allOf(highLoadFutures.toArray(new CompletableFuture[0])).get(60, TimeUnit.SECONDS);

            // Then - Analyze response time consistency
            double lowLoadAverage = lowLoadResponseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
            double highLoadAverage = highLoadResponseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);

            long lowLoadMax = lowLoadResponseTimes.stream().mapToLong(Long::longValue).max().orElse(0L);
            long highLoadMax = highLoadResponseTimes.stream().mapToLong(Long::longValue).max().orElse(0L);

            logger.info("Response Time Analysis:");
            logger.info("- Low load average: {:.2f} ms", lowLoadAverage);
            logger.info("- High load average: {:.2f} ms", highLoadAverage);
            logger.info("- Low load max: {} ms", lowLoadMax);
            logger.info("- High load max: {} ms", highLoadMax);
            logger.info("- Performance degradation: {:.2f}x", highLoadAverage / lowLoadAverage);

            // Performance consistency assertions
            assertThat(lowLoadResponseTimes).isNotEmpty();
            assertThat(highLoadResponseTimes).isNotEmpty();
            assertThat(lowLoadAverage).isLessThan(5000.0); // Low load should be fast
            assertThat(highLoadAverage).isLessThan(15000.0); // High load should still be reasonable
            assertThat(highLoadAverage / lowLoadAverage).isLessThan(10.0); // Performance shouldn't degrade by more than
                                                                           // 10x

        } finally {
            executor.shutdown();
            executor.awaitTermination(15, TimeUnit.SECONDS);
        }
    }

    private CustomerResponse createTestCustomer(int index) {
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Performance" + index);
        request.setLastName("Test" + index);
        request.setSecondLastName("Customer" + index);
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));

        return customerService.createCustomer(request);
    }
}
