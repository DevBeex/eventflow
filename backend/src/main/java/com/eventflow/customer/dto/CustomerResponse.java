package com.eventflow.customer.dto;

import java.time.Instant;

public record CustomerResponse(
        long id,
        String name,
        String email,
        Instant createdAt,
        Instant updatedAt
) {
}
