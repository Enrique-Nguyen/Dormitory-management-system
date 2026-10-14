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
import vn.edu.tlu.dms.room.service.SemesterService;

import java.net.URI;
import java.util.List;

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
    @GetMapping("/api/semesters")
    public List<SemesterResponse> getSemesterList() {
        return service.getAll();
    }

    @RequirePrivilege(Permission.SEMESTER_READ)
    @GetMapping("/api/semesters/{id}")
    public SemesterResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @RequirePrivilege(Permission.SEMESTER_CREATE)
    @PostMapping("/api/semesters")
    public ResponseEntity<SemesterResponse> createSemester(
            @Valid @RequestBody SemesterRequest input) {
        var result = service.create(input);
        return ResponseEntity.created(URI.create("/api/semesters/" + result.id())).body(result);
    }

    @RequirePrivilege(Permission.SEMESTER_UPDATE)
    @PatchMapping("/api/semesters/{id}")
    public SemesterResponse updateSemester(
            @PathVariable Long id, @Valid @RequestBody SemesterRequest input) {
        return service.update(id, input);
    }

    @RequirePrivilege(Permission.SEMESTER_DELETE)
    @DeleteMapping("/api/semesters/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
