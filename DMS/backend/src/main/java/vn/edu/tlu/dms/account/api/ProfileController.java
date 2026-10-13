package vn.edu.tlu.dms.account.api;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.account.api.AccountDtos.Profile;
import vn.edu.tlu.dms.account.api.AccountDtos.ProfileInput;
import vn.edu.tlu.dms.account.service.UserService;

@RestController
@RequestMapping("/api/me/profile")
public class ProfileController {

    private final UserService service;

    public ProfileController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public Profile get() {
        return service.profile();
    }

    @PatchMapping
    public Profile update(@Valid @RequestBody ProfileInput input) {
        return service.updateProfile(input);
    }
}
