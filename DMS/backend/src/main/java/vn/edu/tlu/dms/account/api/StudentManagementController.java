package vn.edu.tlu.dms.account.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.account.service.StudentManagementService;
import vn.edu.tlu.dms.common.dto.PageResponse;

import java.time.LocalDate;
import java.util.Map;

@RestController
public class StudentManagementController {

    private final StudentManagementService service;

    public StudentManagementController(StudentManagementService service) {
        this.service = service;
    }

    public record StudentInput(
            @NotBlank @Size(max = 20) String studentCode,
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Pattern(regexp = "M|F") String gender,
            LocalDate dateOfBirth,
            @Size(max = 100) String faculty,
            @Size(max = 10) String cohort,
            @Size(max = 20) String phone,
            @Size(max = 200) String hometown,
            @Size(max = 200) String emergencyContact) {}

    /** PATCH: trường không gửi (null) giữ nguyên giá trị hiện có, không hỗ trợ xóa bằng null. */
    public record StudentPatch(
            @Size(max = 20) String studentCode,
            @Size(max = 100) String fullName,
            @Pattern(regexp = "M|F") String gender,
            LocalDate dateOfBirth,
            @Size(max = 100) String faculty,
            @Size(max = 10) String cohort,
            @Size(max = 20) String phone,
            @Size(max = 200) String hometown,
            @Size(max = 200) String emergencyContact) {}

    @GetMapping({"/api/students", "/api/admin/students"})
    public PageResponse<Map<String, Object>> searchStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String q) {
        return service.searchStudents(page, size, q);
    }

    @GetMapping({"/api/students/{id}", "/api/admin/students/{id}"})
    public Map<String, Object> getStudentById(@PathVariable Long id) {
        return service.getStudentById(id);
    }

    @PostMapping("/api/admin/students")
    public AccountDtos.TemporaryPassword createStudent(@Valid @RequestBody StudentInput input) {
        return service.createStudent(input);
    }

    @PatchMapping({"/api/students/{id}", "/api/admin/students/{id}"})
    public Map<String, Object> updateStudent(
            @PathVariable Long id, @Valid @RequestBody StudentPatch input) {
        return service.updateStudent(id, input);
    }

    @DeleteMapping("/api/admin/students/{id}")
    public ResponseEntity<Void> deleteUnusedStudent(@PathVariable Long id) {
        service.deleteUnusedStudent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/students/{id}/reset-password")
    public AccountDtos.TemporaryPassword resetStudentPassword(@PathVariable Long id) {
        return service.resetStudentPassword(id);
    }

    @GetMapping("/api/technicians")
    public Object getTechnicians() {
        return service.getTechnicians();
    }
}
