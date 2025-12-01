package com.bank.models.shared.api;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ValidationErrorResponse extends ApiErrorResponse {

    private List<FieldValidationError> validationErrors;
}
