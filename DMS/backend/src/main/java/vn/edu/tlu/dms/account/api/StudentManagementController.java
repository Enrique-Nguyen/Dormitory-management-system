package vn.edu.tlu.dms.account.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.account.service.StudentManagementService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Gender;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;

import java.time.LocalDate;
import java.util.Map;

@RestController
public class StudentManagementController {

    private final StudentManagementService service;

    public StudentManagementController(StudentManagementService service) {
        this.service = service;
    }

    public record Save(
            @Positive Long id,
            @Size(max = 20) String studentCode,
            @Size(max = 100) String fullName,
            @Pattern(regexp = Gender.PATTERN) String gender,
            LocalDate dateOfBirth,
            @Size(max = 100) String faculty,
            @Size(max = 10) String cohort,
            @Size(max = 20) String phone,
            @Size(max = 200) String hometown,
            @Size(max = 200) String emergencyContact) {}

    @RequirePrivilege(Permission.STUDENT_READ)
    @PostMapping({"/api/students/search-by-page", "/api/admin/students/search-by-page"})
    public PageResponse<Map<String, Object>> searchByPage(@Valid @RequestBody SearchRequest input) {
        return service.searchByPage(input);
    }

    @RequirePrivilege(Permission.STUDENT_READ)
    @GetMapping({"/api/students/{id}", "/api/admin/students/{id}"})
    public Map<String, Object> getStudentById(@PathVariable Long id) {
        return service.getStudentById(id);
    }

    @RequirePrivilege({Permission.STUDENT_CREATE, Permission.STUDENT_UPDATE})
    @PostMapping({"/api/admin/students/save-or-update", "/api/students/save-or-update"})
    public Object saveOrUpdate(@Valid @RequestBody Save input) {
        return service.saveOrUpdate(input);
    }

    @RequirePrivilege(Permission.STUDENT_DELETE)
    @DeleteMapping("/api/admin/students/{id}")
    public ResponseEntity<Void> deleteUnusedStudent(@PathVariable Long id) {
        service.deleteUnusedStudent(id);
        return ResponseEntity.noContent().build();
    }

    @RequirePrivilege(Permission.STUDENT_RESET_PASSWORD)
    @PostMapping("/api/students/{id}/reset-password")
    public AccountDtos.TemporaryPassword resetStudentPassword(@PathVariable Long id) {
        return service.resetStudentPassword(id);
    }

    @RequirePrivilege(Permission.MAINTENANCE_UPDATE)
    @PostMapping("/api/technicians/search-by-page")
    public PageResponse<Map<String, Object>> searchTechniciansByPage(
            @Valid @RequestBody SearchRequest input) {
        return service.searchTechniciansByPage(input);
    }
}
