package vn.edu.tlu.dms.account.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.account.api.AccountDtos.Login;
import vn.edu.tlu.dms.account.api.AccountDtos.Me;
import vn.edu.tlu.dms.account.api.AccountDtos.Password;
import vn.edu.tlu.dms.account.service.AuthService;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    private final SecurityContextRepository contexts;

    public AuthController(AuthService service, SecurityContextRepository contexts) {
        this.service = service;
        this.contexts = contexts;
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public Me login(
            @Valid @RequestBody Login input,
            HttpServletRequest request,
            HttpServletResponse response) {
        var principal = service.authenticate(input.username().trim(), input.password());
        // Vai trò lấy từ CSDL; không chấp nhận vai trò do người đăng nhập tự gửi.
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
        SecurityContextHolder.setContext(context);
        var me = service.getCurrentUser();
        var authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + me.role())));
        new ChangeSessionIdAuthenticationStrategy()
                .onAuthentication(authentication, request, response);
        new CsrfAuthenticationStrategy(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .onAuthentication(authentication, request, response);
        context.setAuthentication(authentication);
        contexts.saveContext(context, request, response);
        return me;
    }

    @GetMapping("/me")
    public Me getCurrentUser() {
        return service.getCurrentUser();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> change(@Valid @RequestBody Password input) {
        service.changePassword(input.currentPassword(), input.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        CookieCsrfTokenRepository.withHttpOnlyFalse().saveToken(null, request, response);
        return ResponseEntity.noContent().build();
    }
}
