package vn.edu.tlu.dms;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.service.IdentityAccountService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Danh tính ngoài không tự có quyền DMS; subject, role, trạng thái và phiên bản đều phải khớp. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class KeycloakAccountIntegrationTest {

    @Autowired private MockMvc mvc;

    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private JwtDecoder decoder;

    @MockitoBean private IdentityAccountService identities;

    private Long id;

    private String subject;

    private String username;

    @BeforeEach
    void createLinkedProfile() {
        subject = UUID.randomUUID().toString();
        username = "kc-" + UUID.randomUUID().toString().substring(0, 8);
        id =
                jdbc.queryForObject(
                        "INSERT INTO app_user(username,full_name,role,keycloak_user_id) VALUES"
                                + " (?,'Admin kiểm thử','ADMIN',?) RETURNING id",
                        Long.class,
                        username,
                        subject);
    }

    private RequestPostProcessor token(String identity, String role, long version) {
        return jwt().jwt(
                        builder ->
                                builder.subject(identity)
                                        .claim("preferred_username", username)
                                        .claim("realm_access", Map.of("roles", List.of(role)))
                                        .claim("dms_security_version", version))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void linkedSubjectReadsExistingDmsProfile() throws Exception {
        mvc.perform(get("/api/auth/me").with(token(subject, "ADMIN", 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").value(id))
                .andExpect(jsonPath("mustChangePassword").value(false));
    }

    @Test
    void sameUsernameWithDifferentSubjectCannotClaimProfile() throws Exception {
        mvc.perform(get("/api/auth/me").with(token(UUID.randomUUID().toString(), "ADMIN", 0)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("errorCode").value("IDENTITY_NOT_LINKED"));
    }

    @Test
    void lockedProfileBlocksAlreadyIssuedToken() throws Exception {
        jdbc.update("UPDATE app_user SET locked=true WHERE id=?", id);
        mvc.perform(get("/api/auth/me").with(token(subject, "ADMIN", 0)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("errorCode").value("ACCOUNT_LOCKED"));
    }

    @Test
    void changedSecurityVersionImmediatelyRevokesOldToken() throws Exception {
        jdbc.update("UPDATE app_user SET security_version=1 WHERE id=?", id);
        mvc.perform(get("/api/auth/me").with(token(subject, "ADMIN", 0)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("errorCode").value("SESSION_REVOKED"));
    }

    @Test
    void roleMismatchDoesNotGrantStoredRole() throws Exception {
        mvc.perform(get("/api/auth/me").with(token(subject, "STUDENT", 0)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousRequestIsRejected() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void dmsLogoutImmediatelyRejectsPreviouslyIssuedToken() throws Exception {
        mvc.perform(post("/api/auth/logout").with(token(subject, "ADMIN", 0)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").with(token(subject, "ADMIN", 0)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("errorCode").value("SESSION_REVOKED"));
    }
}
