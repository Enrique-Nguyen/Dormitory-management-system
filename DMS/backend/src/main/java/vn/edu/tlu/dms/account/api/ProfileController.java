package vn.edu.tlu.dms.account.api;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.account.api.AccountDtos.Profile;
import vn.edu.tlu.dms.account.api.AccountDtos.ProfileInput;
import vn.edu.tlu.dms.account.service.UserService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.security.RequirePrivilege;

@RestController
@RequestMapping("/api/me/profile")
public class ProfileController {

    private final UserService service;

    public ProfileController(UserService service) {
        this.service = service;
    }

    @RequirePrivilege(Permission.PROFILE_READ)
    @GetMapping
    public Profile get() {
        return service.profile();
    }

    @RequirePrivilege(Permission.PROFILE_UPDATE)
    @PostMapping("/save-or-update")
    public Profile saveOrUpdate(@Valid @RequestBody ProfileInput input) {
        return service.saveOrUpdate(input);
    }
}
