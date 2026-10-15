package vn.edu.tlu.dms.registration.api;

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
import vn.edu.tlu.dms.registration.service.PriorityCategoryService;

@RestController
public class PriorityCategoryController {

    private final PriorityCategoryService service;

    public PriorityCategoryController(PriorityCategoryService service) {
        this.service = service;
    }

    // Danh mục dùng chung cho form; chỉ cần một quyền trong danh sách (OR).
    @RequirePrivilege({
        Permission.PRIORITY_CATEGORY_READ,
        Permission.ROOM_APPLICATION_CREATE,
        Permission.ROOM_APPLICATION_UPDATE
    })
    @PostMapping("/api/priority-categories/search-by-page")
    public PageResponse<PriorityCategoryResponse> searchByPage(
            @Valid @RequestBody SearchRequest search) {
        return service.searchByPage(search);
    }

    @RequirePrivilege(Permission.PRIORITY_CATEGORY_READ)
    @GetMapping("/api/priority-categories/{id}")
    public PriorityCategoryResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @RequirePrivilege({Permission.PRIORITY_CATEGORY_CREATE, Permission.PRIORITY_CATEGORY_UPDATE})
    @PostMapping("/api/admin/priority-categories/save-or-update")
    public PriorityCategoryResponse saveOrUpdate(
            @Valid @RequestBody PriorityCategoryRequest input) {
        return service.saveOrUpdate(input);
    }

    @RequirePrivilege(Permission.PRIORITY_CATEGORY_DELETE)
    @DeleteMapping("/api/admin/priority-categories/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
