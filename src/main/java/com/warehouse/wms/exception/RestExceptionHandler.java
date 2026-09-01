package com.warehouse.wms.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice(basePackages = "com.warehouse.wms.api")
public class RestExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException ex) { return error(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of()); }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ApiError> conflict(BusinessRuleException ex) { return error(HttpStatus.CONFLICT, ex.getMessage(), Map.of()); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, e -> e.getDefaultMessage() == null ? "Invalid value" : e.getDefaultMessage(), (a, b) -> a));
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(DataIntegrityViolationException ex) { return error(HttpStatus.CONFLICT, "The resource violates a uniqueness or reference constraint", Map.of()); }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> optimisticConflict(OptimisticLockingFailureException ex) { return error(HttpStatus.CONFLICT, "The resource was changed by another operation; reload and retry", Map.of()); }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, Map<String, String> fieldErrors) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, fieldErrors));
    }
}
