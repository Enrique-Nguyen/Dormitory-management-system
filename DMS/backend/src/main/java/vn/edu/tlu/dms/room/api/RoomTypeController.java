package vn.edu.tlu.dms.room.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.room.service.RoomTypeService;

@RestController
public class RoomTypeController {

    private final RoomTypeService service;

    public RoomTypeController(RoomTypeService service) {
        this.service = service;
    }

    // Danh mục dùng chung cho form; chỉ cần một quyền trong danh sách (OR).
    @RequirePrivilege({Permission.ROOM_TYPE_READ, Permission.ROOM_CREATE, Permission.ROOM_UPDATE})
    @PostMapping("/api/room-types/search-by-page")
    public PageResponse<RoomTypeResponse> searchByPage(@Valid @RequestBody SearchRequest search) {
        return service.searchByPage(search);
    }

    @RequirePrivilege(Permission.ROOM_TYPE_READ)
    @GetMapping("/api/room-types/{id}")
    public RoomTypeResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @RequirePrivilege({Permission.ROOM_TYPE_CREATE, Permission.ROOM_TYPE_UPDATE})
    @PostMapping("/api/room-types/save-or-update")
    public RoomTypeResponse saveOrUpdate(@Valid @RequestBody RoomTypeRequest input) {
        return service.saveOrUpdate(input);
    }

    @RequirePrivilege(Permission.ROOM_TYPE_DELETE)
    @DeleteMapping("/api/room-types/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
