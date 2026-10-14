package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.account.service.IdentityAccountService;
import vn.edu.tlu.dms.common.security.RequirePrivilege;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Danh tính ngoài không tự có quyền DMS; subject, role, trạng thái và phiên bản đều phải khớp. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({
    TestcontainersConfiguration.class,
    KeycloakAccountIntegrationTest.PrivilegeProbeConfiguration.class
})
@Transactional
class KeycloakAccountIntegrationTest {

    @Autowired private MockMvc mvc;

    @Autowired private JdbcTemplate jdbc;

    @Autowired private PrivilegeProbeController privilegeProbe;

    @Autowired private ClassPrivilegeProbeController classPrivilegeProbe;

    @MockitoBean private JwtDecoder decoder;

    @MockitoBean private IdentityAccountService identities;

    private Long id;

    private String subject;

    private String username;

    @BeforeEach
    void createLinkedProfile() {
        privilegeProbe.resetInvocations();
        classPrivilegeProbe.resetInvocations();
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

    @Test
    void changingStaffRoleRevokesPreviouslyIssuedTokenAndGrantsTechnicianPermissions()
            throws Exception {
        Long unitId =
                jdbc.queryForObject("SELECT id FROM management_unit WHERE code='K1'", Long.class);
        Long targetId = createLinkedUser("STAFF", unitId, true);
        String targetSubject =
                jdbc.queryForObject(
                        "SELECT keycloak_user_id FROM app_user WHERE id=?", String.class, targetId);
        // Chụp token cũ trước khi đổi vai trò; không dựng lại từ security_version mới.
        RequestPostProcessor previousStaffToken = token(targetSubject, "STAFF", 0);
        mvc.perform(get("/api/rooms").with(previousStaffToken)).andExpect(status().isOk());
        mvc.perform(
                        patch("/api/admin/users/" + targetId)
                                .with(token(subject, "ADMIN", 0))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"TECHNICIAN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TECHNICIAN"))
                .andExpect(jsonPath("$.crossUnitAccess").value(false));
        mvc.perform(get("/api/auth/me").with(previousStaffToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SESSION_REVOKED"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT security_version FROM app_user WHERE id=?",
                                Long.class,
                                targetId))
                .isEqualTo(1);
        mvc.perform(get("/api/auth/me").with(token(targetSubject, "TECHNICIAN", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TECHNICIAN"))
                .andExpect(jsonPath("$.permissions").value(hasItem("MAINTENANCE_UPDATE")))
                .andExpect(jsonPath("$.permissions").value(not(hasItem("ROOM_CREATE"))));
        mvc.perform(get("/api/rooms").with(token(targetSubject, "TECHNICIAN", 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void promotingStaffToAdminClearsUnitAndAggregateFlagAndRevokesOldToken() throws Exception {
        Long unitId =
                jdbc.queryForObject("SELECT id FROM management_unit WHERE code='K1'", Long.class);
        Long targetId = createLinkedUser("STAFF", unitId, true);
        String targetSubject =
                jdbc.queryForObject(
                        "SELECT keycloak_user_id FROM app_user WHERE id=?", String.class, targetId);
        RequestPostProcessor previousStaffToken = token(targetSubject, "STAFF", 0);
        mvc.perform(
                        patch("/api/admin/users/" + targetId)
                                .with(token(subject, "ADMIN", 0))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.managementUnitId").doesNotExist())
                .andExpect(jsonPath("$.crossUnitAccess").value(false));
        mvc.perform(get("/api/auth/me").with(previousStaffToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SESSION_REVOKED"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT security_version FROM app_user WHERE id=?",
                                Long.class,
                                targetId))
                .isEqualTo(1);
        mvc.perform(get("/api/auth/me").with(token(targetSubject, "ADMIN", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.managementUnit").doesNotExist())
                .andExpect(jsonPath("$.permissions").value(hasItem("USER_CREATE")))
                .andExpect(jsonPath("$.permissions").value(hasItem("ROOM_CREATE")));
        mvc.perform(get("/api/rooms").with(token(targetSubject, "ADMIN", 1)))
                .andExpect(status().isOk());
    }

    @Test
    void adminCannotDemoteOwnActiveAccount() throws Exception {
        Long unitId =
                jdbc.queryForObject("SELECT id FROM management_unit WHERE code='K1'", Long.class);
        mvc.perform(
                        patch("/api/admin/users/" + id)
                                .with(token(subject, "ADMIN", 0))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"role\":\"TECHNICIAN\",\"managementUnitId\":"
                                                + unitId
                                                + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT security_version FROM app_user WHERE id=?", Long.class, id))
                .isZero();
        mvc.perform(get("/api/auth/me").with(token(subject, "ADMIN", 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.permissions").value(hasItem("USER_CREATE")));
    }

    /** Controller không có guard trong thân hàm: thiếu quyền phải bị chặn trước khi chạy hàm. */
    @Test
    void privilegeAnnotationBlocksControllerBeforeItsBodyRuns() throws Exception {
        mvc.perform(get("/api/privilege-test/admin-only").with(useRole("TECHNICIAN")))
                .andExpect(status().isForbidden());
        assertThat(privilegeProbe.invocationCount()).isZero();

        mvc.perform(get("/api/privilege-test/admin-only").with(useRole("ADMIN")))
                .andExpect(status().isOk());
        assertThat(privilegeProbe.invocationCount()).isEqualTo(1);
    }

    /** Giống V5: có một trong các quyền là đủ, kể cả quyền thứ hai trong mảng. */
    @Test
    void privilegeAnnotationAllowsAnyListedPermission() throws Exception {
        mvc.perform(get("/api/privilege-test/any-permission").with(useRole("TECHNICIAN")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/privilege-test/any-permission").with(useRole("STAFF")))
                .andExpect(status().isOk());
        assertThat(privilegeProbe.invocationCount()).isEqualTo(2);
    }

    @Test
    void privilegeAnnotationRejectsRoleWithoutAnyListedPermission() throws Exception {
        mvc.perform(get("/api/privilege-test/room-admin-only").with(useRole("TECHNICIAN")))
                .andExpect(status().isForbidden());
        assertThat(privilegeProbe.invocationCount()).isZero();
    }

    /** Admin có mọi quyền đã khai báo, nhưng không bỏ qua mã quyền viết sai hoặc chưa đăng ký. */
    @Test
    void unknownPrivilegeFailsClosedEvenForAdmin() throws Exception {
        mvc.perform(get("/api/privilege-test/unknown").with(token(subject, "ADMIN", 0)))
                .andExpect(status().isForbidden());
        assertThat(privilegeProbe.invocationCount()).isZero();
    }

    @Test
    void emptyPrivilegeAnnotationFailsClosed() throws Exception {
        mvc.perform(get("/api/privilege-test/empty").with(token(subject, "ADMIN", 0)))
                .andExpect(status().isForbidden());
        assertThat(privilegeProbe.invocationCount()).isZero();
    }

    @Test
    void apiWithoutPrivilegeAnnotationFailsClosed() throws Exception {
        mvc.perform(get("/api/privilege-test/missing").with(token(subject, "ADMIN", 0)))
                .andExpect(status().isForbidden());
        assertThat(privilegeProbe.invocationCount()).isZero();
    }

    @Test
    void anonymousRequestCannotReachPrivilegeProtectedController() throws Exception {
        mvc.perform(get("/api/privilege-test/any-permission")).andExpect(status().isUnauthorized());
        assertThat(privilegeProbe.invocationCount()).isZero();
    }

    /** Quyền ở hàm được ưu tiên; quyền ở lớp chỉ áp dụng khi hàm không khai riêng. */
    @Test
    void methodPrivilegeOverridesClassPrivilegeInsteadOfUsingItsFallback() throws Exception {
        mvc.perform(get("/api/privilege-test/class-fallback").with(useRole("TECHNICIAN")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/privilege-test/method-override").with(useRole("TECHNICIAN")))
                .andExpect(status().isForbidden());
        assertThat(classPrivilegeProbe.invocationCount()).isEqualTo(1);
    }

    private RequestPostProcessor useRole(String role) {
        Long unitId = null;
        if ("STAFF".equals(role) || "TECHNICIAN".equals(role)) {
            unitId =
                    jdbc.queryForObject(
                            "SELECT id FROM management_unit WHERE code='K1'", Long.class);
        }
        jdbc.update("UPDATE app_user SET role=?,management_unit_id=? WHERE id=?", role, unitId, id);
        return token(subject, role, 0);
    }

    private Long createLinkedUser(String role, Long unitId, boolean crossUnitAccess) {
        return jdbc.queryForObject(
                "INSERT INTO"
                    + " app_user(username,full_name,role,management_unit_id,cross_unit_access,keycloak_user_id,must_change_password)"
                    + " VALUES(?,?,?,?,?,?,false) RETURNING id",
                Long.class,
                "role-" + UUID.randomUUID().toString().substring(0, 8),
                "Tài khoản đổi quyền",
                role,
                unitId,
                crossUnitAccess,
                UUID.randomUUID().toString());
    }

    /** Probe chỉ có trong context test này, không thêm API thử nghiệm vào context CRUD khác. */
    @TestConfiguration(proxyBeanMethods = false)
    static class PrivilegeProbeConfiguration {
        @Bean
        PrivilegeProbeController privilegeProbeController() {
            return new PrivilegeProbeController();
        }

        @Bean
        ClassPrivilegeProbeController classPrivilegeProbeController() {
            return new ClassPrivilegeProbeController();
        }
    }

    @TestComponent
    @RestController
    public static class PrivilegeProbeController {
        private final AtomicInteger invocations = new AtomicInteger();

        public int invocationCount() {
            return invocations.get();
        }

        public void resetInvocations() {
            invocations.set(0);
        }

        @RequirePrivilege("USER_CREATE")
        @GetMapping("/api/privilege-test/admin-only")
        public int adminOnly() {
            return invocations.incrementAndGet();
        }

        @RequirePrivilege({"USER_CREATE", "MAINTENANCE_UPDATE"})
        @GetMapping("/api/privilege-test/any-permission")
        public int anyPermission() {
            return invocations.incrementAndGet();
        }

        @RequirePrivilege({"USER_CREATE", "ROOM_CREATE"})
        @GetMapping("/api/privilege-test/room-admin-only")
        public int roomOrAdminPermission() {
            return invocations.incrementAndGet();
        }

        @RequirePrivilege("UNREGISTERED_PRIVILEGE")
        @GetMapping("/api/privilege-test/unknown")
        public int unknownPermission() {
            return invocations.incrementAndGet();
        }

        @RequirePrivilege({})
        @GetMapping("/api/privilege-test/empty")
        public int emptyPermissions() {
            return invocations.incrementAndGet();
        }

        @GetMapping("/api/privilege-test/missing")
        public int missingAnnotation() {
            return invocations.incrementAndGet();
        }
    }

    @TestComponent
    @RestController
    @RequirePrivilege("PROFILE_READ")
    public static class ClassPrivilegeProbeController {
        private final AtomicInteger invocations = new AtomicInteger();

        public int invocationCount() {
            return invocations.get();
        }

        public void resetInvocations() {
            invocations.set(0);
        }

        @GetMapping("/api/privilege-test/class-fallback")
        public int classFallback() {
            return invocations.incrementAndGet();
        }

        @RequirePrivilege("USER_CREATE")
        @GetMapping("/api/privilege-test/method-override")
        public int methodOverride() {
            return invocations.incrementAndGet();
        }
    }
}
