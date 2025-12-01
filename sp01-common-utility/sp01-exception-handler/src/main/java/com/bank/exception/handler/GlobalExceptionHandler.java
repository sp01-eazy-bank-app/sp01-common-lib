package com.bank.exception.handler;

import com.bank.exception.model.BusinessException;
import com.bank.exception.model.TechnicalException;
import com.bank.models.shared.api.ApiErrorResponse;
import com.bank.models.shared.api.FieldValidationError;
import com.bank.models.shared.api.ValidationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // handles business exception thrown explicitly in the code
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                e.getMessage(),
                request.getRequestURI(),
                e.getError().toString()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    // handles constraint validation exception that raises from the request body
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e,
                                                                              HttpServletRequest request) {
        List<FieldValidationError> validationErrors = e.getBindingResult().getFieldErrors()
                .stream().map(err -> new FieldValidationError(err.getField(), err.getDefaultMessage()))
                .toList();
        ValidationErrorResponse errorResponse = ValidationErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation Failed")
                .path(request.getRequestURI())
                .code("VALIDATION_ERROR")
                .validationErrors(validationErrors)
                .build();
        return ResponseEntity.badRequest().body(errorResponse);
    }

    // handles missing path variable
    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ValidationErrorResponse> handleMissingPathVariable(MissingPathVariableException e, HttpServletRequest request) {
        FieldValidationError error = new FieldValidationError(
                e.getVariableName(),
                "Required path variable '" + e.getVariableName() + "' is missing"
        );

        ValidationErrorResponse response = ValidationErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(request.getRequestURI())
                .code("VALIDATION_ERROR")
                .validationErrors(List.of(error))
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    // only to handle the missing amount (Double) value as part of the request param
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ValidationErrorResponse> handleMissingRequestParam(MissingServletRequestParameterException e,
                                                                             HttpServletRequest request) {
        FieldValidationError error = new FieldValidationError(
                e.getParameterName(),
                "Required request parameter '" + e.getParameterName() + "' is missing"
        );

        ValidationErrorResponse response = ValidationErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(request.getRequestURI())
                .code("VALIDATION_ERROR")
                .validationErrors(List.of(error))
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    // handles all constraint violation - request param (except the 'amount' (Double)) and path variable (except missing path variable)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        List<FieldValidationError> fieldErrors = e.getConstraintViolations()
                .stream()
                .map(violation -> new FieldValidationError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                )).toList();

        ValidationErrorResponse response = ValidationErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(request.getRequestURI())
                .code("VALIDATION_ERROR")
                .validationErrors(fieldErrors)
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    // handle technical exception
    @ExceptionHandler(TechnicalException.class)
    public ResponseEntity<ApiErrorResponse> handleTechnicalException(TechnicalException e, HttpServletRequest request) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                e.getMessage(),
                request.getRequestURI(),
                e.getError().toString()
        );
        return ResponseEntity.internalServerError().body(errorResponse);
    }

    // handles any remaining exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception e, HttpServletRequest request) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                e.getMessage(),
                request.getRequestURI(),
                "GENERIC_ERROR"
        );
        return ResponseEntity.internalServerError().body(errorResponse);
    }
}
