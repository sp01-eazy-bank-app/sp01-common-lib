package com.bank.email.response;

public record EmailResponse(
        String recipient,
        String status,
        String messageId
) {
}
