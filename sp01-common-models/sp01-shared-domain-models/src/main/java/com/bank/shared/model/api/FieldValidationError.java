package com.bank.shared.model.api;

public record FieldValidationError(
        String field,
        String message
) {
}
