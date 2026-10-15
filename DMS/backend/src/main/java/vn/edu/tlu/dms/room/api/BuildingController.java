package vn.edu.tlu.dms.room.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.room.service.BuildingService;

@RestController
public class BuildingController {

    private final BuildingService service;

    public BuildingController(BuildingService service) {
        this.service = service;
    }

    // Danh mục dùng chung cho form; chỉ cần một quyền trong danh sách (OR).
    @RequirePrivilege({Permission.BUILDING_READ, Permission.ROOM_CREATE, Permission.ROOM_UPDATE})
    @PostMapping("/api/buildings/search-by-page")
    public PageResponse<BuildingResponse> searchByPage(@Valid @RequestBody SearchRequest search) {
        return service.searchByPage(search);
    }

    @RequirePrivilege(Permission.BUILDING_READ)
    @GetMapping("/api/buildings/{id}")
    public BuildingResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @RequirePrivilege({Permission.BUILDING_CREATE, Permission.BUILDING_UPDATE})
    @PostMapping("/api/admin/buildings/save-or-update")
    public BuildingResponse saveOrUpdate(@Valid @RequestBody BuildingRequest input) {
        return service.saveOrUpdate(input);
    }

    @RequirePrivilege(Permission.BUILDING_DELETE)
    @DeleteMapping("/api/admin/buildings/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
