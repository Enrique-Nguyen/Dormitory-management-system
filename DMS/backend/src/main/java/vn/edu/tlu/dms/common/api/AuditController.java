package vn.edu.tlu.dms.common.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.support.SqlPage;

import java.util.ArrayList;
import java.util.Map;

/** Nhật ký chỉ đọc: CSDL cũng cấm UPDATE/DELETE để bảo vệ dấu vết thao tác. */
@RestController
public class AuditController {

    private final SqlPage queries;

    private final ScopeGuard guard;

    public AuditController(SqlPage queries, ScopeGuard guard) {
        this.queries = queries;
        this.guard = guard;
    }

    @GetMapping("/api/admin/audit-logs")
    @RequirePrivilege(Permission.AUDIT_READ)
    public PageResponse<Map<String, Object>> searchAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String q) {
        guard.requirePermission(Permission.AUDIT_READ);
        var args = new ArrayList<Object>();
        args.add("%" + q + "%");
        args.add("%" + q + "%");
        return queries.search(
                "SELECT"
                    + " a.id,a.actor_id,u.full_name,a.action,a.entity_type,a.entity_id,a.reason,a.request_id,a.created_at"
                    + " FROM audit_log a LEFT JOIN app_user u ON u.id=a.actor_id WHERE a.action"
                    + " ILIKE ? OR u.full_name ILIKE ?",
                args,
                page,
                size);
    }
}
