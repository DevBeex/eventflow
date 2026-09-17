package com.eventflow.outbox.repository;

import com.eventflow.outbox.entity.OutboxEvent;
import com.eventflow.outbox.entity.OutboxStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OutboxRepository implements PanacheRepository<OutboxEvent> {

    public List<OutboxEvent> findPendingDue(Instant now, int limit) {
        return find("""
                status = ?1 AND nextAttemptAt <= ?2
                ORDER BY nextAttemptAt ASC, createdAt ASC, eventId ASC
                """, OutboxStatus.PENDING, now)
                .page(0, limit)
                .list();
    }

    public Optional<OutboxEvent> findByEventId(UUID eventId) {
        return find("eventId = ?1", eventId).firstResultOptional();
    }
}
