package com.aplazo.challenge.aplazo_fullstack_challenge.services;

import java.util.UUID;

import com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer.CustomerRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer.CustomerResponse;

public interface CustomerService {
    CustomerResponse createCustomer(CustomerRequest customer);
    CustomerResponse getCustomerById(UUID id);
}