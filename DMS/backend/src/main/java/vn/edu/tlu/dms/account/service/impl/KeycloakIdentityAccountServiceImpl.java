package vn.edu.tlu.dms.account.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import vn.edu.tlu.dms.account.config.SecurityConfig.KeycloakProperties;
import vn.edu.tlu.dms.account.domain.AppUser;
import vn.edu.tlu.dms.account.repository.AppUserRepository;
import vn.edu.tlu.dms.account.service.IdentityAccountService;
import vn.edu.tlu.dms.common.constants.DmsConstants.KeycloakClaim;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.exception.BusinessException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Gọi Admin REST bằng service account; không truyền secret hoặc mật khẩu vào log/audit. */
@Service
public class KeycloakIdentityAccountServiceImpl implements IdentityAccountService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(KeycloakIdentityAccountServiceImpl.class);

    private static final Set<String> DMS_ROLES = Role.ALL;

    private final KeycloakProperties properties;

    private final RestClient client;

    private final JdbcTemplate jdbc;

    private final AppUserRepository users;

    private final TransactionTemplate transaction;

    private final ObjectMapper mapper;

    private final SecureRandom random = new SecureRandom();

    private String token;

    private Instant expiresAt = Instant.EPOCH;

    public KeycloakIdentityAccountServiceImpl(
            KeycloakProperties properties,
            JdbcTemplate jdbc,
            AppUserRepository users,
            PlatformTransactionManager manager,
            ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.users = users;
        this.transaction = new TransactionTemplate(manager);
        this.mapper = mapper;
        this.properties = properties;
        var factory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.client =
                RestClient.builder()
                        .baseUrl(properties.serverUrl())
                        .requestFactory(factory)
                        .build();
    }

    private synchronized String accessToken() {
        if (token != null && Instant.now().isBefore(expiresAt)) {
            return token;
        }
        if (properties.adminClientSecret() == null || properties.adminClientSecret().isBlank()) {
            throw new BusinessException("IDENTITY_NOT_CONFIGURED", 503);
        }
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.adminClientId());
        form.add("client_secret", properties.adminClientSecret());
        try {
            Map<?, ?> response =
                    client.post()
                            .uri(
                                    "/realms/{realm}/protocol/openid-connect/token",
                                    properties.realm())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(form)
                            .retrieve()
                            .body(Map.class);
            if (response == null || !(response.get("access_token") instanceof String value)) {
                throw new BusinessException("IDENTITY_UNAVAILABLE", 503);
            }
            token = value;
            expiresAt =
                    Instant.now()
                            .plusSeconds(((Number) response.get("expires_in")).longValue() - 15);
            return token;
        } catch (BusinessException error) {
            throw error;
        } catch (RuntimeException error) {
            throw new BusinessException("IDENTITY_UNAVAILABLE", 503);
        }
    }

    private String usersPath() {
        return "/admin/realms/" + properties.realm() + "/users";
    }

    private Map<String, Object> readAccount(String id) {
        @SuppressWarnings("unchecked")
        Map<String, Object> account =
                client.get()
                        .uri(usersPath() + "/{id}", id)
                        .headers(headers -> headers.setBearerAuth(accessToken()))
                        .retrieve()
                        .body(Map.class);
        if (account == null) {
            throw new BusinessException("IDENTITY_UNAVAILABLE", 503);
        }
        return new HashMap<>(account);
    }

    private void writeAccount(String id, Object account) {
        client.put()
                .uri(usersPath() + "/{id}", id)
                .headers(headers -> headers.setBearerAuth(accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(account)
                .retrieve()
                .toBodilessEntity();
    }

    private void updateDmsFields(Map<String, Object> account, AppUser user) {
        account.put("username", user.getUsername());
        account.put("firstName", user.getFullName());
        account.put("enabled", !Boolean.TRUE.equals(user.getLocked()));
        // Không ghi đè các thuộc tính độc lập khác của Keycloak.
        var attributes = new HashMap<String, Object>();
        if (account.get("attributes") instanceof Map<?, ?> existing) {
            existing.forEach((key, value) -> attributes.put(key.toString(), value));
        }
        attributes.put(
                KeycloakClaim.SECURITY_VERSION, List.of(user.getSecurityVersion().toString()));
        account.put("attributes", attributes);
    }

    private List<?> readRoles(String id) {
        List<?> roles =
                client.get()
                        .uri(usersPath() + "/{id}/role-mappings/realm", id)
                        .headers(headers -> headers.setBearerAuth(accessToken()))
                        .retrieve()
                        .body(List.class);
        return roles == null ? List.of() : roles;
    }

    private void replaceRole(String id, String role) {
        List<?> previous =
                readRoles(id).stream()
                        .filter(
                                value ->
                                        value instanceof Map<?, ?> item
                                                && DMS_ROLES.contains(item.get("name")))
                        .toList();
        if (!previous.isEmpty()) {
            client.method(HttpMethod.DELETE)
                    .uri(usersPath() + "/{id}/role-mappings/realm", id)
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(previous)
                    .retrieve()
                    .toBodilessEntity();
        }
        Map<?, ?> representation =
                client.get()
                        .uri("/admin/realms/{realm}/roles/{role}", properties.realm(), role)
                        .headers(headers -> headers.setBearerAuth(accessToken()))
                        .retrieve()
                        .body(Map.class);
        client.post()
                .uri(usersPath() + "/{id}/role-mappings/realm", id)
                .headers(headers -> headers.setBearerAuth(accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(representation))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void provisionAccount(AppUser user, String temporaryPassword) {
        if (user.getKeycloakUserId() != null) {
            throw BusinessException.conflict("IDENTITY_ALREADY_LINKED");
        }
        String identityId = null;
        try {
            var account = new HashMap<String, Object>();
            updateDmsFields(account, user);
            account.put(
                    "credentials",
                    List.of(
                            Map.of(
                                    "type",
                                    "password",
                                    "value",
                                    temporaryPassword,
                                    "temporary",
                                    true)));
            account.put("requiredActions", List.of("UPDATE_PASSWORD"));
            var response =
                    client.post()
                            .uri(usersPath())
                            .headers(headers -> headers.setBearerAuth(accessToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(account)
                            .retrieve()
                            .toBodilessEntity();
            var location = response.getHeaders().getLocation();
            if (location == null) {
                throw new BusinessException("IDENTITY_UNAVAILABLE", 503);
            }
            identityId = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
            user.setKeycloakUserId(identityId);
            // Keycloak là nơi duy nhất giữ mật khẩu; hash cũ không dùng để đăng nhập nữa.
            user.setPasswordHash(null);
            String createdId = identityId;
            onRollback(() -> deleteAccount(createdId));
            replaceRole(identityId, user.getRole());
        } catch (RuntimeException error) {
            if (identityId != null
                    && !TransactionSynchronizationManager.isSynchronizationActive()) {
                deleteAccount(identityId);
            }
            throw translate(error);
        }
    }

    @Override
    public void synchronizeAccount(AppUser user) {
        requireLinked(user);
        try {
            var previous = readAccount(user.getKeycloakUserId());
            List<?> previousRoles = readRoles(user.getKeycloakUserId());
            String identityId = user.getKeycloakUserId();
            onRollback(
                    () -> {
                        writeAccount(identityId, previous);
                        List<?> currentRoles = readRoles(identityId);
                        if (!currentRoles.isEmpty()) {
                            client.method(HttpMethod.DELETE)
                                    .uri(usersPath() + "/{id}/role-mappings/realm", identityId)
                                    .headers(headers -> headers.setBearerAuth(accessToken()))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(currentRoles)
                                    .retrieve()
                                    .toBodilessEntity();
                        }
                        if (!previousRoles.isEmpty()) {
                            client.post()
                                    .uri(usersPath() + "/{id}/role-mappings/realm", identityId)
                                    .headers(headers -> headers.setBearerAuth(accessToken()))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(previousRoles)
                                    .retrieve()
                                    .toBodilessEntity();
                        }
                    });
            var updated = new HashMap<>(previous);
            updateDmsFields(updated, user);
            writeAccount(identityId, updated);
            replaceRole(identityId, user.getRole());
        } catch (RuntimeException error) {
            throw translate(error);
        }
    }

    @Override
    public void resetAccountPassword(AppUser user, String temporaryPassword) {
        requireLinked(user);
        try {
            client.put()
                    .uri(usersPath() + "/{id}/reset-password", user.getKeycloakUserId())
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("type", "password", "value", temporaryPassword, "temporary", true))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException error) {
            throw translate(error);
        }
    }

    @Override
    public void revokeIdentitySessions(AppUser user) {
        requireLinked(user);
        try {
            client.post()
                    .uri(usersPath() + "/{id}/logout", user.getKeycloakUserId())
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException error) {
            throw translate(error);
        }
    }

    @Override
    public void clearTemporaryLoginFailures(AppUser user) {
        requireLinked(user);
        try {
            client.delete()
                    .uri(
                            "/admin/realms/{realm}/attack-detection/brute-force/users/{id}",
                            properties.realm(),
                            user.getKeycloakUserId())
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException error) {
            throw translate(error);
        }
    }

    @Override
    public void deleteIdentityAfterCommit(AppUser user) {
        requireLinked(user);
        String identityId = user.getKeycloakUserId();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        runCleanup(() -> deleteAccount(identityId), "xóa danh tính", identityId);
                    }
                });
    }

    private void deleteAccount(String id) {
        client.delete()
                .uri(usersPath() + "/{id}", id)
                .headers(headers -> headers.setBearerAuth(accessToken()))
                .retrieve()
                .toBodilessEntity();
    }

    private void requireLinked(AppUser user) {
        if (user.getKeycloakUserId() == null) {
            throw new BusinessException("IDENTITY_NOT_LINKED", 409);
        }
    }

    private void onRollback(Runnable compensation) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {

                        @Override
                        public void afterCompletion(int status) {
                            if (status == STATUS_ROLLED_BACK) {
                                runCleanup(compensation, "bù transaction", null);
                            }
                        }
                    });
        }
    }

    private void runCleanup(Runnable action, String operation, String identityId) {
        try {
            action.run();
        } catch (RuntimeException error) {
            // Không log response của Keycloak vì có thể chứa thông tin tài khoản/credential.
            LOGGER.error(
                    "Không {} Keycloak cho ID {}; cần đối soát bằng Admin Console",
                    operation,
                    identityId);
        }
    }

    private BusinessException translate(RuntimeException error) {
        if (error instanceof BusinessException business) {
            return business;
        }
        if (error instanceof RestClientResponseException response
                && response.getStatusCode().value() == 409) {
            return BusinessException.conflict("DUPLICATE_USERNAME");
        }
        return new BusinessException("IDENTITY_UNAVAILABLE", 503);
    }

    @Override
    public void migrateUnlinkedAccounts(
            String adminUsername, String initialAdminPassword, String credentialFile) {
        if (initialAdminPassword == null
                || !initialAdminPassword.matches("(?s)(?=.*[A-Za-z])(?=.*[0-9]).{8,64}")) {
            throw new IllegalArgumentException(
                    "Cần DMS_KEYCLOAK_INITIAL_ADMIN_PASSWORD có chữ, số và dài 8–64 ký tự");
        }
        transaction.executeWithoutResult(
                status -> {
                    var existing = users.lockByUsername(adminUsername).orElse(null);
                    if (existing != null) {
                        if (!Role.ADMIN.equals(existing.getRole())) {
                            throw BusinessException.conflict("BOOTSTRAP_USERNAME_IN_USE");
                        }
                        return;
                    }
                    var admin = new AppUser();
                    admin.setUsername(adminUsername);
                    admin.setFullName("Quản trị viên DMS");
                    admin.setRole(Role.ADMIN);
                    admin.setCrossUnitAccess(false);
                    admin.setMustChangePassword(true);
                    admin.setLocked(false);
                    admin.setFailedLoginCount(0);
                    admin.setCreatedAt(OffsetDateTime.now());
                    users.saveAndFlush(admin);
                });
        List<Long> ids =
                jdbc.queryForList(
                        "SELECT id FROM app_user WHERE keycloak_user_id IS NULL ORDER BY id",
                        Long.class);
        var credentials = new ArrayList<Map<String, Object>>();
        Path output = Path.of(credentialFile);
        if (!ids.isEmpty()) {
            // Tệp chứa mật khẩu khởi tạo phải ở thư mục local đã bị Git bỏ qua.
            try {
                Files.createDirectories(output.toAbsolutePath().getParent());
                if (Files.exists(output)) {
                    throw new IllegalStateException(
                            "Tệp cấp mật khẩu đã tồn tại; giữ lại và chọn tên tệp khác");
                }
                Files.writeString(output, "[]", StandardCharsets.UTF_8);
            } catch (IOException error) {
                throw new IllegalStateException("Không tạo được tệp cấp mật khẩu local", error);
            }
        }
        for (Long id : ids) {
            transaction.executeWithoutResult(
                    status -> {
                        AppUser user = users.lockById(id).orElseThrow(BusinessException::notFound);
                        if (user.getKeycloakUserId() != null) {
                            return;
                        }
                        String password =
                                user.getUsername().equals(adminUsername)
                                        ? initialAdminPassword
                                        : temporaryPassword();
                        provisionAccount(user, password);
                        user.setMustChangePassword(true);
                        users.saveAndFlush(user);
                        var record =
                                Map.<String, Object>of(
                                        "userId",
                                        id,
                                        "username",
                                        user.getUsername(),
                                        "temporaryPassword",
                                        password);
                        credentials.add(record);
                        try {
                            Files.writeString(
                                    output,
                                    mapper.writerWithDefaultPrettyPrinter()
                                            .writeValueAsString(credentials),
                                    StandardCharsets.UTF_8);
                        } catch (IOException error) {
                            credentials.remove(record);
                            throw new IllegalStateException(
                                    "Không lưu được mật khẩu cấp mới; rollback tài khoản hiện tại",
                                    error);
                        }
                    });
        }
    }

    private String temporaryPassword() {
        byte[] bytes = new byte[18];
        random.nextBytes(bytes);
        return "Dms1!" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
