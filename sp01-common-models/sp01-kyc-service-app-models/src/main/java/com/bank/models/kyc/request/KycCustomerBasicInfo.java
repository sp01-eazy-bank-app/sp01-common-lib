package com.bank.models.kyc.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record KycCustomerBasicInfo(

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @NotBlank
        String emailId,

        @NotBlank
        String phoneNumber,

        @NotBlank
        String address,

        @NotBlank
        String country,

        @NotNull
        LocalDate dateOfBirth
) {
}
