package vn.edu.tlu.dms.notification.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.support.SqlPage;
import vn.edu.tlu.dms.notification.api.NotificationDtos;
import vn.edu.tlu.dms.notification.service.NotificationCrudService;

import java.util.ArrayList;
import java.util.List;

/** CRUD thông báo thủ công của người đăng. Không sửa/xóa thông báo hệ thống. */
@Service
@Transactional(readOnly = true)
public class NotificationCrudServiceImpl implements NotificationCrudService {

    private final JdbcTemplate jdbc;

    private final SqlPage queries;

    private final CurrentUser current;

    private final ScopeGuard guard;

    private final AuditService audit;

    public NotificationCrudServiceImpl(
            JdbcTemplate jdbc,
            SqlPage queries,
            CurrentUser current,
            ScopeGuard guard,
            AuditService audit) {
        this.jdbc = jdbc;
        this.queries = queries;
        this.current = current;
        this.guard = guard;
        this.audit = audit;
    }

    @Override()
    public Object searchPublished(int page, int size, String q) {
        guard.requireRole("STAFF");
        return queries.search(
                "SELECT n.id,n.scope,n.title,n.body,n.created_at,n.created_by,n.management_unit_id"
                        + " AS unit_id,n.building_id,n.floor,n.room_id FROM notification n WHERE"
                        + " n.created_by=? AND n.source_type IS NULL AND (n.title ILIKE ? OR n.body"
                        + " ILIKE ?)",
                new ArrayList<>(List.of(current.get().id(), "%" + q + "%", "%" + q + "%")),
                page,
                size);
    }

    @Override()
    public Object getPublished(Long id) {
        guard.requireRole("STAFF");
        return queries.one(
                "SELECT id,scope,title,body,created_at,management_unit_id AS"
                        + " unit_id,building_id,floor,room_id FROM notification WHERE id=? AND"
                        + " created_by=? AND source_type IS NULL",
                id,
                current.get().id());
    }

    @Transactional
    @Override()
    public Object updatePublished(Long id, NotificationDtos.Create input) {
        getPublished(id);
        jdbc.queryForObject("SELECT id FROM notification WHERE id=? FOR UPDATE", Long.class, id);
        // Chỉ sửa nội dung; đổi phạm vi làm thay đổi người nhận cũ nên phải tạo thông báo mới.
        var row =
                queries.one(
                        "SELECT scope,management_unit_id,building_id,floor,room_id FROM"
                                + " notification WHERE id=?",
                        id);
        if (!input.scope().equals(row.get("scope"))
                || !sameNumber(input.unitId(), row.get("managementUnitId"))
                || !sameNumber(input.buildingId(), row.get("buildingId"))
                || !sameNumber(input.roomId(), row.get("roomId"))
                || !sameNumber(input.floor(), row.get("floor"))) {
            throw BusinessException.invalid("scope", "Không đổi phạm vi thông báo đã đăng");
        }
        jdbc.update(
                "UPDATE notification SET title=?,body=? WHERE id=?",
                input.title().trim(),
                input.body().trim(),
                id);
        audit.record(
                current.get().id(), "NOTIFICATION_UPDATE", "NOTIFICATION", id, null, input, null);
        return getPublished(id);
    }

    /** So sánh theo giá trị số: DTO dùng Short/Long còn JDBC trả Integer/Long cho cùng một cột. */
    private static boolean sameNumber(Number requested, Object stored) {
        if (requested == null || stored == null) {
            return requested == null && stored == null;
        }
        return stored instanceof Number number && requested.longValue() == number.longValue();
    }

    @Transactional
    @Override()
    public void deletePublished(Long id) {
        var before = getPublished(id);
        jdbc.queryForObject("SELECT id FROM notification WHERE id=? FOR UPDATE", Long.class, id);
        jdbc.update("DELETE FROM notification_read WHERE notification_id=?", id);
        jdbc.update("DELETE FROM notification WHERE id=?", id);
        audit.record(
                current.get().id(), "NOTIFICATION_DELETE", "NOTIFICATION", id, before, null, null);
    }
}
