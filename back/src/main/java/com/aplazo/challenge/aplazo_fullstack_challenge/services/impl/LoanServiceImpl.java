package com.aplazo.challenge.aplazo_fullstack_challenge.services.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.LoanRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.LoanResponse;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.CustomerEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.InstallmentEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.LoanEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.enums.LoanStatus;
import com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception.CustomerNotFoundException;
import com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception.LoanNotFoundException;
import com.aplazo.challenge.aplazo_fullstack_challenge.mapper.LoanMapper;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.CustomerRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.LoanRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.services.InstallmentService;
import com.aplazo.challenge.aplazo_fullstack_challenge.services.LoanService;

@Service
@Transactional
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final InstallmentService installmentService;
    private final CustomerRepository customerRepository;
    private final LoanMapper loanMapper;

    public LoanServiceImpl(LoanRepository loanRepository, InstallmentService installmentService,
                          CustomerRepository customerRepository, LoanMapper loanMapper) {
        this.loanRepository = loanRepository;
        this.installmentService = installmentService;
        this.customerRepository = customerRepository;
        this.loanMapper = loanMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoanResponse createLoan(LoanRequest loanRequest) {
        // Verificar que el customer existe
        CustomerEntity customer = customerRepository.findById(loanRequest.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
        
        // Convertir DTO a Entity
        LoanEntity loanEntity = loanMapper.toEntity(loanRequest);
        loanEntity.setCustomer(customer);
        loanEntity.setStatus(LoanStatus.ACTIVE);
        
        // Calcular comisión (5% del monto)
        BigDecimal commission = loanRequest.getAmount().multiply(new BigDecimal("0.05"));
        loanEntity.setCommissionAmount(commission);
        
        // Guardar el loan
        LoanEntity savedLoan = loanRepository.save(loanEntity);
        
        // Generar y guardar las 5 cuotas
        List<InstallmentEntity> installments = installmentService.generateInstallments(savedLoan, loanRequest.getAmount(), commission);
        installments = installmentService.saveInstallments(installments);
        
        // Cargar el loan con installments para la respuesta
        savedLoan.setInstallments(installments);
        
        return loanMapper.toResponse(savedLoan);
    }
    

    @Override
    @Transactional(readOnly = true)
    public LoanResponse getLoanById(UUID id) {
        LoanEntity loan = loanRepository.findById(id)
                .orElseThrow(() -> new LoanNotFoundException("Préstamo no encontrado con ID: " + id));
        return loanMapper.toResponse(loan);
    }

}
