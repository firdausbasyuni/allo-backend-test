package com.allobank.splitbill.service;

import com.allobank.splitbill.entity.AuditLog;
import com.allobank.splitbill.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog logEvent(
            String eventType,
            Long groupId,
            Long entityId,
            String details
    ) {
        AuditLog auditLog = new AuditLog(
                eventType,
                groupId,
                entityId,
                details
        );

        return auditLogRepository.save(auditLog);
    }
}
