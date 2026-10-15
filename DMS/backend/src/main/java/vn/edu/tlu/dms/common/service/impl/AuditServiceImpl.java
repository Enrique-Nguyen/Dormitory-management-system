package vn.edu.tlu.dms.common.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.domain.AuditLog;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.repository.AuditLogRepository;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.support.SqlPage;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

@Service
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository repository;

    private final ObjectMapper mapper;

    private final SqlPage queries;

    private final ScopeGuard guard;

    public AuditServiceImpl(
            AuditLogRepository repository, ObjectMapper mapper, SqlPage queries, ScopeGuard guard) {
        this.repository = repository;
        this.mapper = mapper;
        this.queries = queries;
        this.guard = guard;
    }

    /** Tất cả giá trị lọc đều bind bằng tham số; không ghép nội dung client vào SQL. */
    @Transactional(readOnly = true)
    @Override
    public PageResponse<Map<String, Object>> searchByPage(SearchRequest input) {
        String q = input.q();
        Long actorId = input.actorId();
        String action = input.action();
        String entityType = input.entityType();
        OffsetDateTime from = input.from();
        OffsetDateTime to = input.to();
        guard.requirePermission(Permission.AUDIT_READ);
        if (actorId != null && actorId <= 0) {
            throw BusinessException.invalid("actorId", "ID người thực hiện phải lớn hơn 0");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw BusinessException.invalid("to", "Mốc kết thúc phải bằng hoặc sau mốc bắt đầu");
        }
        String keyword = q == null ? "" : q.trim();
        var args = new ArrayList<Object>();
        args.add("%" + keyword + "%");
        args.add("%" + keyword + "%");
        StringBuilder sql =
                new StringBuilder(
                        "SELECT"
                            + " a.id,a.actor_id,u.full_name,a.action,a.entity_type,a.entity_id,a.reason,a.request_id,a.created_at"
                            + " FROM audit_log a LEFT JOIN app_user u ON u.id=a.actor_id WHERE"
                            + " (a.action ILIKE ? OR u.full_name ILIKE ?)");
        if (actorId != null) {
            sql.append(" AND a.actor_id = ?");
            args.add(actorId);
        }
        if (action != null && !action.isBlank()) {
            sql.append(" AND a.action = ?");
            args.add(action.trim());
        }
        if (entityType != null && !entityType.isBlank()) {
            sql.append(" AND a.entity_type = ?");
            args.add(entityType.trim());
        }
        // Hai mốc đều inclusive; OffsetDateTime so sánh theo thời điểm thực khi khác múi giờ.
        if (from != null) {
            sql.append(" AND a.created_at >= ?");
            args.add(from);
        }
        if (to != null) {
            sql.append(" AND a.created_at <= ?");
            args.add(to);
        }
        return queries.search(
                sql.toString(),
                args,
                input,
                Set.of(
                        "id",
                        "actorId",
                        "fullName",
                        "action",
                        "entityType",
                        "entityId",
                        "reason",
                        "requestId",
                        "createdAt"),
                "id,desc");
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
