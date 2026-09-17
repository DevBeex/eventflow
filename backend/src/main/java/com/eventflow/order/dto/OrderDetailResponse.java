package com.eventflow.order.dto;

import com.eventflow.common.MoneySerializer;
import com.eventflow.customer.dto.CustomerResponse;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDetailResponse(
        long id,
        CustomerResponse customer,
        String status,
        @JsonSerialize(using = MoneySerializer.class) BigDecimal total,
        String currency,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
}
