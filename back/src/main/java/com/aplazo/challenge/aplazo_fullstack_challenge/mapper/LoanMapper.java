package com.aplazo.challenge.aplazo_fullstack_challenge.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.Installment;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.LoanRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.LoanResponse;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.PaymentPlan;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.InstallmentEntity;
import com.aplazo.challenge.aplazo_fullstack_challenge.entity.LoanEntity;

@Mapper(componentModel = "spring")
public interface LoanMapper {

    // Request → Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true) // Se asigna en el servicio
    @Mapping(target = "status", ignore = true) // Se asigna en el servicio
    @Mapping(target = "commissionAmount", ignore = true) // Se calcula en el servicio
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "installments", ignore = true) // Se generan en el servicio
    LoanEntity toEntity(LoanRequest request);

    // Entity → Response
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "paymentPlan", source = "installments", qualifiedByName = "mapPaymentPlan")
    LoanResponse toResponse(LoanEntity entity);

    @Named("mapPaymentPlan")
    default PaymentPlan mapPaymentPlan(List<InstallmentEntity> installments) {
        if (installments == null || installments.isEmpty()) {
            return null;
        }

        BigDecimal commissionAmount = installments.get(0).getLoan().getCommissionAmount();
        List<Installment> installmentDtos = installments.stream()
                .map(this::mapInstallment)
                .toList();

        return PaymentPlan.builder()
                .commissionAmount(commissionAmount)
                .installments(installmentDtos)
                .build();
    }

    default Installment mapInstallment(InstallmentEntity entity) {
        return Installment.builder()
                .amount(entity.getAmount())
                .scheduledPaymentDate(entity.getScheduledPaymentDate())
                .status(entity.getStatus())
                .build();
    }

}