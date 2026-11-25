package vn.edu.tlu.dms.room.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.room.service.FloorService;

import java.util.Map;

@RestController
@RequestMapping("/api/floors")
public class FloorController {
    private final FloorService service;

    public FloorController(FloorService service) { this.service = service; }

    @PostMapping("/search-by-page")
    @RequirePrivilege(Permission.CATALOG_READ)
    public PageResponse<Map<String, Object>> searchByPage(@Valid @RequestBody SearchRequest input) {
        return service.searchByPage(input);
    }

    @GetMapping("/{id}")
    @RequirePrivilege(Permission.CATALOG_READ)
    public Map<String, Object> getById(@PathVariable Long id) { return service.getById(id); }

    @PostMapping("/save-or-update")
    @RequirePrivilege({Permission.CATALOG_CREATE, Permission.CATALOG_UPDATE})
    public Map<String, Object> saveOrUpdate(@Valid @RequestBody FloorRequest input) {
        return service.saveOrUpdate(input);
    }

    @DeleteMapping("/{id}")
    @RequirePrivilege(Permission.CATALOG_DELETE)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
