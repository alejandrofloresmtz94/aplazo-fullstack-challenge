export interface LoanRequest {
  customerId: string; // UUID
  amount: number; // > 0
}

export interface LoanResponse {
  id: string; // UUID
  customerId: string; // UUID
  status?: 'ACTIVE' | 'LATE' | 'COMPLETED';
  createdAt: string; // ISO-8601 datetime
  paymentPlan?: PaymentPlan;
}

export interface PaymentPlan {
  commissionAmount: number;
  installments: InstallmentResponse[];
}

export interface InstallmentResponse {
  amount: number;
  scheduledPaymentDate: string; // formato: YYYY-MM-DD
  status: 'NEXT' | 'PENDING' | 'ERROR';
}
