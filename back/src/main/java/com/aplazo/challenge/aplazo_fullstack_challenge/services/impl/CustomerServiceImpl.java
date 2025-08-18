package com.aplazo.challenge.aplazo_fullstack_challenge.services.impl;

import java.util.UUID;

import com.aplazo.challenge.aplazo_fullstack_challenge.entity.CustomerEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.CustomerRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.services.CustomerService;

public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public CustomerEntity createCustomer(CustomerEntity customer) {
        return customerRepository.save(customer);
    }

    @Override
    public CustomerEntity getCustomerById(UUID id) {
        return customerRepository.findById(id).orElse(null);
    }

}
