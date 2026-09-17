package com.eventflow.event;

import com.eventflow.common.MoneyUtils;
import com.eventflow.order.entity.Order;
import com.eventflow.outbox.entity.OutboxEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class EventPayloadValidator {

    @Inject
    ObjectMapper objectMapper;

    public OrderCreatedEvent parseAndValidate(String json) {
        if (json == null || json.isBlank()) {
            throw new InvalidEventException("Empty payload");
        }
        try {
            OrderCreatedEvent event = objectMapper.readValue(json, OrderCreatedEvent.class);
            validateEvent(event);
            return event;
        } catch (JsonProcessingException ex) {
            throw new InvalidEventException("Invalid JSON payload", ex);
        }
    }

    public void validateEvent(OrderCreatedEvent event) {
        if (event.eventId() == null) {
            throw new InvalidEventException("eventId is required");
        }
        if (!OrderCreatedEvent.TYPE.equals(event.eventType())) {
            throw new InvalidEventException("Unsupported eventType");
        }
        if (event.schemaVersion() != OrderCreatedEvent.SCHEMA_VERSION) {
            throw new InvalidEventException("Unsupported schemaVersion");
        }
        if (event.orderId() <= 0 || event.customerId() <= 0) {
            throw new InvalidEventException("Invalid orderId or customerId");
        }
        if (!MoneyUtils.CURRENCY.equals(event.currency())) {
            throw new InvalidEventException("Invalid currency");
        }
        if (event.total() == null || event.occurredAt() == null) {
            throw new InvalidEventException("Missing required fields");
        }
        try {
            event.total().setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new InvalidEventException("Invalid total scale");
        }
    }

    public void validateAgainstOutbox(OrderCreatedEvent event, OutboxEvent outbox, Order order) {
        if (outbox == null) {
            throw new InvalidEventException("Unknown eventId");
        }
        if (!Objects.equals(outbox.eventId, event.eventId())) {
            throw new InvalidEventException("eventId mismatch");
        }
        if (outbox.orderId != event.orderId()) {
            throw new InvalidEventException("orderId mismatch with outbox");
        }
        if (!OrderCreatedEvent.TYPE.equals(outbox.eventType)) {
            throw new InvalidEventException("eventType mismatch with outbox");
        }

        OrderCreatedEvent stored;
        try {
            stored = objectMapper.readValue(outbox.payload, OrderCreatedEvent.class);
        } catch (JsonProcessingException ex) {
            throw new InvalidEventException("Stored payload is invalid", ex);
        }

        if (!semanticEquals(event, stored)) {
            throw new InvalidEventException("Payload does not match canonical outbox event");
        }

        if (order == null) {
            throw new InvalidEventException("Order not found");
        }
        if (!Objects.equals(order.id, event.orderId())) {
            throw new InvalidEventException("orderId mismatch with order");
        }
        if (!Objects.equals(order.customer.id, event.customerId())) {
            throw new InvalidEventException("customerId mismatch with order");
        }
        if (order.total.compareTo(event.total()) != 0) {
            throw new InvalidEventException("total mismatch with order");
        }
        if (!sameInstant(order.createdAt, event.occurredAt())) {
            throw new InvalidEventException("occurredAt mismatch with order");
        }
    }

    public boolean semanticEquals(OrderCreatedEvent a, OrderCreatedEvent b) {
        return Objects.equals(a.eventId(), b.eventId())
                && Objects.equals(a.eventType(), b.eventType())
                && a.schemaVersion() == b.schemaVersion()
                && a.orderId() == b.orderId()
                && a.customerId() == b.customerId()
                && compareMoney(a.total(), b.total())
                && Objects.equals(a.currency(), b.currency())
                && sameInstant(a.occurredAt(), b.occurredAt());
    }

    private boolean compareMoney(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return Objects.equals(a, b);
        }
        return a.compareTo(b) == 0;
    }

    private boolean sameInstant(java.time.Instant a, java.time.Instant b) {
        if (a == null || b == null) {
            return Objects.equals(a, b);
        }
        return a.truncatedTo(java.time.temporal.ChronoUnit.MICROS)
                .equals(b.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
    }

    public static class InvalidEventException extends RuntimeException {
        public InvalidEventException(String message) {
            super(message);
        }

        public InvalidEventException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
