package vn.edu.tlu.dms.room.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.ReasonRequest;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.room.api.RoomDtos.Input;
import vn.edu.tlu.dms.room.api.RoomDtos.Leader;
import vn.edu.tlu.dms.room.api.RoomDtos.Response;
import vn.edu.tlu.dms.room.api.RoomDtos.Status;
import vn.edu.tlu.dms.room.service.RoomService;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService service;

    public RoomController(RoomService service) {
        this.service = service;
    }

    @RequirePrivilege(Permission.ROOM_READ)
    @PostMapping("/search-by-page")
    public PageResponse<Response> searchByPage(@Valid @RequestBody SearchRequest request) {
        return service.searchByPage(request);
    }

    @RequirePrivilege(Permission.ROOM_READ)
    @GetMapping("/{id}")
    public Response getRoomById(@PathVariable Long id) {
        return service.getRoomById(id);
    }

    @RequirePrivilege({Permission.ROOM_CREATE, Permission.ROOM_UPDATE})
    @PostMapping("/save-or-update")
    public Response saveOrUpdate(@Valid @RequestBody Input input) {
        return service.saveOrUpdate(input);
    }

    @RequirePrivilege(Permission.ROOM_UPDATE)
    @PostMapping("/{id}/status")
    public Response changeRoomStatus(@PathVariable Long id, @Valid @RequestBody Status input) {
        return service.changeRoomStatus(id, input);
    }

    @RequirePrivilege(Permission.ROOM_DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @RequirePrivilege(Permission.ROOM_UPDATE)
    @PutMapping("/{id}/leader")
    public Response assignRoomLeader(@PathVariable Long id, @Valid @RequestBody Leader input) {
        return service.assignRoomLeader(id, input.studentId());
    }

    @RequirePrivilege(Permission.ROOM_UPDATE)
    @DeleteMapping("/{id}/leader")
    public ResponseEntity<Void> removeLeader(
            @PathVariable Long id, @Valid @RequestBody ReasonRequest input) {
        service.removeRoomLeader(id, input.reason());
        return ResponseEntity.noContent().build();
    }
}
