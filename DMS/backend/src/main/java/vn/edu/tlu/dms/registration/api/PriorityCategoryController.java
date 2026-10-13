package vn.edu.tlu.dms.registration.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.registration.service.PriorityCategoryService;

import java.net.URI;
import java.util.List;

@RestController
public class PriorityCategoryController {

    private final PriorityCategoryService service;

    public PriorityCategoryController(PriorityCategoryService service) {
        this.service = service;
    }

    @GetMapping("/api/priority-categories")
    public List<PriorityCategoryResponse> getPriorityCategoryList() {
        return service.getAll();
    }

    @GetMapping("/api/priority-categories/{id}")
    public PriorityCategoryResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping("/api/admin/priority-categories")
    public ResponseEntity<PriorityCategoryResponse> createPriorityCategory(
            @Valid @RequestBody PriorityCategoryRequest input) {
        var result = service.create(input);
        return ResponseEntity.created(URI.create("/api/priority-categories/" + result.id()))
                .body(result);
    }

    @PatchMapping("/api/admin/priority-categories/{id}")
    public PriorityCategoryResponse updatePriorityCategory(
            @PathVariable Long id, @Valid @RequestBody PriorityCategoryRequest input) {
        return service.update(id, input);
    }

    @DeleteMapping("/api/admin/priority-categories/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
