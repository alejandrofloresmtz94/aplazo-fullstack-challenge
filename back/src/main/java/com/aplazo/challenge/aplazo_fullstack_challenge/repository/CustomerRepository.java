package com.aplazo.challenge.aplazo_fullstack_challenge.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aplazo.challenge.aplazo_fullstack_challenge.entity.CustomerEntity;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {
}
