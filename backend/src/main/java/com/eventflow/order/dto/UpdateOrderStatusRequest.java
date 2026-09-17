package com.eventflow.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = false)
public record UpdateOrderStatusRequest(String status) {
}
