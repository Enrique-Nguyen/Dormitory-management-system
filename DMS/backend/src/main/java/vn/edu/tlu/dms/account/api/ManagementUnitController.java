package vn.edu.tlu.dms.account.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.account.service.ManagementUnitService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;

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
    @PostMapping("/api/management-units/search-by-page")
    public PageResponse<ManagementUnitResponse> searchByPage(
            @Valid @RequestBody SearchRequest search) {
        return service.searchByPage(search);
    }

    @RequirePrivilege(Permission.MANAGEMENT_UNIT_READ)
    @GetMapping("/api/management-units/{id}")
    public ManagementUnitResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @RequirePrivilege({Permission.MANAGEMENT_UNIT_CREATE, Permission.MANAGEMENT_UNIT_UPDATE})
    @PostMapping("/api/admin/management-units/save-or-update")
    public ManagementUnitResponse saveOrUpdate(@Valid @RequestBody ManagementUnitRequest input) {
        return service.saveOrUpdate(input);
    }

    @RequirePrivilege(Permission.MANAGEMENT_UNIT_DELETE)
    @DeleteMapping("/api/admin/management-units/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
