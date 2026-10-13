package vn.edu.tlu.dms.room.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.room.api.RoomDtos.BedResponse;
import vn.edu.tlu.dms.room.api.RoomDtos.BedStatus;
import vn.edu.tlu.dms.room.service.RoomService;

@RestController
@RequestMapping("/api/beds")
public class BedController {

    private final RoomService service;

    public BedController(RoomService service) {
        this.service = service;
    }

    @PostMapping("/{id}/status")
    public BedResponse changeRoomStatus(
            @PathVariable Long id, @Valid @RequestBody BedStatus input) {
        return service.changeBedStatus(id, input);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteBed(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public BedResponse updateBedCode(
            @PathVariable Long id, @Valid @RequestBody RoomDtos.BedInput input) {
        return service.updateBedCode(id, input);
    }
}
