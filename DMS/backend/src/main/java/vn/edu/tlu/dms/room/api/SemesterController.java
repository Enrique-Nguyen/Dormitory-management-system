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
import vn.edu.tlu.dms.room.service.SemesterService;

@RestController
public class SemesterController {

    private final SemesterService service;

    public SemesterController(SemesterService service) {
        this.service = service;
    }

    // Danh mục dùng chung cho form; chỉ cần một quyền trong danh sách (OR).
    @RequirePrivilege({
        Permission.SEMESTER_READ,
        Permission.REGISTRATION_PERIOD_CREATE,
        Permission.REGISTRATION_PERIOD_UPDATE
    })
    @PostMapping("/api/semesters/search-by-page")
    public PageResponse<SemesterResponse> searchByPage(@Valid @RequestBody SearchRequest search) {
        return service.searchByPage(search);
    }

    @RequirePrivilege(Permission.SEMESTER_READ)
    @GetMapping("/api/semesters/{id}")
    public SemesterResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @RequirePrivilege({Permission.SEMESTER_CREATE, Permission.SEMESTER_UPDATE})
    @PostMapping("/api/semesters/save-or-update")
    public SemesterResponse saveOrUpdate(@Valid @RequestBody SemesterRequest input) {
        return service.saveOrUpdate(input);
    }

    @RequirePrivilege(Permission.SEMESTER_DELETE)
    @DeleteMapping("/api/semesters/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
