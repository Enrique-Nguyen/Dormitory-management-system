package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import vn.edu.tlu.dms.account.api.AccountDtos;
import vn.edu.tlu.dms.account.config.SecurityConfig.KeycloakProperties;
import vn.edu.tlu.dms.account.service.StudentImportService;

import java.util.List;

class SensitiveValuesTest {

    @Test
    void diagnosticStringsHidePasswordsAndClientSecretIncludingNestedImportResult() {
        String secret = "Synthetic-secret-123";
        var account = new StudentImportService.CreatedAccount(1L, "9900260001", secret);
        var result =
                new StudentImportService.Result(false, 1, 1, 1, 0, 0, List.of(), List.of(account));
        for (Object sensitive :
                List.of(
                        account,
                        result,
                        new AccountDtos.TemporaryPassword(1L, secret),
                        new AccountDtos.Password(secret, secret),
                        new KeycloakProperties(
                                "http://localhost:8180", "dms", "dms-api", "dms-admin", secret))) {
            assertThat(sensitive.toString()).doesNotContain(secret).contains("REDACTED");
        }
    }

    @Test
    void redactionKeepsAuthorizedOneTimeJsonResponseContract() throws Exception {
        var mapper = new ObjectMapper();
        var temporary = new AccountDtos.TemporaryPassword(1L, "Synthetic-123");
        var imported = new StudentImportService.CreatedAccount(1L, "9900260001", "Synthetic-456");
        assertThat(
                        mapper.readTree(mapper.writeValueAsBytes(temporary))
                                .path("temporaryPassword")
                                .asText())
                .isEqualTo("Synthetic-123");
        assertThat(
                        mapper.readTree(mapper.writeValueAsBytes(imported))
                                .path("temporaryPassword")
                                .asText())
                .isEqualTo("Synthetic-456");
    }
}
