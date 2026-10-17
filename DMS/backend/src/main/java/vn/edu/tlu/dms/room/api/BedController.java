package vn.edu.tlu.dms.room.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.room.api.RoomDtos.AvailableRoom;
import vn.edu.tlu.dms.room.api.RoomDtos.BedInput;
import vn.edu.tlu.dms.room.api.RoomDtos.BedResponse;
import vn.edu.tlu.dms.room.api.RoomDtos.BedStatus;
import vn.edu.tlu.dms.room.service.RoomService;

import java.util.List;

@RestController
@RequestMapping("/api/beds")
public class BedController {

    private final RoomService service;

    public BedController(RoomService service) {
        this.service = service;
    }

    @RequirePrivilege(Permission.BED_READ)
    @GetMapping("/available")
    public List<AvailableRoom> findAvailableBeds(
            @RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) Long transferRequestId,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long roomTypeId) {
        return service.findAvailableBeds(applicationId, transferRequestId, buildingId, roomTypeId);
    }

    @RequirePrivilege({Permission.BED_CREATE, Permission.BED_UPDATE})
    @PostMapping("/save-or-update")
    public BedResponse saveOrUpdate(@Valid @RequestBody BedInput input) {
        return service.saveOrUpdateBed(input);
    }

    @RequirePrivilege(Permission.BED_UPDATE)
    @PostMapping("/{id}/status")
    public BedResponse changeBedStatus(@PathVariable Long id, @Valid @RequestBody BedStatus input) {
        return service.changeBedStatus(id, input);
    }

    @RequirePrivilege(Permission.BED_DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteBed(id);
        return ResponseEntity.noContent().build();
    }
}
