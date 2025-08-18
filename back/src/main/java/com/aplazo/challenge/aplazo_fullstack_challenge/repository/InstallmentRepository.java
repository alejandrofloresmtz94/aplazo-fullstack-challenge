package com.aplazo.challenge.aplazo_fullstack_challenge.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aplazo.challenge.aplazo_fullstack_challenge.entity.InstallmentEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.LoanEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.enums.InstallmentStatus;

public interface InstallmentRepository extends JpaRepository<InstallmentEntity, UUID> {
    List<InstallmentEntity> findByLoan(LoanEntity loan);
    List<InstallmentEntity> findByStatus(InstallmentStatus status);
}
