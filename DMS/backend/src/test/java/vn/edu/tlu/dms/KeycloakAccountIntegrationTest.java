package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.persistence.EntityManager;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.account.service.IdentityAccountService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.common.security.ScopeGuard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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

    @Autowired private ObjectMapper mapper;

    @Autowired private EntityManager entityManager;

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
        mvc.perform(searchByPage("/api/rooms").with(previousStaffToken)).andExpect(status().isOk());
        mvc.perform(
                        post("/api/admin/users/save-or-update")
                                .with(token(subject, "ADMIN", 0))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(targetId, "{\"role\":\"TECHNICIAN\"}")))
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
        mvc.perform(searchByPage("/api/rooms").with(token(targetSubject, "TECHNICIAN", 1)))
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
                        post("/api/admin/users/save-or-update")
                                .with(token(subject, "ADMIN", 0))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(targetId, "{\"role\":\"ADMIN\"}")))
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
        mvc.perform(searchByPage("/api/rooms").with(token(targetSubject, "ADMIN", 1)))
                .andExpect(status().isOk());
    }

    @Test
    void adminCannotDemoteOwnActiveAccount() throws Exception {
        Long unitId =
                jdbc.queryForObject("SELECT id FROM management_unit WHERE code='K1'", Long.class);
        mvc.perform(
                        post("/api/admin/users/save-or-update")
                                .with(token(subject, "ADMIN", 0))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        withId(
                                                id,
                                                "{\"role\":\"TECHNICIAN\",\"managementUnitId\":"
                                                        + unitId
                                                        + "}")))
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

    /** Đổi đơn vị giữ nguyên role nhưng token cũ không được tiếp tục sử dụng phạm vi cũ. */
    @Test
    void changingManagementUnitRevokesExistingTokenAndReturnsNewScope() throws Exception {
        Long firstUnit =
                jdbc.queryForObject("SELECT id FROM management_unit WHERE code='K1'", Long.class);
        Long nextUnit =
                jdbc.queryForObject("SELECT id FROM management_unit WHERE code='N23'", Long.class);
        Long targetId = createLinkedUser("STAFF", firstUnit, false);
        String targetSubject =
                jdbc.queryForObject(
                        "SELECT keycloak_user_id FROM app_user WHERE id=?", String.class, targetId);
        RequestPostProcessor oldToken = token(targetSubject, "STAFF", 0);
        mvc.perform(get("/api/auth/me").with(oldToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.managementUnit.id").value(firstUnit));
        mvc.perform(
                        post("/api/admin/users/save-or-update")
                                .with(token(subject, "ADMIN", 0))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        withId(
                                                targetId,
                                                "{\"managementUnitId\":" + nextUnit + "}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.managementUnitId").value(nextUnit));
        mvc.perform(get("/api/auth/me").with(oldToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SESSION_REVOKED"));
        mvc.perform(get("/api/auth/me").with(token(targetSubject, "STAFF", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.managementUnit.id").value(nextUnit));
    }

    /** Mở khóa không hồi sinh phiên cũ; reset chỉ trả mật khẩu tạm và thu hồi phiên mới nhất. */
    @Test
    void lockUnlockAndPasswordResetKeepOldTokensRevokedWithoutStoringPassword() throws Exception {
        Long unit =
                jdbc.queryForObject("SELECT id FROM management_unit WHERE code='K1'", Long.class);
        Long targetId = createLinkedUser("TECHNICIAN", unit, false);
        String targetSubject =
                jdbc.queryForObject(
                        "SELECT keycloak_user_id FROM app_user WHERE id=?", String.class, targetId);
        RequestPostProcessor originalToken = token(targetSubject, "TECHNICIAN", 0);
        mvc.perform(
                        post("/api/admin/users/" + targetId + "/lock")
                                .with(token(subject, "ADMIN", 0))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"Khóa tài khoản kiểm thử\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").with(originalToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_LOCKED"));
        mvc.perform(
                        post("/api/admin/users/" + targetId + "/unlock")
                                .with(token(subject, "ADMIN", 0)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").with(originalToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SESSION_REVOKED"));
        RequestPostProcessor afterUnlock = token(targetSubject, "TECHNICIAN", 1);
        mvc.perform(get("/api/auth/me").with(afterUnlock)).andExpect(status().isOk());
        mvc.perform(
                        post("/api/admin/users/" + targetId + "/reset-password")
                                .with(token(subject, "ADMIN", 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(targetId))
                .andExpect(jsonPath("$.temporaryPassword").isString());
        mvc.perform(get("/api/auth/me").with(afterUnlock))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SESSION_REVOKED"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT security_version FROM app_user WHERE id=?",
                                Long.class,
                                targetId))
                .isEqualTo(2);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT password_hash FROM app_user WHERE id=?",
                                String.class,
                                targetId))
                .isNull();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT must_change_password FROM app_user WHERE id=?",
                                Boolean.class,
                                targetId))
                .isTrue();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM audit_log WHERE actor_id=? AND entity_id=?"
                                    + " AND action='USER_RESET_PASSWORD' AND old_value IS NULL AND"
                                    + " new_value IS NULL",
                                Long.class,
                                id,
                                targetId))
                .isEqualTo(1);
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

    /** Migration mới giữ nguyên quyền mặc định đã kiểm thử; ADMIN nhận thêm màn phân quyền. */
    @Test
    void rolePermissionMigrationPreservesExistingNonAdminPrivileges() throws Exception {
        assertThat(jdbc.queryForList("SELECT role FROM role_permission_matrix", String.class))
                .containsExactlyInAnyOrder(Role.STAFF, Role.STUDENT, Role.TECHNICIAN);
        for (String role : List.of(Role.STAFF, Role.STUDENT, Role.TECHNICIAN)) {
            JsonNode stored =
                    mapper.readTree(
                            jdbc.queryForObject(
                                    "SELECT permissions::text FROM role_permission_matrix WHERE"
                                            + " role=?",
                                    String.class,
                                    role));
            var codes = new ArrayList<String>();
            stored.forEach(value -> codes.add(value.asText()));
            assertThat(codes)
                    .doesNotHaveDuplicates()
                    .containsExactlyInAnyOrderElementsOf(ScopeGuard.permissionsForRole(role));
            assertThat(matrixVersion(role)).isZero();
        }
        mvc.perform(get("/api/auth/me").with(token(subject, Role.ADMIN, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.length()").value(106))
                .andExpect(
                        jsonPath("$.permissions").value(hasItem(Permission.ROLE_PERMISSION_READ)))
                .andExpect(
                        jsonPath("$.permissions")
                                .value(hasItem(Permission.ROLE_PERMISSION_UPDATE)));
    }

    /** Ô không phù hợp với vai trò bị khóa; không biến quyền màn hình thành thay đổi vai trò. */
    @Test
    void adminReadsCompleteMatrixWithEligibilityAndRequiredProfileMetadata() throws Exception {
        JsonNode adminMatrix = readMatrix(Role.ADMIN);
        assertThat(adminMatrix.path("readOnly").asBoolean()).isTrue();
        assertThat(matrixPrivileges(adminMatrix)).hasSize(106);
        for (JsonNode privilege : matrixPrivileges(adminMatrix)) {
            assertThat(privilege.path("granted").asBoolean()).isTrue();
            assertThat(privilege.path("assignable").asBoolean()).isFalse();
        }
        JsonNode staffMatrix = readMatrix(Role.STAFF);
        assertThat(staffMatrix.path("readOnly").asBoolean()).isFalse();
        JsonNode roomUpdate = matrixPrivilege(staffMatrix, Permission.ROOM_UPDATE);
        assertThat(roomUpdate.path("granted").asBoolean()).isTrue();
        assertThat(roomUpdate.path("defaultGranted").asBoolean()).isTrue();
        assertThat(roomUpdate.path("assignable").asBoolean()).isTrue();
        JsonNode userCreate = matrixPrivilege(staffMatrix, Permission.USER_CREATE);
        assertThat(userCreate.path("granted").asBoolean()).isFalse();
        assertThat(userCreate.path("assignable").asBoolean()).isFalse();
        assertThat(
                        matrixPrivilege(staffMatrix, Permission.PROFILE_READ)
                                .path("required")
                                .asBoolean())
                .isTrue();
    }

    @Test
    void nonAdminAndAnonymousCannotReadOrModifyRolePermissionMatrix() throws Exception {
        for (String role : List.of(Role.STAFF, Role.STUDENT, Role.TECHNICIAN)) {
            RequestPostProcessor actor = useRole(role);
            mvc.perform(get("/api/admin/role-permissions/STAFF").with(actor))
                    .andExpect(status().isForbidden());
            mvc.perform(
                            put("/api/admin/role-permissions/STAFF")
                                    .with(actor)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            matrixBody(
                                                    0, ScopeGuard.permissionsForRole(Role.STAFF))))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/admin/role-permissions/STAFF")).andExpect(status().isUnauthorized());
        assertThat(matrixVersion(Role.STAFF)).isZero();
    }

    /** Token vẫn hợp lệ: quyền mới được đọc từ CSDL ở request kế tiếp, không cần đăng nhập lại. */
    @Test
    void revokingAndRestoringScreenPermissionsImmediatelyAffectsPreviouslyIssuedToken()
            throws Exception {
        Long unitId =
                jdbc.queryForObject("SELECT id FROM management_unit WHERE code='K1'", Long.class);
        Long staffId = createLinkedUser(Role.STAFF, unitId, false);
        String staffSubject =
                jdbc.queryForObject(
                        "SELECT keycloak_user_id FROM app_user WHERE id=?", String.class, staffId);
        RequestPostProcessor oldToken = token(staffSubject, Role.STAFF, 0);
        mvc.perform(searchByPage("/api/rooms").with(oldToken)).andExpect(status().isOk());

        var restricted = new ArrayList<>(ScopeGuard.permissionsForRole(Role.STAFF));
        // Thu hồi đúng màn ROOM, giữ ROOM_TYPE/ROOM_APPLICATION/ROOM_TRANSFER độc lập.
        restricted.removeAll(
                List.of(
                        Permission.ROOM_READ,
                        Permission.ROOM_CREATE,
                        Permission.ROOM_UPDATE,
                        Permission.ROOM_DELETE));
        updateMatrix(Role.STAFF, 0, restricted)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(searchByPage("/api/rooms").with(oldToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").with(oldToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions").value(not(hasItem(Permission.ROOM_READ))))
                .andExpect(jsonPath("$.permissions").value(hasItem(Permission.PROFILE_READ)));

        updateMatrix(Role.STAFF, 1, ScopeGuard.permissionsForRole(Role.STAFF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
        mvc.perform(searchByPage("/api/rooms").with(oldToken)).andExpect(status().isOk());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT security_version FROM app_user WHERE id=?",
                                Long.class,
                                staffId))
                .isZero();
        // ADMIN không bị ảnh hưởng bởi thao tác thu hồi quyền của STAFF.
        mvc.perform(searchByPage("/api/rooms").with(token(subject, Role.ADMIN, 0)))
                .andExpect(status().isOk());
    }

    /** Kiểm tra trực tiếp advisor trước thân hàm, tránh chỉ thử giao diện hoặc danh sách /me. */
    @Test
    void removingTechnicianUpdatePermissionBlocksAnnotationBeforeControllerBody() throws Exception {
        Long unitId =
                jdbc.queryForObject("SELECT id FROM management_unit WHERE code='K1'", Long.class);
        Long technicianId = createLinkedUser(Role.TECHNICIAN, unitId, false);
        String technicianSubject =
                jdbc.queryForObject(
                        "SELECT keycloak_user_id FROM app_user WHERE id=?",
                        String.class,
                        technicianId);
        RequestPostProcessor oldToken = token(technicianSubject, Role.TECHNICIAN, 0);
        mvc.perform(get("/api/privilege-test/any-permission").with(oldToken))
                .andExpect(status().isOk());
        var restricted = new ArrayList<>(ScopeGuard.permissionsForRole(Role.TECHNICIAN));
        restricted.remove(Permission.MAINTENANCE_UPDATE);
        updateMatrix(Role.TECHNICIAN, 0, restricted).andExpect(status().isOk());
        mvc.perform(get("/api/privilege-test/any-permission").with(oldToken))
                .andExpect(status().isForbidden());
        assertThat(privilegeProbe.invocationCount()).isEqualTo(1);
        mvc.perform(get("/api/auth/me").with(oldToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions").value(hasItem(Permission.MAINTENANCE_READ)))
                .andExpect(
                        jsonPath("$.permissions")
                                .value(not(hasItem(Permission.MAINTENANCE_UPDATE))));
    }

    /** Giữ quyền đọc hồ sơ để tài khoản bị thu hồi nghiệp vụ vẫn lấy /me và đăng xuất được. */
    @Test
    void profileOnlyPermissionsKeepLoginProfileAndLogoutAccessible() throws Exception {
        Long unitId =
                jdbc.queryForObject("SELECT id FROM management_unit WHERE code='K1'", Long.class);
        Long staffId = createLinkedUser(Role.STAFF, unitId, false);
        String staffSubject =
                jdbc.queryForObject(
                        "SELECT keycloak_user_id FROM app_user WHERE id=?", String.class, staffId);
        RequestPostProcessor oldToken = token(staffSubject, Role.STAFF, 0);
        updateMatrix(Role.STAFF, 0, List.of(Permission.PROFILE_READ)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").with(oldToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.length()").value(1));
        mvc.perform(searchByPage("/api/rooms").with(oldToken)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").with(oldToken)).andExpect(status().isNoContent());
    }

    /** Backend xác thực toàn bộ danh sách trước khi lưu; không tin ô disabled ở frontend. */
    @Test
    void invalidPermissionListsAreRejectedWithoutChangingPersistedMatrix() throws Exception {
        List<String> baseline = ScopeGuard.permissionsForRole(Role.STAFF);
        var duplicate = new ArrayList<>(baseline);
        duplicate.add(Permission.ROOM_READ);
        var unknown = new ArrayList<>(baseline);
        unknown.add("UNREGISTERED_READ");
        var ineligible = new ArrayList<>(baseline);
        ineligible.add(Permission.USER_CREATE);
        var missingRequired = new ArrayList<>(baseline);
        missingRequired.remove(Permission.PROFILE_READ);
        var missingRead = new ArrayList<>(baseline);
        missingRead.remove(Permission.ROOM_READ);
        var blank = new ArrayList<>(baseline);
        blank.add(" ");
        for (List<String> invalid :
                List.of(
                        duplicate,
                        unknown,
                        ineligible,
                        missingRequired,
                        missingRead,
                        blank,
                        List.<String>of())) {
            updateMatrix(Role.STAFF, 0, invalid)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        }
        updateMatrix(Role.STAFF, -1, baseline).andExpect(status().isBadRequest());
        assertThat(matrixVersion(Role.STAFF)).isZero();
        assertThat(
                        mapper.readTree(
                                        jdbc.queryForObject(
                                                "SELECT permissions::text FROM"
                                                    + " role_permission_matrix WHERE role='STAFF'",
                                                String.class))
                                .size())
                .isEqualTo(73);
        assertThat(matrixAuditCount()).isZero();
    }

    /** Hai quản trị viên sửa cùng phiên bản: người lưu sau nhận 409, không ghi đè dữ liệu mới. */
    @Test
    void staleMatrixVersionCannotOverwriteNewerPermissionsOrProduceExtraAudit() throws Exception {
        var restricted = new ArrayList<>(ScopeGuard.permissionsForRole(Role.STAFF));
        restricted.remove(Permission.ROOM_DELETE);
        updateMatrix(Role.STAFF, 0, restricted)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        updateMatrix(Role.STAFF, 0, ScopeGuard.permissionsForRole(Role.STAFF))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PERMISSION_MATRIX_STALE"));
        assertThat(matrixVersion(Role.STAFF)).isEqualTo(1);
        assertThat(
                        matrixPrivilege(readMatrix(Role.STAFF), Permission.ROOM_DELETE)
                                .path("granted")
                                .asBoolean())
                .isFalse();
        entityManager.flush();
        assertThat(matrixAuditCount()).isEqualTo(1);
    }

    @Test
    void savingSamePermissionSetInAnotherOrderDoesNotIncrementVersionOrAudit() throws Exception {
        var same = new ArrayList<>(ScopeGuard.permissionsForRole(Role.STAFF));
        Collections.reverse(same);
        updateMatrix(Role.STAFF, 0, same)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(0));
        entityManager.flush();
        assertThat(matrixAuditCount()).isZero();
        assertThat(matrixVersion(Role.STAFF)).isZero();
    }

    /**
     * Nhật ký chứa phiên bản và quyền trước/sau, cùng người thao tác; không chỉ ghi tên sự kiện.
     */
    @Test
    void changedMatrixRecordsExactBeforeAfterAndUpdater() throws Exception {
        var restricted = new ArrayList<>(ScopeGuard.permissionsForRole(Role.STAFF));
        restricted.remove(Permission.ROOM_DELETE);
        updateMatrix(Role.STAFF, 0, restricted).andExpect(status().isOk());
        entityManager.flush();
        Map<String, Object> audit =
                jdbc.queryForMap(
                        "SELECT actor_id,entity_type,entity_id,old_value::text AS old_value,"
                                + " new_value::text AS new_value FROM audit_log"
                                + " WHERE actor_id=? AND action='ROLE_PERMISSION_UPDATE'",
                        id);
        assertThat(audit.get("actor_id")).isEqualTo(id);
        assertThat(audit.get("entity_type")).isEqualTo("ROLE_PERMISSION");
        assertThat(audit.get("entity_id")).isNull();
        JsonNode before = mapper.readTree((String) audit.get("old_value"));
        JsonNode after = mapper.readTree((String) audit.get("new_value"));
        assertThat(before.path("role").asText()).isEqualTo(Role.STAFF);
        assertThat(before.path("version").asLong()).isZero();
        assertThat(before.path("permissions").size()).isEqualTo(73);
        assertThat(after.path("role").asText()).isEqualTo(Role.STAFF);
        assertThat(after.path("version").asLong()).isEqualTo(1);
        assertThat(after.path("permissions").size()).isEqualTo(72);
        var afterCodes = new ArrayList<String>();
        after.path("permissions").forEach(value -> afterCodes.add(value.asText()));
        assertThat(afterCodes).doesNotContain(Permission.ROOM_DELETE);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT updated_by FROM role_permission_matrix WHERE role='STAFF'",
                                Long.class))
                .isEqualTo(id);
    }

    @Test
    void adminPermissionsAreImmutableAndUnknownRoleDoesNotUseAnyFallback() throws Exception {
        updateMatrix(Role.ADMIN, 0, List.of(Permission.PROFILE_READ))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_PERMISSIONS_IMMUTABLE"));
        for (String role : List.of("UNKNOWN_ROLE", "staff")) {
            mvc.perform(
                            get("/api/admin/role-permissions/" + role)
                                    .with(token(subject, Role.ADMIN, 0)))
                    .andExpect(status().isBadRequest());
            updateMatrix(role, 0, List.of(Permission.PROFILE_READ))
                    .andExpect(status().isBadRequest());
        }
        assertThat(matrixAuditCount()).isZero();
        mvc.perform(get("/api/auth/me").with(token(subject, Role.ADMIN, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.length()").value(106));
    }

    /** Mất cấu hình không tự hồi sinh quyền mặc định; ADMIN vẫn truy cập để chẩn đoán. */
    @Test
    void missingRolePermissionRowFailsClosedWithoutAffectingAdmin() throws Exception {
        Long unitId =
                jdbc.queryForObject("SELECT id FROM management_unit WHERE code='K1'", Long.class);
        Long staffId = createLinkedUser(Role.STAFF, unitId, false);
        String staffSubject =
                jdbc.queryForObject(
                        "SELECT keycloak_user_id FROM app_user WHERE id=?", String.class, staffId);
        RequestPostProcessor oldToken = token(staffSubject, Role.STAFF, 0);
        jdbc.update("DELETE FROM role_permission_matrix WHERE role='STAFF'");
        mvc.perform(get("/api/auth/me").with(oldToken)).andExpect(status().isForbidden());
        mvc.perform(searchByPage("/api/rooms").with(oldToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/role-permissions/STAFF").with(token(subject, Role.ADMIN, 0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PERMISSION_MATRIX_UNAVAILABLE"));
        updateMatrix(Role.STAFF, 0, ScopeGuard.permissionsForRole(Role.STAFF))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/auth/me").with(token(subject, Role.ADMIN, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.length()").value(106));
    }

    /** Dữ liệu quyền hỏng hoặc tự chèn quyền ngoài vai trò phải khóa cả bộ quyền của vai trò đó. */
    @Test
    void corruptRolePermissionArraysFailClosedInsteadOfPartiallyGrantingPermissions()
            throws Exception {
        Long unitId =
                jdbc.queryForObject("SELECT id FROM management_unit WHERE code='K1'", Long.class);
        Long staffId = createLinkedUser(Role.STAFF, unitId, false);
        String staffSubject =
                jdbc.queryForObject(
                        "SELECT keycloak_user_id FROM app_user WHERE id=?", String.class, staffId);
        RequestPostProcessor oldToken = token(staffSubject, Role.STAFF, 0);
        for (String invalidJson :
                List.of(
                        "[\"PROFILE_READ\",37]",
                        "[\"PROFILE_READ\",\"PROFILE_READ\"]",
                        "[\"PROFILE_READ\",\"UNREGISTERED_READ\"]",
                        "[\"PROFILE_READ\",\"USER_CREATE\"]",
                        "[\"ROOM_READ\"]",
                        "[\"PROFILE_READ\",\"ROOM_UPDATE\"]")) {
            jdbc.update(
                    "UPDATE role_permission_matrix SET permissions=CAST(? AS jsonb) WHERE"
                            + " role='STAFF'",
                    invalidJson);
            mvc.perform(get("/api/auth/me").with(oldToken)).andExpect(status().isForbidden());
            mvc.perform(searchByPage("/api/rooms").with(oldToken))
                    .andExpect(status().isForbidden());
        }
    }

    private JsonNode readMatrix(String role) throws Exception {
        return mapper.readTree(
                mvc.perform(
                                get("/api/admin/role-permissions/" + role)
                                        .with(token(subject, Role.ADMIN, 0)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
    }

    private List<JsonNode> matrixPrivileges(JsonNode matrix) {
        var privileges = new ArrayList<JsonNode>();
        matrix.path("screens")
                .forEach(screen -> screen.path("privileges").forEach(privileges::add));
        return privileges;
    }

    private JsonNode matrixPrivilege(JsonNode matrix, String code) {
        return matrixPrivileges(matrix).stream()
                .filter(privilege -> code.equals(privilege.path("code").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Thiếu ô quyền " + code));
    }

    private ResultActions updateMatrix(String role, long version, List<String> permissions)
            throws Exception {
        return mvc.perform(
                put("/api/admin/role-permissions/" + role)
                        .with(token(subject, Role.ADMIN, 0))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(matrixBody(version, permissions)));
    }

    private String matrixBody(long version, List<String> permissions) throws Exception {
        return mapper.writeValueAsString(Map.of("version", version, "permissions", permissions));
    }

    private long matrixVersion(String role) {
        return jdbc.queryForObject(
                "SELECT version FROM role_permission_matrix WHERE role=?", Long.class, role);
    }

    private long matrixAuditCount() {
        return jdbc.queryForObject(
                "SELECT count(*) FROM audit_log WHERE actor_id=? AND"
                        + " action='ROLE_PERMISSION_UPDATE'",
                Long.class,
                id);
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

    /** Gửi bộ lọc dưới dạng JSON có kiểu; dữ liệu sai vẫn để API kiểm tra và trả 400. */
    private MockHttpServletRequestBuilder searchByPage(String base, Object... filters)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("page", 0);
        body.put("size", 20);
        for (int index = 0; index < filters.length; index += 2) {
            String key = (String) filters[index];
            Object value = filters[index + 1];
            if ("sort".equals(key)) {
                value = List.of(value.toString());
            } else if (List.of("page", "size", "managementUnitId", "actorId", "buildingId")
                    .contains(key)) {
                value = Long.valueOf(value.toString());
            } else if ("locked".equals(key)
                    && List.of("true", "false").contains(value.toString())) {
                value = Boolean.valueOf(value.toString());
            }
            body.put(key, value);
        }
        return post(base + "/search-by-page")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body));
    }

    /** Giữ nguyên payload cập nhật từng phần; id chỉ chọn bản ghi, không cho phép tạo ngầm. */
    private String withId(Long targetId, String payload) throws Exception {
        ObjectNode body = (ObjectNode) mapper.readTree(payload);
        body.put("id", targetId);
        return mapper.writeValueAsString(body);
    }
}
