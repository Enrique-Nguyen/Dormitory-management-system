package vn.edu.tlu.dms.account.api;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.account.api.AccountDtos.Preference;
import vn.edu.tlu.dms.account.api.AccountDtos.PreferenceInput;
import vn.edu.tlu.dms.account.service.UserPreferenceService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.security.RequirePrivilege;

@RestController
@RequestMapping("/api/me/preferences")
public class UserPreferenceController {

    private final UserPreferenceService service;

    public UserPreferenceController(UserPreferenceService service) {
        this.service = service;
    }

    @RequirePrivilege(Permission.PROFILE_READ)
    @GetMapping
    public Preference get() {
        return service.preferences();
    }

    @RequirePrivilege(Permission.PROFILE_UPDATE)
    @PostMapping("/save-or-update")
    public Preference saveOrUpdate(@Valid @RequestBody PreferenceInput input) {
        return service.saveOrUpdate(input);
    }
}
