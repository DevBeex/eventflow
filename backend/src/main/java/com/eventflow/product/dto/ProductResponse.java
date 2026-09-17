package com.eventflow.product.dto;

import com.eventflow.common.MoneySerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        long id,
        String name,
        String description,
        @JsonSerialize(using = MoneySerializer.class) BigDecimal price,
        String currency,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
