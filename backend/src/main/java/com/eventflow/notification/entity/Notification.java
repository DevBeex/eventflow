package com.eventflow.notification.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "event_id", nullable = false)
    public UUID eventId;

    @Column(name = "order_id", nullable = false)
    public Long orderId;

    @Column(nullable = false, length = 32)
    public String type;

    @Column(nullable = false, length = 255)
    public String message;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
