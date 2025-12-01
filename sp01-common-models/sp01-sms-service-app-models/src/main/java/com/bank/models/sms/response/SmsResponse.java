package com.bank.models.sms.response;

public record SmsResponse(String recipient, String status, String messageId) {
}
