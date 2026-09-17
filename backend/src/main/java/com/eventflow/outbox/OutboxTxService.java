package com.eventflow.outbox;

import com.eventflow.outbox.entity.OutboxEvent;
import com.eventflow.outbox.entity.OutboxStatus;
import com.eventflow.outbox.repository.OutboxRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class OutboxTxService {

    @Inject
    OutboxRepository outboxRepository;

    @Transactional
    public void markAttemptStarted(UUID eventId, Instant attemptStart) {
        OutboxEvent event = outboxRepository.findByEventId(eventId).orElse(null);
        if (event == null || event.status == OutboxStatus.PUBLISHED) {
            return;
        }
        event.attempts = event.attempts + 1;
        event.nextAttemptAt = attemptStart.plusSeconds(5);
        event.lastError = null;
    }

    @Transactional
    public void markPublished(UUID eventId, Instant publishedAt) {
        OutboxEvent event = outboxRepository.findByEventId(eventId).orElse(null);
        if (event == null) {
            return;
        }
        event.status = OutboxStatus.PUBLISHED;
        event.publishedAt = publishedAt;
        event.lastError = null;
    }

    @Transactional
    public void markAttemptFailed(UUID eventId, String error, Instant nextAttemptAt) {
        OutboxEvent event = outboxRepository.findByEventId(eventId).orElse(null);
        if (event == null || event.status == OutboxStatus.PUBLISHED) {
            return;
        }
        event.status = OutboxStatus.PENDING;
        event.lastError = error;
        event.nextAttemptAt = nextAttemptAt;
    }
}
