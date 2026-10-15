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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.ReasonRequest;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Assign;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Note;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Response;
import vn.edu.tlu.dms.maintenance.api.MaintenanceDtos.Save;
import vn.edu.tlu.dms.maintenance.service.MaintenanceRequestService;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/api/maintenance-requests")
public class MaintenanceRequestController {

    private final MaintenanceRequestService service;

    public MaintenanceRequestController(MaintenanceRequestService service) {
        this.service = service;
    }

    @RequirePrivilege(Permission.MAINTENANCE_READ)
    @PostMapping("/search-by-page")
    public PageResponse<Response> searchByPage(@Valid @RequestBody SearchRequest request) {
        return service.searchByPage(request);
    }

    @RequirePrivilege(Permission.MAINTENANCE_READ)
    @GetMapping("/{id}")
    public Response getMaintenanceRequestById(@PathVariable Long id) {
        return service.getMaintenanceRequestById(id);
    }

    @RequirePrivilege({Permission.MAINTENANCE_CREATE, Permission.MAINTENANCE_UPDATE})
    @PostMapping(value = "/save-or-update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response saveOrUpdate(
            @Valid @RequestPart("data") Save input,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
        return service.saveOrUpdate(input, photos);
    }

    @RequirePrivilege(Permission.MAINTENANCE_READ)
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

    @RequirePrivilege(Permission.MAINTENANCE_UPDATE)
    @PostMapping("/{id}/assign")
    public Response assignTechnician(@PathVariable Long id, @Valid @RequestBody Assign input) {
        return service.assignTechnician(id, input.technicianId());
    }

    @RequirePrivilege(Permission.MAINTENANCE_UPDATE)
    @PostMapping("/{id}/reject")
    public Response rejectMaintenanceRequest(
            @PathVariable Long id, @Valid @RequestBody ReasonRequest input) {
        return service.performMaintenanceAction(id, BusinessAction.REJECT, input.reason());
    }

    // Hủy yêu cầu dùng quyền xóa; Service vẫn kiểm tra trạng thái và người sở hữu.
    @RequirePrivilege(Permission.MAINTENANCE_DELETE)
    @PostMapping("/{id}/cancel")
    public Response cancelMaintenanceRequest(@PathVariable Long id) {
        return service.performMaintenanceAction(id, BusinessAction.CANCEL, null);
    }

    @RequirePrivilege(Permission.MAINTENANCE_UPDATE)
    @PostMapping("/{id}/start")
    public Response startMaintenanceWork(@PathVariable Long id) {
        return service.performMaintenanceAction(id, BusinessAction.START, null);
    }

    @RequirePrivilege(Permission.MAINTENANCE_UPDATE)
    @PostMapping("/{id}/notes")
    public ResponseEntity<Response> addProgressNote(
            @PathVariable Long id, @Valid @RequestBody Note input) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.performMaintenanceAction(id, BusinessAction.ADD_NOTE, input.note()));
    }

    @RequirePrivilege(Permission.MAINTENANCE_UPDATE)
    @PostMapping("/{id}/resolve")
    public Response resolveMaintenanceRequest(
            @PathVariable Long id, @Valid @RequestBody Note input) {
        return service.performMaintenanceAction(id, BusinessAction.RESOLVE, input.note());
    }

    @RequirePrivilege(Permission.MAINTENANCE_UPDATE)
    @PostMapping("/{id}/accept")
    public Response acceptMaintenanceWork(
            @PathVariable Long id, @Valid @RequestBody(required = false) ReasonRequest input) {
        return service.performMaintenanceAction(
                id, BusinessAction.ACCEPT, input == null ? null : input.reason());
    }

    @RequirePrivilege(Permission.MAINTENANCE_UPDATE)
    @PostMapping("/{id}/reopen")
    public Response reopenMaintenanceRequest(
            @PathVariable Long id, @Valid @RequestBody ReasonRequest input) {
        return service.performMaintenanceAction(id, BusinessAction.REOPEN, input.reason());
    }
}
