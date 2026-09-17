package com.eventflow.outbox.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent extends PanacheEntityBase {

    @Id
    @Column(name = "event_id")
    public UUID eventId;

    @Column(name = "order_id", nullable = false)
    public Long orderId;

    @Column(name = "event_type", nullable = false, length = 32)
    public String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    public String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public OutboxStatus status;

    @Column(nullable = false)
    public long attempts;

    @Column(name = "next_attempt_at", nullable = false)
    public Instant nextAttemptAt;

    @Column(name = "last_error", length = 1000)
    public String lastError;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "published_at")
    public Instant publishedAt;
}
