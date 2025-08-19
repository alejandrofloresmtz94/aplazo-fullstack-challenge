package com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoanRequest {
    
    @NotNull(message = "El customer ID es obligatorio")
    private UUID customerId;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;

}
