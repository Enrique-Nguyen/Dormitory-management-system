package vn.edu.tlu.dms.notification.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.support.SqlPage;
import vn.edu.tlu.dms.notification.api.NotificationDtos;
import vn.edu.tlu.dms.notification.service.NotificationCrudService;

import java.util.ArrayList;
import java.util.Objects;

/** CRUD bài thủ công: STAFF tự đăng, ADMIN mọi bài. Không sửa/xóa thông báo hệ thống. */
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
        guard.requirePermission(Permission.NOTIFICATION_READ);
        guard.requireRole(Role.STAFF);
        var args = new ArrayList<Object>();
        // Admin xem thông báo thủ công của mọi người; Staff vẫn chỉ quản lý bài tự đăng.
        String owner = current.get().isAdmin() ? "TRUE" : "n.created_by=?";
        if (!current.get().isAdmin()) {
            args.add(current.get().id());
        }
        args.add("%" + q + "%");
        args.add("%" + q + "%");
        return queries.search(
                "SELECT n.id,n.scope,n.title,n.body,n.created_at,n.created_by,n.management_unit_id"
                        + " AS unit_id,n.building_id,n.floor,n.room_id FROM notification n WHERE "
                        + owner
                        + " AND n.source_type IS NULL AND (n.title ILIKE ? OR n.body"
                        + " ILIKE ?)",
                args,
                page,
                size);
    }

    @Override()
    public Object getPublished(Long id) {
        guard.requirePermission(Permission.NOTIFICATION_READ);
        guard.requireRole(Role.STAFF);
        var args = new ArrayList<Object>();
        args.add(id);
        String owner = current.get().isAdmin() ? "TRUE" : "created_by=?";
        if (!current.get().isAdmin()) {
            args.add(current.get().id());
        }
        return queries.one(
                "SELECT id,scope,title,body,created_at,management_unit_id AS"
                        + " unit_id,building_id,floor,room_id FROM notification WHERE id=? AND "
                        + owner
                        + " AND source_type IS NULL",
                args.toArray());
    }

    @Transactional
    @Override()
    public Object updatePublished(Long id, NotificationDtos.Create input) {
        guard.requirePermission(Permission.NOTIFICATION_UPDATE);
        getPublished(id);
        jdbc.queryForObject("SELECT id FROM notification WHERE id=? FOR UPDATE", Long.class, id);
        // Chỉ sửa nội dung; đổi phạm vi làm thay đổi người nhận cũ nên phải tạo thông báo mới.
        var row =
                queries.one(
                        "SELECT scope,management_unit_id,building_id,floor,room_id FROM"
                                + " notification WHERE id=?",
                        id);
        if (!input.scope().equals(row.get("scope"))
                || !Objects.equals(input.unitId(), row.get("managementUnitId"))
                || !Objects.equals(input.buildingId(), row.get("buildingId"))
                || !Objects.equals(input.roomId(), row.get("roomId"))
                || !Objects.equals(input.floor(), row.get("floor"))) {
            throw BusinessException.invalid("scope", "Không đổi phạm vi thông báo đã đăng");
        }
        jdbc.update(
                "UPDATE notification SET title=?,body=? WHERE id=?",
                input.title().trim(),
                input.body().trim(),
                id);
        audit.record(
                current.get().id(),
                AuditAction.NOTIFICATION_UPDATE,
                "NOTIFICATION",
                id,
                null,
                input,
                null);
        return getPublished(id);
    }

    @Transactional
    @Override()
    public void deletePublished(Long id) {
        guard.requirePermission(Permission.NOTIFICATION_DELETE);
        var before = getPublished(id);
        jdbc.queryForObject("SELECT id FROM notification WHERE id=? FOR UPDATE", Long.class, id);
        jdbc.update("DELETE FROM notification_read WHERE notification_id=?", id);
        jdbc.update("DELETE FROM notification WHERE id=?", id);
        audit.record(
                current.get().id(),
                AuditAction.NOTIFICATION_DELETE,
                "NOTIFICATION",
                id,
                before,
                null,
                null);
    }
}
