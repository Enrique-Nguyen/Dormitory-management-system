package vn.edu.tlu.dms.registration.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.ReasonRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.Input;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.Response;
import vn.edu.tlu.dms.registration.service.RoomApplicationService;

import java.net.URI;
import java.util.List;

@RestController
public class RoomApplicationController {

    private final RoomApplicationService service;

    public RoomApplicationController(RoomApplicationService service) {
        this.service = service;
    }

    @RequirePrivilege(Permission.ROOM_APPLICATION_READ)
    @GetMapping("/api/applications")
    public PageResponse<Response> searchApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort,
            @RequestParam(required = false) Long periodId,
            @RequestParam(required = false) List<String> status,
            @RequestParam(defaultValue = "") String q) {
        return service.searchApplications(page, size, sort, periodId, status, false, q);
    }

    @RequirePrivilege(Permission.ROOM_APPLICATION_READ)
    @GetMapping("/api/me/applications")
    public PageResponse<Response> getMyApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String q) {
        return service.searchApplications(page, size, null, null, null, true, q);
    }

    @RequirePrivilege(Permission.ROOM_APPLICATION_READ)
    @GetMapping("/api/applications/{id}")
    public Response getApplicationById(@PathVariable Long id) {
        return service.getApplicationById(id);
    }

    @RequirePrivilege(Permission.ROOM_APPLICATION_CREATE)
    @PostMapping("/api/applications")
    public ResponseEntity<Response> createApplication(@Valid @RequestBody Input input) {
        var response = service.createApplication(input);
        return ResponseEntity.created(URI.create("/api/applications/" + response.id()))
                .body(response);
    }

    @RequirePrivilege(Permission.ROOM_APPLICATION_UPDATE)
    @PatchMapping("/api/applications/{id}")
    public Response updateDraftApplication(@PathVariable Long id, @Valid @RequestBody Input input) {
        return service.updateDraftApplication(id, input);
    }

    @RequirePrivilege(Permission.ROOM_APPLICATION_UPDATE)
    @PostMapping("/api/applications/{id}/submit")
    public Response submitApplication(@PathVariable Long id) {
        return service.performApplicationAction(id, BusinessAction.SUBMIT, null);
    }

    @RequirePrivilege(Permission.ROOM_APPLICATION_UPDATE)
    @PostMapping("/api/applications/{id}/approve")
    public Response approveApplication(@PathVariable Long id) {
        return service.performApplicationAction(id, BusinessAction.APPROVE, null);
    }

    @RequirePrivilege(Permission.ROOM_APPLICATION_UPDATE)
    @PostMapping("/api/applications/{id}/reject")
    public Response rejectApplication(
            @PathVariable Long id, @Valid @RequestBody ReasonRequest input) {
        return service.performApplicationAction(id, BusinessAction.REJECT, input.reason());
    }

    @RequirePrivilege(Permission.ROOM_APPLICATION_DELETE)
    @PostMapping("/api/applications/{id}/cancel")
    public Response cancelApplication(@PathVariable Long id) {
        return service.performApplicationAction(id, BusinessAction.CANCEL, null);
    }
}
