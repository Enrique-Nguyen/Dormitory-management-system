package vn.edu.tlu.dms.room.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.room.service.BuildingService;

import java.net.URI;
import java.util.List;

@RestController
public class BuildingController {

    private final BuildingService service;

    public BuildingController(BuildingService service) {
        this.service = service;
    }

    // Danh mục dùng chung cho form; chỉ cần một quyền trong danh sách (OR).
    @RequirePrivilege({Permission.BUILDING_READ, Permission.ROOM_CREATE, Permission.ROOM_UPDATE})
    @GetMapping("/api/buildings")
    public List<BuildingResponse> getBuildingList() {
        return service.getAll();
    }

    @RequirePrivilege(Permission.BUILDING_READ)
    @GetMapping("/api/buildings/{id}")
    public BuildingResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @RequirePrivilege(Permission.BUILDING_CREATE)
    @PostMapping("/api/admin/buildings")
    public ResponseEntity<BuildingResponse> createBuilding(
            @Valid @RequestBody BuildingRequest input) {
        var result = service.create(input);
        return ResponseEntity.created(URI.create("/api/buildings/" + result.id())).body(result);
    }

    @RequirePrivilege(Permission.BUILDING_UPDATE)
    @PatchMapping("/api/admin/buildings/{id}")
    public BuildingResponse updateBuilding(
            @PathVariable Long id, @Valid @RequestBody BuildingRequest input) {
        return service.update(id, input);
    }

    @RequirePrivilege(Permission.BUILDING_DELETE)
    @DeleteMapping("/api/admin/buildings/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
