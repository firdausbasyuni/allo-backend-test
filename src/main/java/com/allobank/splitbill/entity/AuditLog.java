package com.allobank.splitbill.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false)
    private Long groupId;

    @Column(nullable = false)
    private Long entityId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(
            String eventType,
            Long groupId,
            Long entityId,
            String details
    ) {
        this.eventType = eventType;
        this.groupId = groupId;
        this.entityId = entityId;
        this.details = details;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getGroupId() {
        return groupId;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
