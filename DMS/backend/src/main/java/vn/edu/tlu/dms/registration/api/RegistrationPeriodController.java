package vn.edu.tlu.dms.registration.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.registration.api.RegistrationPeriodDtos.Input;
import vn.edu.tlu.dms.registration.api.RegistrationPeriodDtos.Response;
import vn.edu.tlu.dms.registration.service.RegistrationPeriodService;

@RestController
@RequestMapping("/api/registration-periods")
public class RegistrationPeriodController {

    private final RegistrationPeriodService service;

    public RegistrationPeriodController(RegistrationPeriodService service) {
        this.service = service;
    }

    @RequirePrivilege(Permission.REGISTRATION_PERIOD_READ)
    @PostMapping("/search-by-page")
    public PageResponse<Response> searchByPage(@Valid @RequestBody SearchRequest search) {
        return service.searchByPage(search, false);
    }

    @RequirePrivilege(Permission.REGISTRATION_PERIOD_READ)
    @GetMapping("/{id}")
    public Response getRegistrationPeriodById(@PathVariable Long id) {
        return service.getRegistrationPeriodById(id);
    }

    @RequirePrivilege(Permission.REGISTRATION_PERIOD_DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUnusedPeriod(@PathVariable Long id) {
        service.deleteUnusedPeriod(id);
        return ResponseEntity.noContent().build();
    }

    @RequirePrivilege({
        Permission.REGISTRATION_PERIOD_CREATE,
        Permission.REGISTRATION_PERIOD_UPDATE
    })
    @PostMapping("/save-or-update")
    public Response saveOrUpdate(@Valid @RequestBody Input input) {
        return service.saveOrUpdate(input);
    }
}
