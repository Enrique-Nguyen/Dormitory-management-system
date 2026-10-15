package vn.edu.tlu.dms.common.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.constants.DmsConstants.Action;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.PermissionCatalog;
import vn.edu.tlu.dms.common.constants.DmsConstants.PermissionCatalog.ScreenDefinition;
import vn.edu.tlu.dms.common.constants.DmsConstants.PermissionMessage;
import vn.edu.tlu.dms.common.constants.DmsConstants.Resource;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.RolePermissionService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ma trận thao tác lưu tại DMS; không thay đổi vai trò đăng nhập ở Keycloak. */
@Service
public class RolePermissionServiceImpl implements RolePermissionService {

    private final JdbcTemplate jdbc;

    private final ObjectMapper mapper;

    private final CurrentUser currentUser;

    private final ObjectProvider<AuditService> audit;

    public RolePermissionServiceImpl(
            JdbcTemplate jdbc,
            ObjectMapper mapper,
            CurrentUser currentUser,
            ObjectProvider<AuditService> audit) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.currentUser = currentUser;
        // Chỉ lấy AuditService lúc lưu: tránh vòng phụ thuộc Audit -> ScopeGuard -> service này.
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public Matrix getRolePermissionMatrix(String role) {
        requireAdmin();
        requireKnownRole(role);
        if (Role.ADMIN.equals(role)) {
            return createMatrix(role, 0, Permission.ALL);
        }
        StoredPermissions row = findStoredPermissions(role, false);
        if (row == null) {
            throw BusinessException.conflict(ErrorCode.PERMISSION_MATRIX_UNAVAILABLE);
        }
        return createMatrix(role, row.version(), readValidStoredPermissions(role, row.json()));
    }

    @Override
    @Transactional
    public Matrix updateRolePermissions(String role, Update input) {
        Actor actor = requireAdmin();
        requireKnownRole(role);
        if (Role.ADMIN.equals(role)) {
            throw new BusinessException(
                    ErrorCode.ADMIN_PERMISSIONS_IMMUTABLE, HttpStatus.FORBIDDEN);
        }
        if (input == null || input.version() == null || input.version() < 0) {
            throw BusinessException.invalid(ApiField.VERSION, PermissionMessage.INVALID_VERSION);
        }
        Set<String> selected = validateSelectedPermissions(role, input.permissions());
        // Khóa cùng dòng vai trò để hai Admin không cùng ghi đè một phiên bản đã đọc.
        StoredPermissions row = findStoredPermissions(role, true);
        if (row == null) {
            throw BusinessException.conflict(ErrorCode.PERMISSION_MATRIX_UNAVAILABLE);
        }
        if (row.version() != input.version()) {
            throw BusinessException.conflict(ErrorCode.PERMISSION_MATRIX_STALE);
        }
        // Với dữ liệu hỏng, quyền có hiệu lực trước khi sửa là rỗng và audit ghi nhận tập này.
        Set<String> previous = readValidStoredPermissions(role, row.json());
        if (previous.equals(selected)) {
            return createMatrix(role, row.version(), previous);
        }
        long nextVersion = Math.incrementExact(row.version());
        jdbc.update(
                "UPDATE role_permission_matrix SET permissions = CAST(? AS jsonb), version = ?,"
                        + " updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE role = ?",
                writePermissions(selected),
                nextVersion,
                actor.id(),
                role);
        // Cùng transaction: lỗi ghi audit sẽ rollback cả quyền; không thu hồi phiên Keycloak.
        audit.getObject()
                .record(
                        actor.id(),
                        AuditAction.ROLE_PERMISSION_UPDATE,
                        Resource.ROLE_PERMISSION,
                        null,
                        auditValue(role, row.version(), previous),
                        auditValue(role, nextVersion, selected),
                        null);
        return createMatrix(role, nextVersion, selected);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getGrantedPermissions(String role) {
        if (Role.ADMIN.equals(role)) {
            return Permission.ALL.stream().sorted().toList();
        }
        if (role == null || !Role.ALL.contains(role)) {
            return List.of();
        }
        StoredPermissions row = findStoredPermissions(role, false);
        // Không cache hay dự phòng bằng mặc định: token cũ mất quyền ở request tiếp theo.
        return row == null
                ? List.of()
                : readValidStoredPermissions(role, row.json()).stream().sorted().toList();
    }

    private Actor requireAdmin() {
        Actor actor = currentUser.get();
        if (!actor.isAdmin()) {
            throw BusinessException.forbidden();
        }
        return actor;
    }

    private void requireKnownRole(String role) {
        if (role == null || !Role.ALL.contains(role)) {
            throw BusinessException.invalid(ApiField.ROLE, PermissionMessage.UNKNOWN_ROLE);
        }
    }

    private StoredPermissions findStoredPermissions(String role, boolean lock) {
        String sql = "SELECT permissions::text, version FROM role_permission_matrix WHERE role = ?";
        if (lock) {
            sql += " FOR UPDATE";
        }
        List<StoredPermissions> rows =
                jdbc.query(
                        sql,
                        (result, index) ->
                                new StoredPermissions(result.getString(1), result.getLong(2)),
                        role);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Dữ liệu bị sửa ngoài API không được mở thêm quyền, dù chỉ một mã sai trong danh sách. */
    private Set<String> readValidStoredPermissions(String role, String json) {
        try {
            JsonNode node = mapper.readTree(json);
            if (node == null || !node.isArray()) {
                return Set.of();
            }
            List<String> permissions = new ArrayList<>();
            for (JsonNode value : node) {
                if (!value.isTextual()) {
                    return Set.of();
                }
                permissions.add(value.textValue());
            }
            return validateSelectedPermissions(role, permissions);
        } catch (JsonProcessingException | BusinessException exception) {
            return Set.of();
        }
    }

    private Set<String> validateSelectedPermissions(String role, List<String> permissions) {
        if (permissions == null) {
            throw BusinessException.invalid(ApiField.PERMISSIONS, PermissionMessage.REQUIRED_LIST);
        }
        Set<String> eligible = Set.copyOf(ScopeGuard.permissionsForRole(role));
        Set<String> selected = new LinkedHashSet<>();
        for (String permission : permissions) {
            if (permission == null || !Permission.ALL.contains(permission)) {
                throw BusinessException.invalid(
                        ApiField.PERMISSIONS, PermissionMessage.UNKNOWN_PERMISSION);
            }
            if (!selected.add(permission)) {
                throw BusinessException.invalid(
                        ApiField.PERMISSIONS,
                        PermissionMessage.DUPLICATE_PERMISSION.formatted(permission));
            }
            if (!eligible.contains(permission)) {
                throw BusinessException.invalid(
                        ApiField.PERMISSIONS,
                        PermissionMessage.INELIGIBLE_PERMISSION.formatted(permission));
            }
        }
        if (!selected.contains(Permission.PROFILE_READ)) {
            throw BusinessException.invalid(
                    ApiField.PERMISSIONS, PermissionMessage.REQUIRED_PROFILE);
        }
        for (String permission : selected) {
            String resource = resourceForPermission(permission);
            String read = permissionCode(resource, Action.READ);
            if (!selected.contains(read)) {
                throw BusinessException.invalid(
                        ApiField.PERMISSIONS,
                        PermissionMessage.REQUIRED_READ.formatted(read, permission));
            }
        }
        return Set.copyOf(selected);
    }

    private Matrix createMatrix(String role, long version, Set<String> granted) {
        boolean readOnly = Role.ADMIN.equals(role);
        Set<String> defaults = Set.copyOf(ScopeGuard.permissionsForRole(role));
        List<Screen> screens = new ArrayList<>();
        for (ScreenDefinition screen : PermissionCatalog.SCREENS) {
            List<Privilege> privileges = new ArrayList<>();
            for (String action : PermissionCatalog.ACTIONS) {
                String code = permissionCode(screen.code(), action);
                if (Permission.ALL.contains(code)) {
                    privileges.add(
                            new Privilege(
                                    code,
                                    action,
                                    PermissionCatalog.ACTION_LABELS.get(action),
                                    granted.contains(code),
                                    defaults.contains(code),
                                    !readOnly && defaults.contains(code),
                                    Permission.PROFILE_READ.equals(code)));
                }
            }
            screens.add(
                    new Screen(
                            screen.code(),
                            screen.label(),
                            screen.group(),
                            List.copyOf(privileges)));
        }
        return new Matrix(
                role,
                PermissionCatalog.ROLE_LABELS.get(role),
                version,
                readOnly,
                List.copyOf(screens));
    }

    private String resourceForPermission(String permission) {
        for (ScreenDefinition screen : PermissionCatalog.SCREENS) {
            for (String action : PermissionCatalog.ACTIONS) {
                if (permissionCode(screen.code(), action).equals(permission)) {
                    return screen.code();
                }
            }
        }
        throw BusinessException.invalid(
                ApiField.PERMISSIONS, PermissionMessage.UNKNOWN_SCREEN.formatted(permission));
    }

    private String permissionCode(String resource, String action) {
        return resource + DmsConstants.PERMISSION_SEPARATOR + action;
    }

    private String writePermissions(Set<String> permissions) {
        try {
            return mapper.writeValueAsString(permissions.stream().sorted().toList());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(PermissionMessage.SERIALIZATION_FAILED, exception);
        }
    }

    private Map<String, Object> auditValue(String role, long version, Set<String> permissions) {
        return Map.of(
                ApiField.ROLE,
                role,
                ApiField.VERSION,
                version,
                ApiField.PERMISSIONS,
                permissions.stream().sorted().toList());
    }

    private record StoredPermissions(String json, long version) {}
}
