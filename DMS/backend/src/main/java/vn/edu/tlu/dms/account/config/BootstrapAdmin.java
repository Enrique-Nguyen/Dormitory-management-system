package vn.edu.tlu.dms.account.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.domain.AppUser;
import vn.edu.tlu.dms.account.repository.AppUserRepository;

import java.time.OffsetDateTime;

@Component
@ConditionalOnProperty(name = "dms.bootstrap-admin.enabled", havingValue = "true")
public class BootstrapAdmin implements ApplicationRunner {

    private final AppUserRepository repository;

    private final PasswordEncoder encoder;

    private final String username;

    private final String password;

    public BootstrapAdmin(
            AppUserRepository repository,
            PasswordEncoder encoder,
            @Value("${dms.bootstrap-admin.username:admin}") String username,
            @Value("${dms.bootstrap-admin.password:}") String password) {
        this.repository = repository;
        this.encoder = encoder;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password.length() < 8
                || password.length() > 64
                || !password.matches(".*[A-Za-z].*")
                || !password.matches(".*[0-9].*")) {
            throw new IllegalStateException(
                    "Set DMS_BOOTSTRAP_ADMIN_PASSWORD to an 8–64 character password containing"
                            + " letters and digits");
        }
        if (repository.exists((r, q, b) -> b.equal(r.get("username"), username))) {
            return;
        }
        var u = new AppUser();
        u.setUsername(username);
        u.setFullName("Quản trị viên");
        u.setPasswordHash(encoder.encode(password));
        u.setRole("ADMIN");
        u.setMustChangePassword(true);
        u.setCrossUnitAccess(false);
        u.setLocked(false);
        u.setFailedLoginCount(0);
        u.setCreatedAt(OffsetDateTime.now());
        repository.saveAndFlush(u);
    }
}
