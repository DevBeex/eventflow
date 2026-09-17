package com.eventflow.notification.service;

import com.eventflow.notification.dto.NotificationResponse;
import com.eventflow.notification.entity.Notification;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.id,
                notification.eventId,
                notification.orderId,
                notification.type,
                notification.message,
                notification.createdAt
        );
    }
}
