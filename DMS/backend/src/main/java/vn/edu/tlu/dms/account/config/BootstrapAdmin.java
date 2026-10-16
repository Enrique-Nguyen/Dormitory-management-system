package vn.edu.tlu.dms.account.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import vn.edu.tlu.dms.account.service.IdentityAccountService;

/** Chỉ chạy khi người vận hành bật chuyển tài khoản; không reset tài khoản đã liên kết. */
@Component
@ConditionalOnProperty(name = "dms.keycloak-bootstrap.enabled", havingValue = "true")
public class BootstrapAdmin implements ApplicationRunner {

    private final IdentityAccountService migration;

    private final String username;

    private final String password;

    private final String credentialFile;

    public BootstrapAdmin(
            IdentityAccountService migration,
            @Value("${dms.keycloak-bootstrap.username:admin}") String username,
            @Value("${dms.keycloak-bootstrap.password:}") String password,
            @Value(
                            "${dms.keycloak-bootstrap.credential-file:./data/local-run/keycloak-accounts.local}")
                    String credentialFile) {
        this.migration = migration;
        this.username = username;
        this.password = password;
        this.credentialFile = credentialFile;
    }

    @Override
    public void run(ApplicationArguments args) {
        migration.migrateUnlinkedAccounts(username, password, credentialFile);
    }
}
