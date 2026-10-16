package vn.edu.tlu.dms.account.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.account.api.AccountDtos.Me;
import vn.edu.tlu.dms.account.service.AuthService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.security.RequirePrivilege;

/** Đăng nhập/đăng xuất/đổi mật khẩu thuộc Keycloak; DMS trả hồ sơ và phạm vi nghiệp vụ. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @RequirePrivilege(Permission.PROFILE_READ)
    @GetMapping("/me")
    public Me getCurrentUser() {
        return service.getCurrentUser();
    }

    @RequirePrivilege(Permission.PROFILE_READ)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        service.revokeCurrentIdentitySessions();
        return ResponseEntity.noContent().build();
    }
}
