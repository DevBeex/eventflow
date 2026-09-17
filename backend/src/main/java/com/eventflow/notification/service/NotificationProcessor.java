package com.eventflow.notification.service;

import com.eventflow.common.ClockService;
import com.eventflow.event.EventPayloadValidator;
import com.eventflow.event.OrderCreatedEvent;
import com.eventflow.notification.consumer.OrderCreatedConsumer.InvalidMessageException;
import com.eventflow.notification.entity.Notification;
import com.eventflow.notification.repository.NotificationRepository;
import com.eventflow.order.entity.Order;
import com.eventflow.order.repository.OrderRepository;
import com.eventflow.outbox.repository.OutboxRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.Optional;

@ApplicationScoped
public class NotificationProcessor {

    private static final Logger LOG = Logger.getLogger(NotificationProcessor.class);
    private static final String NOTIFICATION_TYPE = OrderCreatedEvent.TYPE;

    @Inject
    EventPayloadValidator eventPayloadValidator;

    @Inject
    OutboxRepository outboxRepository;

    @Inject
    OrderRepository orderRepository;

    @Inject
    NotificationRepository notificationRepository;

    @Inject
    ClockService clockService;

    @Transactional
    public void process(String payload) {
        OrderCreatedEvent event;
        try {
            event = eventPayloadValidator.parseAndValidate(payload);
        } catch (EventPayloadValidator.InvalidEventException ex) {
            throw new InvalidMessageException(ex.getMessage(), "unknown");
        }

        var outbox = outboxRepository.findByEventId(event.eventId()).orElse(null);
        Order order = orderRepository.findById(event.orderId());
        try {
            eventPayloadValidator.validateAgainstOutbox(event, outbox, order);
        } catch (EventPayloadValidator.InvalidEventException ex) {
            throw new InvalidMessageException(ex.getMessage(), event.eventId().toString());
        }

        Optional<Notification> existingByEvent = notificationRepository.findByEventId(event.eventId());
        if (existingByEvent.isPresent()) {
            Notification existing = existingByEvent.get();
            if (existing.orderId == event.orderId() && NOTIFICATION_TYPE.equals(existing.type)) {
                LOG.infof("Duplicate notification ignored for eventId=%s orderId=%d", event.eventId(), event.orderId());
                return;
            }
            throw new InvalidMessageException("Conflicting notification for eventId", event.eventId().toString());
        }

        String messageText = "Order " + event.orderId() + " was created successfully";
        var now = clockService.now();
        Optional<Long> insertedId = notificationRepository.insertIfAbsent(
                event.eventId(), event.orderId(), NOTIFICATION_TYPE, messageText, now);

        if (insertedId.isPresent()) {
            LOG.infof("Notification created for eventId=%s orderId=%d", event.eventId(), event.orderId());
            return;
        }

        Notification existing = notificationRepository.findByEventId(event.eventId()).orElse(null);
        if (existing != null && existing.orderId == event.orderId() && NOTIFICATION_TYPE.equals(existing.type)) {
            LOG.infof("Duplicate notification ignored for eventId=%s orderId=%d", event.eventId(), event.orderId());
            return;
        }

        Optional<Notification> byOrderType = notificationRepository.findByOrderIdAndType(event.orderId(), NOTIFICATION_TYPE);
        if (byOrderType.isPresent() && !byOrderType.get().eventId.equals(event.eventId())) {
            throw new InvalidMessageException("Conflicting notification for order", event.eventId().toString());
        }
    }
}
