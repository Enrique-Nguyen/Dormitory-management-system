package vn.edu.tlu.dms.account.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.account.service.ManagementUnitService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.security.RequirePrivilege;

import java.net.URI;
import java.util.List;

@RestController
public class ManagementUnitController {

    private final ManagementUnitService service;

    public ManagementUnitController(ManagementUnitService service) {
        this.service = service;
    }

    // Danh mục dùng chung cho form; chỉ cần một quyền trong danh sách (OR).
    @RequirePrivilege({
        Permission.MANAGEMENT_UNIT_READ,
        Permission.BUILDING_CREATE,
        Permission.BUILDING_UPDATE,
        Permission.REGISTRATION_PERIOD_CREATE,
        Permission.REGISTRATION_PERIOD_UPDATE
    })
    @GetMapping("/api/management-units")
    public List<ManagementUnitResponse> getManagementUnitList() {
        return service.getAll();
    }

    @RequirePrivilege(Permission.MANAGEMENT_UNIT_READ)
    @GetMapping("/api/management-units/{id}")
    public ManagementUnitResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @RequirePrivilege(Permission.MANAGEMENT_UNIT_CREATE)
    @PostMapping("/api/admin/management-units")
    public ResponseEntity<ManagementUnitResponse> createManagementUnit(
            @Valid @RequestBody ManagementUnitRequest input) {
        var result = service.create(input);
        return ResponseEntity.created(URI.create("/api/management-units/" + result.id()))
                .body(result);
    }

    @RequirePrivilege(Permission.MANAGEMENT_UNIT_UPDATE)
    @PatchMapping("/api/admin/management-units/{id}")
    public ManagementUnitResponse updateManagementUnit(
            @PathVariable Long id, @Valid @RequestBody ManagementUnitRequest input) {
        return service.update(id, input);
    }

    @RequirePrivilege(Permission.MANAGEMENT_UNIT_DELETE)
    @DeleteMapping("/api/admin/management-units/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
