package vn.edu.tlu.dms.common.api;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.common.service.AuditService;

import java.time.OffsetDateTime;
import java.util.Map;

/** Nhật ký chỉ đọc: CSDL cũng cấm UPDATE/DELETE để bảo vệ dấu vết thao tác. */
@RestController
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/audit-logs")
    @RequirePrivilege(Permission.AUDIT_READ)
    public PageResponse<Map<String, Object>> searchAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime to) {
        return service.searchAuditLogs(page, size, q, actorId, action, entityType, from, to);
    }
}
