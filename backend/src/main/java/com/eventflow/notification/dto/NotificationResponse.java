package com.eventflow.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        long id,
        UUID eventId,
        long orderId,
        String type,
        String message,
        Instant createdAt
) {
}
