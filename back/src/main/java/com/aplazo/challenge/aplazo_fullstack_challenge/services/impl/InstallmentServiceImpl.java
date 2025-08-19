package com.aplazo.challenge.aplazo_fullstack_challenge.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aplazo.challenge.aplazo_fullstack_challenge.entity.InstallmentEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.LoanEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.enums.InstallmentStatus;
import com.aplazo.challenge.aplazo_fullstack_challenge.repository.InstallmentRepository;
import com.aplazo.challenge.aplazo_fullstack_challenge.services.InstallmentService;

@Service
@Transactional
public class InstallmentServiceImpl implements InstallmentService {

    private static final int NUMBER_OF_INSTALLMENTS = 5;
    private static final int PAYMENT_INTERVAL_WEEKS = 2;

    private final InstallmentRepository installmentRepository;

    public InstallmentServiceImpl(InstallmentRepository installmentRepository) {
        this.installmentRepository = installmentRepository;
    }

    @Override
    public List<InstallmentEntity> generateInstallments(LoanEntity loan, BigDecimal loanAmount,
            BigDecimal commissionAmount) {
        List<InstallmentEntity> installments = new ArrayList<>();

        // Calcular monto total (préstamo + comisión)
        BigDecimal totalAmount = loanAmount.add(commissionAmount);

        // Dividir en 5 cuotas iguales
        BigDecimal installmentAmount = totalAmount.divide(
                new BigDecimal(NUMBER_OF_INSTALLMENTS),
                2,
                RoundingMode.HALF_UP);

        LocalDate currentDate = LocalDate.now();

        // Generar las 5 cuotas
        for (int i = 0; i < NUMBER_OF_INSTALLMENTS; i++) {
            InstallmentEntity installment = InstallmentEntity.builder()
                    .loan(loan)
                    .amount(installmentAmount)
                    .scheduledPaymentDate(currentDate.plusWeeks((long) PAYMENT_INTERVAL_WEEKS * (i + 1)))
                    .status(i == 0 ? InstallmentStatus.NEXT : InstallmentStatus.PENDING)
                    .build();

            installments.add(installment);
        }

        return installments;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<InstallmentEntity> saveInstallments(List<InstallmentEntity> installments) {
        return installmentRepository.saveAll(installments);
    }

}
