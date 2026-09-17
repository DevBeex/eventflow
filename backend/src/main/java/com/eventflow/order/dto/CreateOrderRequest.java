package com.eventflow.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateOrderRequest(Long customerId, List<OrderItemRequest> items) {

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record OrderItemRequest(Long productId, Integer quantity) {
    }
}
