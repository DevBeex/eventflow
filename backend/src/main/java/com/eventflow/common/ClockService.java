package com.eventflow.common;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class ClockService {

    /**
     * Returns the current instant truncated to microseconds to match PostgreSQL TIMESTAMPTZ
     * and keep outbox occurredAt aligned with orders.created_at after round-trips.
     */
    public Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
