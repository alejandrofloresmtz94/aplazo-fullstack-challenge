package com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.aplazo.challenge.aplazo_fullstack_challenge.enums.InstallmentStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Installment {

    private BigDecimal amount;

    private LocalDate scheduledPaymentDate;

    private InstallmentStatus status;

}
