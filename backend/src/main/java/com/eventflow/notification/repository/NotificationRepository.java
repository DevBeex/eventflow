package com.eventflow.notification.repository;

import com.eventflow.notification.entity.Notification;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class NotificationRepository implements PanacheRepository<Notification> {

    @Inject
    EntityManager entityManager;

    public long countFiltered(Long orderId) {
        if (orderId == null) {
            return count();
        }
        return count("orderId = ?1", orderId);
    }

    public List<Notification> findPage(int page, int size, Long orderId) {
        if (orderId == null) {
            return find("ORDER BY createdAt DESC, id DESC")
                    .page(page, size)
                    .list();
        }
        return find("orderId = ?1 ORDER BY createdAt DESC, id DESC", orderId)
                .page(page, size)
                .list();
    }

    public Optional<Notification> findByEventId(UUID eventId) {
        return find("eventId = ?1", eventId).firstResultOptional();
    }

    public Optional<Notification> findByOrderIdAndType(long orderId, String type) {
        return find("orderId = ?1 AND type = ?2", orderId, type).firstResultOptional();
    }

    public Optional<Long> insertIfAbsent(UUID eventId, long orderId, String type, String message, Instant createdAt) {
        List<?> result = entityManager.createNativeQuery("""
                INSERT INTO notifications (event_id, order_id, type, message, created_at)
                VALUES (?1, ?2, ?3, ?4, ?5)
                ON CONFLICT (event_id) DO NOTHING
                RETURNING id
                """)
                .setParameter(1, eventId)
                .setParameter(2, orderId)
                .setParameter(3, type)
                .setParameter(4, message)
                .setParameter(5, createdAt)
                .getResultList();

        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(((Number) result.get(0)).longValue());
    }
}
