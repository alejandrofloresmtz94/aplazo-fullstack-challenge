package com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CustomerResponse {

    private UUID id;

    private Instant createdAt;

    private BigDecimal creditLineAmount;

    private BigDecimal availableCreditLineAmount;

}
