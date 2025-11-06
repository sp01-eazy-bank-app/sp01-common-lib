package com.bank.domain.sms.response;

public record SmsResponse(String recipient, String status, String messageId) {
}
