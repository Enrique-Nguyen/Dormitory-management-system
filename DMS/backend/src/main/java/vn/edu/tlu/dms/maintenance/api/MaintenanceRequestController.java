package vn.edu.tlu.dms.maintenance.api;

import jakarta.validation.Valid;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.ReasonRequest;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Assign;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Create;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Note;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Response;
import vn.edu.tlu.dms.maintenance.service.MaintenanceRequestService;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/api/maintenance-requests")
public class MaintenanceRequestController {

    private final MaintenanceRequestService service;

    public MaintenanceRequestController(MaintenanceRequestService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<Response> searchMaintenanceRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "") String q) {
        return service.searchMaintenanceRequests(page, size, status, category, severity, q);
    }

    @GetMapping("/{id}")
    public Response getMaintenanceRequestById(@PathVariable Long id) {
        return service.getMaintenanceRequestById(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response> createMaintenanceRequest(
            @Valid @RequestPart("data") Create input,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
        var response = service.createMaintenanceRequest(input, photos);
        return ResponseEntity.created(URI.create("/api/maintenance-requests/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}/photos/{photoId}")
    public ResponseEntity<Resource> getMaintenancePhoto(
            @PathVariable Long id, @PathVariable Long photoId) throws IOException {
        var path = service.getAuthorizedPhotoFile(id, photoId);
        String type = Files.probeContentType(path);
        return ResponseEntity.ok()
                .header("X-Content-Type-Options", "nosniff")
                .contentType(
                        type == null
                                ? MediaType.APPLICATION_OCTET_STREAM
                                : MediaType.parseMediaType(type))
                .body(new FileSystemResource(path));
    }

    @PostMapping("/{id}/assign")
    public Response assignTechnician(@PathVariable Long id, @Valid @RequestBody Assign input) {
        return service.assignTechnician(id, input.technicianId());
    }

    @PostMapping("/{id}/reject")
    public Response rejectMaintenanceRequest(
            @PathVariable Long id, @Valid @RequestBody ReasonRequest input) {
        return service.performMaintenanceAction(id, "reject", input.reason());
    }

    @PostMapping("/{id}/cancel")
    public Response cancelMaintenanceRequest(@PathVariable Long id) {
        return service.performMaintenanceAction(id, "cancel", null);
    }

    @PostMapping("/{id}/start")
    public Response startMaintenanceWork(@PathVariable Long id) {
        return service.performMaintenanceAction(id, "start", null);
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<Response> addProgressNote(
            @PathVariable Long id, @Valid @RequestBody Note input) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.performMaintenanceAction(id, "add-note", input.note()));
    }

    @PostMapping("/{id}/resolve")
    public Response resolveMaintenanceRequest(
            @PathVariable Long id, @Valid @RequestBody Note input) {
        return service.performMaintenanceAction(id, "resolve", input.note());
    }

    @PostMapping("/{id}/accept")
    public Response acceptMaintenanceWork(
            @PathVariable Long id, @Valid @RequestBody(required = false) ReasonRequest input) {
        return service.performMaintenanceAction(
                id, "accept", input == null ? null : input.reason());
    }

    @PostMapping("/{id}/reopen")
    public Response reopenMaintenanceRequest(
            @PathVariable Long id, @Valid @RequestBody ReasonRequest input) {
        return service.performMaintenanceAction(id, "reopen", input.reason());
    }
}
