export interface CustomerRequest {
  firstName: string;
  lastName: string;
  secondLastName: string;
  dateOfBirth: string; // formato: YYYY-MM-DD
}

export interface CustomerResponse {
  id: string; // UUID
  createdAt: string; // ISO-8601 datetime
  creditLineAmount: number;
  availableCreditLineAmount: number;
}

export interface ErrorResponse {
  code: string; // formato: APZ000000
  error: string;
  timestamp: number;
  message: string;
  path: string;
}
