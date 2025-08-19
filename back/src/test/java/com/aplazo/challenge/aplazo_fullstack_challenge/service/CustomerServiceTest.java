package com.aplazo.challenge.aplazo_fullstack_challenge.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer.CustomerRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer.CustomerResponse;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.CustomerEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception.CustomerNotFoundException;
import com.aplazo.challenge.aplazo_fullstack_challenge.mapper.CustomerMapper;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.CustomerRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.services.impl.CustomerServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("Customer Service Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private CustomerRequest customerRequest;
    private CustomerEntity customerEntity;
    private CustomerResponse customerResponse;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();

        customerRequest = new CustomerRequest();
        customerRequest.setFirstName("Juan");
        customerRequest.setLastName("Pérez");
        customerRequest.setSecondLastName("López");
        customerRequest.setDateOfBirth(LocalDate.of(1990, 5, 15));

        customerEntity = CustomerEntity.builder()
                .id(customerId)
                .firstName("Juan")
                .lastName("Pérez")
                .secondLastName("López")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .creditLineAmount(new BigDecimal("1.00"))
                .availableCreditLineAmount(new BigDecimal("0.00"))
                .createdAt(Instant.now())
                .build();

        customerResponse = CustomerResponse.builder()
                .id(customerId)
                .creditLineAmount(new BigDecimal("1.00"))
                .availableCreditLineAmount(new BigDecimal("0.00"))
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should create customer successfully")
    void shouldCreateCustomerSuccessfully() {
        // Given
        when(customerMapper.toEntity(customerRequest)).thenReturn(customerEntity);
        when(customerRepository.saveAndFlush(any(CustomerEntity.class))).thenReturn(customerEntity);
        when(customerMapper.toResponse(customerEntity)).thenReturn(customerResponse);

        // When
        CustomerResponse result = customerService.createCustomer(customerRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(customerId);
        assertThat(result.getCreditLineAmount()).isEqualTo(new BigDecimal("1.00"));
        assertThat(result.getAvailableCreditLineAmount()).isEqualTo(new BigDecimal("0.00"));

        verify(customerMapper).toEntity(customerRequest);
        verify(customerRepository).saveAndFlush(any(CustomerEntity.class));
        verify(customerMapper).toResponse(customerEntity);
    }

    @Test
    @DisplayName("Should get customer by id successfully")
    void shouldGetCustomerByIdSuccessfully() {
        // Given
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customerEntity));
        when(customerMapper.toResponse(customerEntity)).thenReturn(customerResponse);

        // When
        CustomerResponse result = customerService.getCustomerById(customerId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(customerId);

        verify(customerRepository).findById(customerId);
        verify(customerMapper).toResponse(customerEntity);
    }

    @Test
    @DisplayName("Should throw CustomerNotFoundException when customer not found")
    void shouldThrowCustomerNotFoundExceptionWhenCustomerNotFound() {
        // Given
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customerService.getCustomerById(customerId))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer not found");

        verify(customerRepository).findById(customerId);
    }
}
