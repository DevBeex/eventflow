package com.eventflow.notification.consumer;

import com.eventflow.notification.service.NotificationProcessor;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.hibernate.exception.JDBCConnectionException;
import org.jboss.logging.Logger;

import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class OrderCreatedConsumer {

    private static final Logger LOG = Logger.getLogger(OrderCreatedConsumer.class);
    private static final int MAX_DB_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 2000L;

    @Inject
    NotificationProcessor notificationProcessor;

    @Inject
    ConsumerControl consumerControl;

    @Incoming("order-created-in")
    @Blocking
    public CompletionStage<Void> consume(Message<String> message) {
        if (consumerControl.isStopped()) {
            LOG.warn("Consumer is stopped; nacking message");
            return message.nack(new IllegalStateException("Consumer stopped"));
        }

        String payload = message.getPayload();
        IncomingKafkaRecordMetadata metadata = extractMetadata(message);

        try {
            processWithRetries(payload);
            return message.ack();
        } catch (InvalidMessageException ex) {
            LOG.errorf(ex, "Invalid message at topic=%s partition=%d offset=%d eventId=%s",
                    metadata.topic(), metadata.partition(), metadata.offset(), ex.eventId());
            consumerControl.stop();
            return message.nack(ex);
        } catch (TransientDbException ex) {
            LOG.errorf(ex, "Transient DB failure after retries at topic=%s partition=%d offset=%d",
                    metadata.topic(), metadata.partition(), metadata.offset());
            consumerControl.stop();
            return message.nack(ex);
        }
    }

    private void processWithRetries(String payload) {
        int attempt = 0;
        while (true) {
            try {
                notificationProcessor.process(payload);
                return;
            } catch (InvalidMessageException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                if (isTransientDb(ex)) {
                    attempt++;
                    if (attempt > MAX_DB_RETRIES) {
                        throw new TransientDbException(ex);
                    }
                    LOG.warnf("Transient DB error on attempt %d, retrying in %dms", attempt, RETRY_DELAY_MS);
                    sleep(RETRY_DELAY_MS);
                } else {
                    throw ex;
                }
            }
        }
    }

    private boolean isTransientDb(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof JDBCConnectionException) {
                return true;
            }
            if (current instanceof java.sql.SQLException sqlEx) {
                String state = sqlEx.getSQLState();
                if ("08000".equals(state) || "08003".equals(state) || "08006".equals(state) || "57P01".equals(state)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private IncomingKafkaRecordMetadata extractMetadata(Message<String> message) {
        return message.getMetadata(IncomingKafkaRecord.class)
                .map(record -> new IncomingKafkaRecordMetadata(
                        record.getTopic(),
                        record.getPartition(),
                        record.getOffset()))
                .orElse(new IncomingKafkaRecordMetadata("order-created", -1, -1));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TransientDbException(ex);
        }
    }

    public record IncomingKafkaRecordMetadata(String topic, int partition, long offset) {
    }

    public static class InvalidMessageException extends RuntimeException {
        private final String eventId;

        public InvalidMessageException(String message, String eventId) {
            super(message);
            this.eventId = eventId;
        }

        public String eventId() {
            return eventId;
        }
    }

    public static class TransientDbException extends RuntimeException {
        public TransientDbException(Throwable cause) {
            super(cause);
        }
    }
}
