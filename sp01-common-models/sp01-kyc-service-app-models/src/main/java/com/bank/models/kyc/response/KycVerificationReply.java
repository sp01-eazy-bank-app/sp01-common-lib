package com.bank.models.kyc.response;

import jakarta.validation.constraints.NotBlank;

public record KycVerificationReply(

        @NotBlank
        String aadhaarNumber,

        boolean isValid
) {
}
