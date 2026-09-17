package com.eventflow.outbox;

import com.eventflow.common.ClockService;
import com.eventflow.outbox.entity.OutboxEvent;
import com.eventflow.outbox.repository.OutboxRepository;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class OutboxPublisher {

    private static final Logger LOG = Logger.getLogger(OutboxPublisher.class);
    private static final int BATCH_SIZE = 20;
    private static final Duration PUBLISH_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration RETRY_DELAY = Duration.ofSeconds(5);

    @Inject
    OutboxRepository outboxRepository;

    @Inject
    OutboxTxService outboxTxService;

    @Inject
    ClockService clockService;

    @Inject
    @Channel("order-created-out")
    Emitter<String> emitter;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(every = "1s", delayed = "2s")
    void publishPending() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            Instant now = clockService.now();
            List<OutboxEvent> pending = outboxRepository.findPendingDue(now, BATCH_SIZE);
            for (OutboxEvent event : pending) {
                publishOne(event);
            }
        } finally {
            running.set(false);
        }
    }

    void publishOne(OutboxEvent event) {
        Instant attemptStart = clockService.now();
        outboxTxService.markAttemptStarted(event.eventId, attemptStart);

        try {
            OutgoingKafkaRecordMetadata<String> metadata = OutgoingKafkaRecordMetadata.<String>builder()
                    .withKey(String.valueOf(event.orderId))
                    .build();

            java.util.concurrent.CompletableFuture<Void> acked = new java.util.concurrent.CompletableFuture<>();
            Message<String> message = Message.of(event.payload, Metadata.of(metadata))
                    .withAck(() -> {
                        acked.complete(null);
                        return java.util.concurrent.CompletableFuture.completedFuture(null);
                    })
                    .withNack(reason -> {
                        acked.completeExceptionally(reason);
                        return java.util.concurrent.CompletableFuture.completedFuture(null);
                    });

            emitter.send(message);
            acked.get(PUBLISH_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);

            outboxTxService.markPublished(event.eventId, clockService.now());
            LOG.infof("Outbox event published eventId=%s orderId=%d", event.eventId, event.orderId);
        } catch (Exception ex) {
            String error = truncate(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(), 1000);
            outboxTxService.markAttemptFailed(event.eventId, error, clockService.now().plus(RETRY_DELAY));
            LOG.warnf(ex, "Failed to publish outbox event eventId=%s orderId=%d", event.eventId, event.orderId);
        }
    }

    private String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
