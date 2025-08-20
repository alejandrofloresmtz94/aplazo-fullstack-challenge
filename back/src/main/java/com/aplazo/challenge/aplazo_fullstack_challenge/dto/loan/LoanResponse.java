package com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.aplazo.challenge.aplazo_fullstack_challenge.enums.LoanStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LoanResponse {

    private UUID id;

    private UUID customerId;

    private BigDecimal amount;

    private LoanStatus status;

    private Instant createdAt;

    private PaymentPlan paymentPlan;

}