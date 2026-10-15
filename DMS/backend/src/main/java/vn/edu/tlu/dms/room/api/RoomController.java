package vn.edu.tlu.dms.room.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.ReasonRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.room.api.RoomDtos.BedInput;
import vn.edu.tlu.dms.room.api.RoomDtos.BedResponse;
import vn.edu.tlu.dms.room.api.RoomDtos.Input;
import vn.edu.tlu.dms.room.api.RoomDtos.Leader;
import vn.edu.tlu.dms.room.api.RoomDtos.Response;
import vn.edu.tlu.dms.room.api.RoomDtos.Status;
import vn.edu.tlu.dms.room.service.RoomService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService service;

    public RoomController(RoomService service) {
        this.service = service;
    }

    @RequirePrivilege(Permission.ROOM_READ)
    @GetMapping
    public PageResponse<Response> searchRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Short floor,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean hasFreeBed,
            @RequestParam(required = false) Boolean noLeader,
            @RequestParam(defaultValue = "") String q) {
        return service.searchRooms(
                page,
                size,
                sort,
                buildingId,
                floor,
                roomTypeId,
                gender,
                status,
                hasFreeBed,
                noLeader,
                q);
    }

    @RequirePrivilege(Permission.ROOM_READ)
    @GetMapping("/{id}")
    public Response getRoomById(@PathVariable Long id) {
        return service.getRoomById(id);
    }

    @RequirePrivilege(Permission.ROOM_CREATE)
    @PostMapping
    public ResponseEntity<Response> create(@Valid @RequestBody Input input) {
        var result = service.createRoom(input);
        return ResponseEntity.created(URI.create("/api/rooms/" + result.id())).body(result);
    }

    @RequirePrivilege(Permission.ROOM_UPDATE)
    @PatchMapping("/{id}")
    public Response updateRoom(@PathVariable Long id, @Valid @RequestBody Input input) {
        return service.updateRoom(id, input);
    }

    @RequirePrivilege(Permission.ROOM_UPDATE)
    @PostMapping("/{id}/status")
    public Response changeRoomStatus(@PathVariable Long id, @Valid @RequestBody Status input) {
        return service.changeRoomStatus(id, input);
    }

    @RequirePrivilege(Permission.BED_CREATE)
    @PostMapping("/{id}/beds")
    public ResponseEntity<BedResponse> bed(
            @PathVariable Long id, @Valid @RequestBody BedInput input) {
        var result = service.addBedToRoom(id, input);
        return ResponseEntity.created(URI.create("/api/beds/" + result.id())).body(result);
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
