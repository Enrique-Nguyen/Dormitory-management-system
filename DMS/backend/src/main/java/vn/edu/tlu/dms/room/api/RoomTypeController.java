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

import vn.edu.tlu.dms.room.service.RoomTypeService;

import java.net.URI;
import java.util.List;

@RestController
public class RoomTypeController {

    private final RoomTypeService service;

    public RoomTypeController(RoomTypeService service) {
        this.service = service;
    }

    @GetMapping("/api/room-types")
    public List<RoomTypeResponse> getRoomTypeList() {
        return service.getAll();
    }

    @GetMapping("/api/room-types/{id}")
    public RoomTypeResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping("/api/room-types")
    public ResponseEntity<RoomTypeResponse> createRoomType(
            @Valid @RequestBody RoomTypeRequest input) {
        var result = service.create(input);
        return ResponseEntity.created(URI.create("/api/room-types/" + result.id())).body(result);
    }

    @PatchMapping("/api/room-types/{id}")
    public RoomTypeResponse updateRoomType(
            @PathVariable Long id, @Valid @RequestBody RoomTypeRequest input) {
        return service.update(id, input);
    }

    @DeleteMapping("/api/room-types/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
