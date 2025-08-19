package com.aplazo.challenge.aplazo_fullstack_challenge.services;

import java.util.UUID;

import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.LoanRequest;
import com.aplazo.challenge.aplazo_fullstack_challenge.dto.loan.LoanResponse;

public interface LoanService {
    LoanResponse createLoan(LoanRequest loanRequest);
    LoanResponse getLoanById(UUID id);
}
