package com.bank.common.request;

import com.bank.common.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateAccountRequest(

        @NotBlank
        String email,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @NotBlank
        String phoneNumber,

        @NotBlank
        String address,

        @NotBlank
        String country,

        @NotNull
        LocalDate dateOfBirth,

        @NotNull
        AccountType accountType
) {
}
