package com.aplazo.challenge.aplazo_fullstack_challenge.services;

import java.math.BigDecimal;
import java.util.List;

import com.aplazo.challenge.aplazo_fullstack_challenge.entity.InstallmentEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.LoanEntity;

public interface InstallmentService {

    List<InstallmentEntity> generateInstallments(LoanEntity loan, BigDecimal loanAmount, BigDecimal commissionAmount);

    List<InstallmentEntity> saveInstallments(List<InstallmentEntity> installments);
}
