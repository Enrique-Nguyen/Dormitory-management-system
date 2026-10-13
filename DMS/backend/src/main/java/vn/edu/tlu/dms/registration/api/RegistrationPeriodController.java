package vn.edu.tlu.dms.registration.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.registration.api.RegistrationPeriodDtos.Input;
import vn.edu.tlu.dms.registration.api.RegistrationPeriodDtos.Response;
import vn.edu.tlu.dms.registration.service.RegistrationPeriodService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/registration-periods")
public class RegistrationPeriodController {

    private final RegistrationPeriodService service;

    public RegistrationPeriodController(RegistrationPeriodService service) {
        this.service = service;
    }

    @GetMapping
    public List<Response> getRegistrationPeriods(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) Boolean open) {
        return service.getRegistrationPeriods(semesterId, kind, open);
    }

    @GetMapping("/{id}")
    public Response getRegistrationPeriodById(@PathVariable Long id) {
        return service.getRegistrationPeriodById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUnusedPeriod(@PathVariable Long id) {
        service.deleteUnusedPeriod(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<Response> createRegistrationPeriod(@Valid @RequestBody Input input) {
        var response = service.createRegistrationPeriod(input);
        return ResponseEntity.created(URI.create("/api/registration-periods/" + response.id()))
                .body(response);
    }

    @PatchMapping("/{id}")
    public Response updateRegistrationPeriod(
            @PathVariable Long id, @Valid @RequestBody Input input) {
        return service.updateRegistrationPeriod(id, input);
    }
}
