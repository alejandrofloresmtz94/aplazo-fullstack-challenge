package com.aplazo.challenge.aplazo_fullstack_challenge.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aplazo.challenge.aplazo_fullstack_challenge.entity.CustomerEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.LoanEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.enums.LoanStatus;

public interface LoanRepository extends JpaRepository<LoanEntity, UUID> {
    List<LoanEntity> findByCustomer(CustomerEntity customer);
    List<LoanEntity> findByStatus(LoanStatus status);
}
