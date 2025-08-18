package com.aplazo.challenge.aplazo_fullstack_challenge.services.impl;

import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.aplazo.challenge.aplazo_fullstack_challenge.entity.LoanEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.enums.LoanStatus;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.InstallmentRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.LoanRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.services.LoanService;

import jakarta.persistence.EntityNotFoundException;

public class LoanServiceImpl implements LoanService {
    
    private final LoanRepository loanRepository;
    private final InstallmentRepository installmentRepository;

    public LoanServiceImpl(LoanRepository loanRepository, InstallmentRepository installmentRepository) {
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoanEntity createLoan(LoanEntity loan) {
        // ToDO: agregar Logica de Negocio para creacion de Prestamos
        loan.setStatus(LoanStatus.ACTIVE);
        LoanEntity savedLoan = loanRepository.save(loan);
        
        if (loan.getInstallments() != null && !loan.getInstallments().isEmpty()) {
            loan.getInstallments().forEach(installment -> installment.setLoan(savedLoan));
            this.installmentRepository.saveAll(loan.getInstallments());
        }
        
        return savedLoan;
    }

    @Override
    @Transactional(readOnly = true)
    public LoanEntity getLoanById(UUID id) {
        return loanRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Préstamo no encontrado con ID: " + id));
    }

}
