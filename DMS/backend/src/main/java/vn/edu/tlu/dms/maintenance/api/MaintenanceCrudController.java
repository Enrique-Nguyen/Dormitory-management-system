package vn.edu.tlu.dms.maintenance.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.maintenance.service.MaintenanceCrudService;

@RestController
public class MaintenanceCrudController {

    private final MaintenanceCrudService service;

    public MaintenanceCrudController(MaintenanceCrudService service) {
        this.service = service;
    }

    public record Edit(
            @NotBlank @Pattern(regexp = "ELECTRIC|WATER|FURNITURE|DOOR_LOCK|OTHER") String category,
            @NotBlank @Pattern(regexp = "LOW|MEDIUM|HIGH") String severity,
            @NotBlank @Size(max = 1000) String description) {}

    @PatchMapping("/api/maintenance-requests/{id}")
    public Object updateRequest(@PathVariable Long id, @Valid @RequestBody Edit input) {
        return service.updateOpenRequest(id, input);
    }

    @DeleteMapping("/api/maintenance-requests/{id}")
    public ResponseEntity<Void> cancelRequest(@PathVariable Long id) {
        service.cancelOpenRequest(id);
        return ResponseEntity.noContent().build();
    }
}
