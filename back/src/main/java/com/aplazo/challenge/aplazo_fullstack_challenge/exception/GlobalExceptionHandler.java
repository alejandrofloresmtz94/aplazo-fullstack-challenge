package com.aplazo.challenge.aplazo_fullstack_challenge.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.aplazo.challenge.aplazo_fullstack_challenge.dto.ErrorResponse;
import com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception.CustomerNotFoundException;
import com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception.LoanNotFoundException;
import com.aplazo.challenge.aplazo_fullstack_challenge.exception.custom_exception.RateLimitExceededException;
import com.aplazo.challenge.aplazo_fullstack_challenge.infra.logging.service.ErrorLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
        private final ErrorLogService errorLogService;
        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler({ AccessDeniedException.class })
        public ResponseEntity<ErrorResponse> handleUnauthorized(
                        RuntimeException ex, HttpServletRequest request) {
                String path = request.getRequestURI();
                logger.warn("GlobalExceptionHandler - Unauthorized access: {}", ex.getMessage());

                errorLogService.logError("APZ000007", "UNAUTHORIZED", ex.getMessage(), path);

                ErrorResponse response = new ErrorResponse(
                                "APZ000007",
                                "UNAUTHORIZED",
                                Instant.now().getEpochSecond(),
                                ex.getMessage(),
                                path);

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        @ExceptionHandler(RateLimitExceededException.class)
        public ResponseEntity<ErrorResponse> handleRateLimit(
                        RateLimitExceededException ex, HttpServletRequest request) {
                String path = request.getRequestURI();
                logger.warn("Rate limit exceeded: {}", ex.getMessage());

                errorLogService.logError("APZ000003", "RATE_LIMIT_ERROR", ex.getMessage(), path);

                ErrorResponse response = new ErrorResponse(
                                "APZ000003",
                                "RATE_LIMIT_ERROR",
                                Instant.now().getEpochSecond(),
                                ex.getMessage(),
                                path);

                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        }

        @ExceptionHandler(CustomerNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleCustomerNotFound(
                        CustomerNotFoundException ex, HttpServletRequest request) {

                String path = request.getRequestURI();
                logger.error("Customer not found: {}", ex.getMessage());

                errorLogService.logError("APZ000005", "CUSTOMER_NOT_FOUND", ex.getMessage(), path);

                ErrorResponse response = new ErrorResponse(
                                "APZ000005",
                                "CUSTOMER_NOT_FOUND",
                                Instant.now().getEpochSecond(),
                                ex.getMessage(),
                                path);

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        @ExceptionHandler(LoanNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleLoanNotFound(
                        LoanNotFoundException ex, HttpServletRequest request) {

                String path = request.getRequestURI();
                logger.error("Loan not found: {}", ex.getMessage());

                errorLogService.logError("APZ000008", "LOAN_NOT_FOUND", ex.getMessage(), path);

                ErrorResponse response = new ErrorResponse(
                                "APZ000008",
                                "LOAN_NOT_FOUND",
                                Instant.now().getEpochSecond(),
                                ex.getMessage(),
                                path);

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // Valida errores de @Valid en @RequestBody
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationExceptions(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                String path = request.getRequestURI();
                logger.error("Invalid request: {}", ex.getMessage());

                String errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                                .collect(Collectors.joining("; "));

                errorLogService.logError("APZ000004", "INVALID_REQUEST", errors, path);

                ErrorResponse response = new ErrorResponse(
                                "APZ000004",
                                "INVALID_REQUEST",
                                Instant.now().getEpochSecond(),
                                errors,
                                path);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Valida errores de ConstraintValidator (ej. @Adult)
        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ErrorResponse> handleConstraintViolation(
                        ConstraintViolationException ex,
                        HttpServletRequest request) {

                String path = request.getRequestURI();
                logger.error("Constraint violation: {}", ex.getMessage());

                String errors = ex.getConstraintViolations()
                                .stream()
                                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                                .collect(Collectors.joining("; "));

                errorLogService.logError("APZ000004", "INVALID_REQUEST", errors, path);

                ErrorResponse response = new ErrorResponse("APZ000004", "INVALID_REQUEST",
                                Instant.now().getEpochSecond(), errors, path);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Error general del servidor
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleAllExceptions(
                        Exception ex,
                        HttpServletRequest request) {

                String path = request.getRequestURI();
                logger.error("Constraint violation: {}", ex.getMessage());

                errorLogService.logError("APZ000001", "INTERNAL_SERVER_ERROR", ex.getMessage(), path);

                ErrorResponse response = new ErrorResponse("APZ000001", "INTERNAL_SERVER_ERROR",
                                Instant.now().getEpochSecond(), ex.getMessage(), path);

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

}
