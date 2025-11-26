package com.bank.models.shared.api;

public record FieldValidationError(
        String field,
        String message
) {
}
