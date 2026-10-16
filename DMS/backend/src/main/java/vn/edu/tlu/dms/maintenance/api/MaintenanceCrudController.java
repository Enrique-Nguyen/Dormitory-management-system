package vn.edu.tlu.dms.maintenance.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.maintenance.service.MaintenanceCrudService;

@RestController
public class MaintenanceCrudController {

    private final MaintenanceCrudService service;

    public MaintenanceCrudController(MaintenanceCrudService service) {
        this.service = service;
    }

    // Xóa ở khung CRUD là hủy yêu cầu, không xóa lịch sử xử lý.
    @RequirePrivilege(Permission.MAINTENANCE_DELETE)
    @DeleteMapping("/api/maintenance-requests/{id}")
    public ResponseEntity<Void> cancelRequest(@PathVariable Long id) {
        service.cancelOpenRequest(id);
        return ResponseEntity.noContent().build();
    }
}
