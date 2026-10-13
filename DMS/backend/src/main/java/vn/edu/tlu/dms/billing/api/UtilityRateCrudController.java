package vn.edu.tlu.dms.billing.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.billing.service.UtilityRateCrudService;

@RestController
public class UtilityRateCrudController {

    private final UtilityRateCrudService service;

    public UtilityRateCrudController(UtilityRateCrudService service) {
        this.service = service;
    }

    @PatchMapping("/api/utility-rates/{id}")
    public Object updateRate(
            @PathVariable Long id, @Valid @RequestBody UtilityRateDtos.Create input) {
        return service.updateRate(id, input);
    }

    @DeleteMapping("/api/utility-rates/{id}")
    public ResponseEntity<Void> deleteRate(@PathVariable Long id) {
        service.deleteRate(id);
        return ResponseEntity.noContent().build();
    }
}
