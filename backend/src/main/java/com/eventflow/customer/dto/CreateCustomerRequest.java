package com.eventflow.customer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateCustomerRequest(String name, String email) {
}
