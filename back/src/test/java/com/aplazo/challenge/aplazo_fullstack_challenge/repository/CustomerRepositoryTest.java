package com.aplazo.challenge.aplazo_fullstack_challenge.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.aplazo.challenge.aplazo_fullstack_challenge.entity.CustomerEntity;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Customer Repository Tests")
class CustomerRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("challenge_test_db")
            .withUsername("test_user")
            .withPassword("test_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private CustomerRepository customerRepository;

    private CustomerEntity testCustomer;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();

        testCustomer = CustomerEntity.builder()
                .firstName("Juan")
                .lastName("Pérez")
                .secondLastName("López")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .creditLineAmount(new BigDecimal("1.00"))
                .availableCreditLineAmount(new BigDecimal("0.00"))
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should save and find customer by id")
    void shouldSaveAndFindCustomerById() {
        // When
        CustomerEntity savedCustomer = customerRepository.save(testCustomer);
        Optional<CustomerEntity> foundCustomer = customerRepository.findById(savedCustomer.getId());

        // Then
        assertThat(savedCustomer.getId()).isNotNull();
        assertThat(foundCustomer).isPresent();
        assertThat(foundCustomer.get().getFirstName()).isEqualTo("Juan");
        assertThat(foundCustomer.get().getLastName()).isEqualTo("Pérez");
        assertThat(foundCustomer.get().getSecondLastName()).isEqualTo("López");
        assertThat(foundCustomer.get().getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(foundCustomer.get().getCreditLineAmount()).isEqualTo(new BigDecimal("1.00"));
        assertThat(foundCustomer.get().getAvailableCreditLineAmount()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("Should return empty when customer not found")
    void shouldReturnEmptyWhenCustomerNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When
        Optional<CustomerEntity> foundCustomer = customerRepository.findById(nonExistentId);

        // Then
        assertThat(foundCustomer).isEmpty();
    }

    @Test
    @DisplayName("Should find all customers")
    void shouldFindAllCustomers() {
        // Given
        customerRepository.save(testCustomer);

        CustomerEntity customer2 = CustomerEntity.builder()
                .firstName("María")
                .lastName("García")
                .secondLastName("Fernández")
                .dateOfBirth(LocalDate.of(1985, 8, 20))
                .creditLineAmount(new BigDecimal("1.00"))
                .availableCreditLineAmount(new BigDecimal("0.00"))
                .createdAt(Instant.now())
                .build();
        customerRepository.save(customer2);

        // When
        List<CustomerEntity> allCustomers = customerRepository.findAll();

        // Then
        assertThat(allCustomers).hasSize(2);
        assertThat(allCustomers).extracting(CustomerEntity::getFirstName)
                .containsExactlyInAnyOrder("Juan", "María");
    }

    @Test
    @DisplayName("Should delete customer by id")
    void shouldDeleteCustomerById() {
        // Given
        CustomerEntity savedCustomer = customerRepository.save(testCustomer);
        UUID customerId = savedCustomer.getId();

        // When
        customerRepository.deleteById(customerId);
        Optional<CustomerEntity> foundCustomer = customerRepository.findById(customerId);

        // Then
        assertThat(foundCustomer).isEmpty();
    }

    @Test
    @DisplayName("Should update customer information")
    void shouldUpdateCustomerInformation() {
        // Given
        CustomerEntity savedCustomer = customerRepository.save(testCustomer);

        // When
        savedCustomer.setCreditLineAmount(new BigDecimal("1500.00"));
        savedCustomer.setAvailableCreditLineAmount(new BigDecimal("1500.00"));
        CustomerEntity updatedCustomer = customerRepository.save(savedCustomer);

        // Then
        assertThat(updatedCustomer.getCreditLineAmount()).isEqualTo(new BigDecimal("1500.00"));
        assertThat(updatedCustomer.getAvailableCreditLineAmount()).isEqualTo(new BigDecimal("1500.00"));
    }

    @Test
    @DisplayName("Should generate UUIDv7 for new customers")
    void shouldGenerateUuidV7ForNewCustomers() {
        // When
        CustomerEntity savedCustomer = customerRepository.save(testCustomer);

        // Then
        assertThat(savedCustomer.getId()).isNotNull();
        assertThat(savedCustomer.getId().toString()
                .matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-7[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"))
                .isTrue(); // UUIDv7
    }

    @Test
    @DisplayName("Should enforce not null constraints")
    void shouldEnforceNotNullConstraints() {
        // Given
        CustomerEntity invalidCustomer = CustomerEntity.builder()
                .firstName(null) // This should violate not null constraint
                .lastName("Pérez")
                .secondLastName("López")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .creditLineAmount(new BigDecimal("1.00"))
                .availableCreditLineAmount(new BigDecimal("0.00"))
                .createdAt(Instant.now())
                .build();

        // When & Then
        assertThrows(DataIntegrityViolationException.class, () -> {
            customerRepository.saveAndFlush(invalidCustomer);
        });
    }

    @Test
    @DisplayName("Should persist and retrieve date of birth correctly")
    void shouldPersistAndRetrieveDateOfBirthCorrectly() {
        // Given
        LocalDate birthDate = LocalDate.of(1992, 12, 25);
        testCustomer.setDateOfBirth(birthDate);

        // When
        CustomerEntity savedCustomer = customerRepository.save(testCustomer);
        Optional<CustomerEntity> foundCustomer = customerRepository.findById(savedCustomer.getId());

        // Then
        assertThat(foundCustomer).isPresent();
        assertThat(foundCustomer.get().getDateOfBirth()).isEqualTo(birthDate);
    }

    @Test
    @DisplayName("Should handle BigDecimal precision correctly")
    void shouldHandleBigDecimalPrecisionCorrectly() {
        // Given
        BigDecimal preciseCreditLine = new BigDecimal("1234.567890");
        BigDecimal preciseAvailableCredit = new BigDecimal("999.123456");
        testCustomer.setCreditLineAmount(preciseCreditLine);
        testCustomer.setAvailableCreditLineAmount(preciseAvailableCredit);

        // When
        CustomerEntity savedCustomer = customerRepository.save(testCustomer);
        Optional<CustomerEntity> foundCustomer = customerRepository.findById(savedCustomer.getId());

        // Then
        assertThat(foundCustomer).isPresent();
        // Note: Depending on database configuration, precision might be rounded
        assertThat(foundCustomer.get().getCreditLineAmount().compareTo(preciseCreditLine)).isZero();
        assertThat(foundCustomer.get().getAvailableCreditLineAmount().compareTo(preciseAvailableCredit)).isZero();
    }

    @Test
    @DisplayName("Should count customers correctly")
    void shouldCountCustomersCorrectly() {
        // Given
        assertThat(customerRepository.count()).isZero();

        customerRepository.save(testCustomer);
        CustomerEntity anotherCustomer = CustomerEntity.builder()
                .firstName("Ana")
                .lastName("Martínez")
                .secondLastName("Ruiz")
                .dateOfBirth(LocalDate.of(1995, 3, 10))
                .creditLineAmount(new BigDecimal("1.00"))
                .availableCreditLineAmount(new BigDecimal("0.00"))
                .createdAt(Instant.now())
                .build();
        customerRepository.save(anotherCustomer);

        // When & Then
        assertThat(customerRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should check customer existence correctly")
    void shouldCheckCustomerExistenceCorrectly() {
        // Given
        CustomerEntity savedCustomer = customerRepository.save(testCustomer);
        UUID existingId = savedCustomer.getId();
        UUID nonExistingId = UUID.randomUUID();

        // When & Then
        assertThat(customerRepository.existsById(existingId)).isTrue();
        assertThat(customerRepository.existsById(nonExistingId)).isFalse();
    }
}
