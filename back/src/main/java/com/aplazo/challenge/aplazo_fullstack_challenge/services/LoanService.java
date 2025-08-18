package com.aplazo.challenge.aplazo_fullstack_challenge.services;

import java.util.UUID;

import com.aplazo.challenge.aplazo_fullstack_challenge.entity.LoanEntity;

public interface LoanService {
    LoanEntity createLoan(LoanEntity loan);
    LoanEntity getLoanById(UUID id);
}
