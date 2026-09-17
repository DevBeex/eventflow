package com.eventflow.notification.service;

import com.eventflow.common.BusinessException;
import com.eventflow.common.ErrorCode;
import com.eventflow.common.PageResponse;
import com.eventflow.common.PaginationParams;
import com.eventflow.notification.dto.NotificationResponse;
import com.eventflow.notification.entity.Notification;
import com.eventflow.notification.repository.NotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class NotificationService {

    @Inject
    NotificationRepository notificationRepository;

    public PageResponse<NotificationResponse> list(PaginationParams pagination, Long orderId) {
        long total = notificationRepository.countFiltered(orderId);
        List<NotificationResponse> items = notificationRepository.findPage(pagination.page(), pagination.size(), orderId)
                .stream()
                .map(NotificationMapper::toResponse)
                .toList();
        return PageResponse.of(items, pagination.page(), pagination.size(), total);
    }

    public NotificationResponse getById(long id) {
        Notification notification = notificationRepository.findById(id);
        if (notification == null) {
            throw new BusinessException(404, ErrorCode.NOTIFICATION_NOT_FOUND, "Notification not found");
        }
        return NotificationMapper.toResponse(notification);
    }
}
