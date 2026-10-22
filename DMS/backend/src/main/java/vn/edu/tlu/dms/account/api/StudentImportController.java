package vn.edu.tlu.dms.account.api;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import vn.edu.tlu.dms.account.service.StudentImportService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.StudentImport;
import vn.edu.tlu.dms.common.security.RequirePrivilege;

@RestController
public class StudentImportController {

    private final StudentImportService service;

    public StudentImportController(StudentImportService service) {
        this.service = service;
    }

    @RequirePrivilege(Permission.STUDENT_CREATE)
    @PostMapping(
            value = "/api/admin/student-imports",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentImportService.Result> importStudents(
            @RequestPart MultipartFile file, @RequestParam(defaultValue = "true") boolean dryRun) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(service.importStudents(file, dryRun));
    }

    @RequirePrivilege(Permission.STUDENT_CREATE)
    @GetMapping("/api/admin/student-imports/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(StudentImport.FILE_NAME)
                                .build()
                                .toString())
                .body(service.downloadTemplate());
    }
}
