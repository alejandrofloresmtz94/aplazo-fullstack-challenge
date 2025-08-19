package com.aplazo.challenge.aplazo_fullstack_challenge.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer.CustomerRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer.CustomerResponse;
import com.aplazo.challenge.aplazo_fullstack_challenge.security.JwtService;
import com.aplazo.challenge.aplazo_fullstack_challenge.services.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final JwtService jwtService;

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest customerRequest) {
        CustomerResponse customer = customerService.createCustomer(customerRequest);
        String token = jwtService.generateToken(customer.getId().toString(), 1800);
        URI location = URI.create("/v1/customers/" + customer.getId());
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);
        headers.add("X-Auth-Token", token);

        return new ResponseEntity<>(customer, headers, HttpStatus.CREATED);
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomerById(@Valid @PathVariable UUID customerId) {
        CustomerResponse customer = customerService.getCustomerById(customerId);
        return new ResponseEntity<>(customer, HttpStatus.OK);
    }

}
