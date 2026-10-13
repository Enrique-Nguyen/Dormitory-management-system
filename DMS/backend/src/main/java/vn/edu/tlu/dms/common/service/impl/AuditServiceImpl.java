package vn.edu.tlu.dms.common.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.domain.AuditLog;
import vn.edu.tlu.dms.common.repository.AuditLogRepository;
import vn.edu.tlu.dms.common.service.AuditService;

import java.time.OffsetDateTime;

@Service
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository repository;

    private final ObjectMapper mapper;

    public AuditServiceImpl(AuditLogRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    @Override()
    public void record(
            Long actorId,
            String action,
            String type,
            Long id,
            Object before,
            Object after,
            String reason) {
        AuditLog row = new AuditLog();
        row.setActorId(actorId);
        row.setAction(action);
        row.setEntityType(type);
        row.setEntityId(id);
        row.setOldValue(json(before));
        row.setNewValue(json(after));
        row.setReason(reason);
        row.setRequestId(MDC.get("requestId"));
        row.setCreatedAt(OffsetDateTime.now());
        repository.save(row);
    }

    private String json(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
