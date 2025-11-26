package com.bank.models.account.request;

import com.bank.models.account.enums.AccountType;
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
