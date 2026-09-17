package com.eventflow.order.dto;

import com.eventflow.common.MoneySerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.math.BigDecimal;

public record OrderItemResponse(
        long productId,
        String productName,
        int quantity,
        @JsonSerialize(using = MoneySerializer.class) BigDecimal unitPrice,
        @JsonSerialize(using = MoneySerializer.class) BigDecimal subtotal
) {
}
