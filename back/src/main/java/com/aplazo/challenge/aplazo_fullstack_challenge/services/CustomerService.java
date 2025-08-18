package com.aplazo.challenge.aplazo_fullstack_challenge.services;

import java.util.UUID;

import com.aplazo.challenge.aplazo_fullstack_challenge.entity.CustomerEntity;

public interface CustomerService {
    CustomerEntity createCustomer(CustomerEntity customer);
    CustomerEntity getCustomerById(UUID id);
}