package vn.edu.tlu.dms.account.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.account.api.AccountDtos.Create;
import vn.edu.tlu.dms.account.api.AccountDtos.CrossUnit;
import vn.edu.tlu.dms.account.api.AccountDtos.Profile;
import vn.edu.tlu.dms.account.api.AccountDtos.Reason;
import vn.edu.tlu.dms.account.api.AccountDtos.TemporaryPassword;
import vn.edu.tlu.dms.account.api.AccountDtos.Update;
import vn.edu.tlu.dms.account.service.UserService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.security.RequirePrivilege;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @RequirePrivilege(Permission.USER_READ)
    @GetMapping
    public PageResponse<Profile> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role) {
        return service.list(page, size, sort, q, role);
    }

    @RequirePrivilege(Permission.USER_READ)
    @GetMapping("/{id}")
    public Profile get(@PathVariable Long id) {
        return service.get(id);
    }

    @RequirePrivilege(Permission.USER_CREATE)
    @PostMapping
    public ResponseEntity<TemporaryPassword> create(@Valid @RequestBody Create input) {
        var result = service.create(input);
        return ResponseEntity.created(URI.create("/api/admin/users/" + result.userId()))
                .body(result);
    }

    @RequirePrivilege(Permission.USER_UPDATE)
    @PatchMapping("/{id}")
    public Profile update(@PathVariable Long id, @Valid @RequestBody Update input) {
        return service.update(id, input);
    }

    @RequirePrivilege(Permission.USER_LOCK)
    @PostMapping("/{id}/lock")
    public ResponseEntity<Void> lock(@PathVariable Long id, @Valid @RequestBody Reason input) {
        service.lock(id, input.reason());
        return ResponseEntity.noContent().build();
    }

    @RequirePrivilege(Permission.USER_UNLOCK)
    @PostMapping("/{id}/unlock")
    public ResponseEntity<Void> unlock(@PathVariable Long id) {
        service.unlock(id);
        return ResponseEntity.noContent().build();
    }

    @RequirePrivilege(Permission.USER_RESET_PASSWORD)
    @PostMapping("/{id}/reset-password")
    public TemporaryPassword reset(@PathVariable Long id) {
        return service.resetPassword(id);
    }

    @RequirePrivilege(Permission.USER_CROSS_UNIT_ACCESS)
    @PutMapping("/{id}/cross-unit-access")
    public ResponseEntity<Void> cross(@PathVariable Long id, @Valid @RequestBody CrossUnit input) {
        service.crossUnitAccess(id, input.enabled());
        return ResponseEntity.noContent().build();
    }
}
