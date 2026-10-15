package vn.edu.tlu.dms.account.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

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
import vn.edu.tlu.dms.common.constants.DmsConstants.Gender;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.security.RequirePrivilege;

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
            @NotBlank @Pattern(regexp = Gender.PATTERN) String gender,
            LocalDate dateOfBirth,
            @Size(max = 100) String faculty,
            @Size(max = 10) String cohort,
            @Size(max = 20) String phone,
            @Size(max = 200) String hometown,
            @Size(max = 200) String emergencyContact) {}

    /**
     * PATCH: trường không gửi giữ nguyên giá trị hiện có. Riêng ngày sinh, gửi {@code null} tường
     * minh thì xóa. Dùng lớp có setter vì record/Optional không phân biệt được key vắng mặt với
     * null: setter chỉ chạy khi key xuất hiện trong JSON.
     */
    public static final class StudentPatch {

        @JsonProperty @Size(max = 20) private String studentCode;

        @JsonProperty @Size(max = 100) private String fullName;

        @JsonProperty @Pattern(regexp = Gender.PATTERN) private String gender;

        private LocalDate dateOfBirth;

        private boolean dateOfBirthPresent;

        @JsonProperty @Size(max = 100) private String faculty;

        @JsonProperty @Size(max = 10) private String cohort;

        @JsonProperty @Size(max = 20) private String phone;

        @JsonProperty @Size(max = 200) private String hometown;

        @JsonProperty @Size(max = 200) private String emergencyContact;

        @JsonSetter("dateOfBirth")
        void setDateOfBirth(LocalDate value) {
            this.dateOfBirth = value;
            this.dateOfBirthPresent = true;
        }

        public String studentCode() {
            return studentCode;
        }

        public String fullName() {
            return fullName;
        }

        public String gender() {
            return gender;
        }

        public LocalDate dateOfBirth() {
            return dateOfBirth;
        }

        /** True khi JSON có key dateOfBirth (kể cả null); false thì giữ giá trị cũ. */
        public boolean dateOfBirthPresent() {
            return dateOfBirthPresent;
        }

        public String faculty() {
            return faculty;
        }

        public String cohort() {
            return cohort;
        }

        public String phone() {
            return phone;
        }

        public String hometown() {
            return hometown;
        }

        public String emergencyContact() {
            return emergencyContact;
        }
    }

    @RequirePrivilege(Permission.STUDENT_READ)
    @GetMapping({"/api/students", "/api/admin/students"})
    public PageResponse<Map<String, Object>> searchStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String q) {
        return service.searchStudents(page, size, q);
    }

    @RequirePrivilege(Permission.STUDENT_READ)
    @GetMapping({"/api/students/{id}", "/api/admin/students/{id}"})
    public Map<String, Object> getStudentById(@PathVariable Long id) {
        return service.getStudentById(id);
    }

    @RequirePrivilege(Permission.STUDENT_CREATE)
    @PostMapping("/api/admin/students")
    public AccountDtos.TemporaryPassword createStudent(@Valid @RequestBody StudentInput input) {
        return service.createStudent(input);
    }

    @RequirePrivilege(Permission.STUDENT_UPDATE)
    @PatchMapping({"/api/students/{id}", "/api/admin/students/{id}"})
    public Map<String, Object> updateStudent(
            @PathVariable Long id, @Valid @RequestBody StudentPatch input) {
        return service.updateStudent(id, input);
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
    @GetMapping("/api/technicians")
    public Object getTechnicians() {
        return service.getTechnicians();
    }
}
