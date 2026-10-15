package vn.edu.tlu.dms.room.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.room.service.RoomTypeService;

import java.net.URI;
import java.util.List;

@RestController
public class RoomTypeController {

    private final RoomTypeService service;

    public RoomTypeController(RoomTypeService service) {
        this.service = service;
    }

    // Danh mục dùng chung cho form; chỉ cần một quyền trong danh sách (OR).
    @RequirePrivilege({Permission.ROOM_TYPE_READ, Permission.ROOM_CREATE, Permission.ROOM_UPDATE})
    @GetMapping("/api/room-types")
    public List<RoomTypeResponse> getRoomTypeList() {
        return service.getAll();
    }

    @RequirePrivilege(Permission.ROOM_TYPE_READ)
    @GetMapping("/api/room-types/{id}")
    public RoomTypeResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @RequirePrivilege(Permission.ROOM_TYPE_CREATE)
    @PostMapping("/api/room-types")
    public ResponseEntity<RoomTypeResponse> createRoomType(
            @Valid @RequestBody RoomTypeRequest input) {
        var result = service.create(input);
        return ResponseEntity.created(URI.create("/api/room-types/" + result.id())).body(result);
    }

    @RequirePrivilege(Permission.ROOM_TYPE_UPDATE)
    @PatchMapping("/api/room-types/{id}")
    public RoomTypeResponse updateRoomType(
            @PathVariable Long id, @Valid @RequestBody RoomTypeRequest input) {
        return service.update(id, input);
    }

    @RequirePrivilege(Permission.ROOM_TYPE_DELETE)
    @DeleteMapping("/api/room-types/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
