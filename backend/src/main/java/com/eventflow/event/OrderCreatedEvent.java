package com.eventflow.event;

import com.eventflow.common.MoneySerializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = false)
public record OrderCreatedEvent(
        UUID eventId,
        String eventType,
        int schemaVersion,
        long orderId,
        long customerId,
        @JsonSerialize(using = MoneySerializer.class) BigDecimal total,
        String currency,
        Instant occurredAt
) {
    public static final String TYPE = "ORDER_CREATED";
    public static final int SCHEMA_VERSION = 1;
}
