package vn.edu.tlu.dms.registration.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.registration.service.RegistrationPeriodService;
import vn.edu.tlu.dms.registration.service.SummerCrudService;

import java.util.Map;

@RestController
public class SummerCrudController {

    private final SummerCrudService service;

    private final RegistrationPeriodService periods;

    public SummerCrudController(SummerCrudService service, RegistrationPeriodService periods) {
        this.service = service;
        this.periods = periods;
    }

    @RequirePrivilege(Permission.REGISTRATION_PERIOD_READ)
    @GetMapping("/api/me/registration-periods/open")
    public Object openPeriods() {
        return periods.getOpenPeriodsForStudent();
    }

    @RequirePrivilege(Permission.SUMMER_REGISTRATION_READ)
    @GetMapping({"/api/summer-choices", "/api/me/summer-choices"})
    public Object searchChoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String q) {
        return service.searchChoices(page, size, q);
    }

    @RequirePrivilege(Permission.SUMMER_REGISTRATION_READ)
    @GetMapping("/api/summer-choices/{id}")
    public Object getChoice(@PathVariable Long id) {
        return service.getChoice(id);
    }

    @RequirePrivilege(Permission.SUMMER_REGISTRATION_CREATE)
    @PostMapping("/api/summer-choices")
    public Object createChoice(@RequestBody Map<String, Object> input) {
        return service.createChoice(input);
    }

    @RequirePrivilege(Permission.SUMMER_REGISTRATION_UPDATE)
    @PatchMapping("/api/summer-choices/{id}")
    public Object updateChoice(@PathVariable Long id, @RequestBody Map<String, Object> input) {
        return service.updateChoice(id, input);
    }

    @RequirePrivilege(Permission.SUMMER_REGISTRATION_DELETE)
    @DeleteMapping("/api/summer-choices/{id}")
    public ResponseEntity<Void> deleteChoice(@PathVariable Long id) {
        service.deleteChoice(id);
        return ResponseEntity.noContent().build();
    }
}
