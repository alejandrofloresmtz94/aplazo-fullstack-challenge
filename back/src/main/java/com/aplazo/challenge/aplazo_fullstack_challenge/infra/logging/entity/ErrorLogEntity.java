package com.aplazo.challenge.aplazo_fullstack_challenge.infra.logging.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "error_logs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ErrorLogEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String code; // patrón ^APZ[0-9]{6}$

    @Column(nullable = false, length = 100)
    private String error;

    @Column(nullable = false)
    private Long timestamp; // Unix timestamp

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column
    private String path;
}
