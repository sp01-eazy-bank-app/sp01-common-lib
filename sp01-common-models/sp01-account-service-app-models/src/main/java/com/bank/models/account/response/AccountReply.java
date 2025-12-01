package com.bank.models.account.response;

import jakarta.validation.constraints.NotBlank;

public record AccountReply(
        @NotBlank
        String customerId
) {
}
