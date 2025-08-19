package com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentPlan {

    private BigDecimal commissionAmount;


    private List<Installment> installments;
}
