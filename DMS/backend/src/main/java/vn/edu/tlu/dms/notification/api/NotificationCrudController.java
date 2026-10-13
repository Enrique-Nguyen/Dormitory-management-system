package vn.edu.tlu.dms.notification.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.notification.service.NotificationCrudService;

@RestController
public class NotificationCrudController {

    private final NotificationCrudService service;

    public NotificationCrudController(NotificationCrudService service) {
        this.service = service;
    }

    @GetMapping("/api/notifications/published")
    public Object searchPublished(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String q) {
        return service.searchPublished(page, size, q);
    }

    @GetMapping("/api/notifications/published/{id}")
    public Object getPublished(@PathVariable Long id) {
        return service.getPublished(id);
    }

    @PatchMapping("/api/notifications/{id}")
    public Object updatePublished(
            @PathVariable Long id, @Valid @RequestBody NotificationDtos.Create input) {
        return service.updatePublished(id, input);
    }

    @DeleteMapping("/api/notifications/{id}")
    public ResponseEntity<Void> deletePublished(@PathVariable Long id) {
        service.deletePublished(id);
        return ResponseEntity.noContent().build();
    }
}
