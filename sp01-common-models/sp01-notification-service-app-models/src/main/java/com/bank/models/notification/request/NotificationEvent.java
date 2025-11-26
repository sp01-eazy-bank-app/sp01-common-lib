package com.bank.models.notification.request;

import com.bank.models.notification.enums.EventType;
import com.bank.models.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record NotificationEvent(

        @NotBlank
        String recipient,

        Map<String, Object> contextData,

        @NotNull
        NotificationType notificationType,

        @NotNull
        EventType eventType
) {
}
