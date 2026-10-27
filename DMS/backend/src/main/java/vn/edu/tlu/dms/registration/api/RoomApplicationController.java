package vn.edu.tlu.dms.registration.api;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.ReasonRequest;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.AssignRequest;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.Input;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.PriorityVerifyRequest;
import vn.edu.tlu.dms.registration.api.ApplicationDtos.Response;
import vn.edu.tlu.dms.registration.service.RoomApplicationService;

@RestController
public class RoomApplicationController {

    private final RoomApplicationService service;

    public RoomApplicationController(RoomApplicationService service) {
        this.service = service;
    }

    @RequirePrivilege(Permission.ROOM_APPLICATION_READ)
    @PostMapping("/api/applications/search-by-page")
    public PageResponse<Response> searchByPage(@Valid @RequestBody SearchRequest search) {
        return service.searchByPage(search, false);
    }

    @RequirePrivilege(Permission.ROOM_APPLICATION_READ)
    @PostMapping("/api/me/applications/search-by-page")
    public PageResponse<Response> searchMyApplicationsByPage(
            @Valid @RequestBody SearchRequest search) {
        // Phạm vi cá nhân do server chỉ định; body không được nhận mine hoặc studentId.
        return service.searchByPage(search, true);
    }

    @RequirePrivilege(Permission.ROOM_APPLICATION_READ)
    @GetMapping("/api/applications/{id}")
    public Response getApplicationById(@PathVariable Long id) {
        return service.getApplicationById(id);
    }

    @RequirePrivilege({Permission.ROOM_APPLICATION_CREATE, Permission.ROOM_APPLICATION_UPDATE})
    @PostMapping("/api/applications/save-or-update")
    public Response saveOrUpdate(@Valid @RequestBody Input input) {
        return service.saveOrUpdate(input);
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

    @RequirePrivilege(Permission.ROOM_APPLICATION_UPDATE)
    @PostMapping("/api/applications/{id}/assign")
    public Response assignApplication(
            @PathVariable Long id, @Valid @RequestBody AssignRequest input) {
        return service.assignBed(id, input.bedId());
    }

    @RequirePrivilege(Permission.ROOM_APPLICATION_UPDATE)
    @PostMapping("/api/applications/{id}/release-hold")
    public Response releaseHold(
            @PathVariable Long id, @Valid @RequestBody ReasonRequest input) {
        return service.releaseHold(id, input.reason());
    }

    @RequirePrivilege(Permission.ROOM_APPLICATION_UPDATE)
    @PostMapping("/api/applications/{id}/priorities/{categoryId}/verify")
    public Response verifyPriority(
            @PathVariable Long id,
            @PathVariable Long categoryId,
            @Valid @RequestBody PriorityVerifyRequest input) {
        return service.verifyPriority(id, categoryId, input.verified(), input.note());
    }
}
