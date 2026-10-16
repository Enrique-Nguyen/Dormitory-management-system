package vn.edu.tlu.dms.account.api;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.account.api.AccountDtos.CrossUnit;
import vn.edu.tlu.dms.account.api.AccountDtos.Profile;
import vn.edu.tlu.dms.account.api.AccountDtos.Reason;
import vn.edu.tlu.dms.account.api.AccountDtos.Save;
import vn.edu.tlu.dms.account.api.AccountDtos.TemporaryPassword;
import vn.edu.tlu.dms.account.service.UserService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.RequirePrivilege;

@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @RequirePrivilege(Permission.USER_READ)
    @PostMapping("/search-by-page")
    public PageResponse<Profile> searchByPage(@Valid @RequestBody SearchRequest input) {
        return service.searchByPage(input);
    }

    @RequirePrivilege(Permission.USER_READ)
    @GetMapping("/{id}")
    public Profile get(@PathVariable Long id) {
        return service.get(id);
    }

    @RequirePrivilege({Permission.USER_CREATE, Permission.USER_UPDATE})
    @PostMapping("/save-or-update")
    public Object saveOrUpdate(@Valid @RequestBody Save input) {
        return service.saveOrUpdate(input);
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
