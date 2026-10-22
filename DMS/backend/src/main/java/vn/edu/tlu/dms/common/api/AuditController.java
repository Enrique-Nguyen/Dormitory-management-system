package vn.edu.tlu.dms.common.api;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.common.service.AuditService;

import java.util.Map;

/** Nhật ký chỉ đọc: CSDL cũng cấm UPDATE/DELETE để bảo vệ dấu vết thao tác. */
@RestController
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @PostMapping("/api/admin/audit-logs/search-by-page")
    @RequirePrivilege(Permission.AUDIT_READ)
    public PageResponse<Map<String, Object>> searchByPage(@Valid @RequestBody SearchRequest input) {
        return service.searchByPage(input);
    }

    @GetMapping("/api/admin/audit-logs/{id}")
    @RequirePrivilege(Permission.AUDIT_READ)
    public Map<String, Object> getAuditById(@PathVariable Long id) {
        return service.getAuditById(id);
    }
}
