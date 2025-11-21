package com.bank.common.response;

import jakarta.validation.constraints.NotBlank;

public record AccountReply(
        @NotBlank
        String customerId
) {
}
