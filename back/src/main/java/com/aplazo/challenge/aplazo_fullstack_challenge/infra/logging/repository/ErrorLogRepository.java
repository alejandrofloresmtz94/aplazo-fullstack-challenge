package com.aplazo.challenge.aplazo_fullstack_challenge.infra.logging.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aplazo.challenge.aplazo_fullstack_challenge.infra.logging.entity.ErrorLogEntity;

public interface ErrorLogRepository extends JpaRepository<ErrorLogEntity, UUID> {
        
}
