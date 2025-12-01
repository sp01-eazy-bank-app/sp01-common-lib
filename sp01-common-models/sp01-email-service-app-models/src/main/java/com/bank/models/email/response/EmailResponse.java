package com.bank.models.email.response;

public record EmailResponse(
        String recipient,
        String status,
        String messageId
) {
}
