package com.allobank.splitbill.repository;

import com.allobank.splitbill.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    List<AuditLog> findByEventTypeOrderByCreatedAtDesc(String eventType);

    List<AuditLog> findByGroupIdAndEventTypeOrderByCreatedAtDesc(
            Long groupId,
            String eventType
    );
}
