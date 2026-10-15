package vn.edu.tlu.dms.common.security;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.constants.DmsConstants.Action;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Resource;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.service.RolePermissionService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ScopeGuard {

    /**
     * Ma trận Role → quyền thao tác, tham khảo RolePrivilege của V5. READ gồm tìm kiếm/xem chi
     * tiết; CREATE/UPDATE/DELETE tách riêng. Quyền thao tác không thay thế phạm vi và trạng thái ở
     * Service Impl. Ma trận này chỉ là cấu hình mặc định và giới hạn quyền phù hợp của từng vai
     * trò; quyền đang có hiệu lực được RolePermissionService đọc từ CSDL.
     */
    private static final Map<String, Set<String>> DEFAULT_ROLE_PERMISSIONS =
            createDefaultRolePermissions();

    private final CurrentUser currentUser;

    private final RolePermissionService rolePermissions;

    public ScopeGuard(CurrentUser currentUser, RolePermissionService rolePermissions) {
        this.currentUser = currentUser;
        this.rolePermissions = rolePermissions;
    }

    public void requireRole(String... roles) {
        Actor actor = currentUser.get();
        if (actor.isAdmin()) {
            return;
        }
        for (String role : roles) {
            if (actor.is(role)) {
                return;
            }
        }
        throw BusinessException.forbidden();
    }

    public void requireUnit(Long unitId) {
        Actor actor = currentUser.get();
        if (actor.isAdmin()) {
            return;
        }
        if (actor.unitId() == null || !actor.unitId().equals(unitId)) {
            throw BusinessException.notFound();
        }
    }

    /**
     * Quyền tổng hợp chỉ mở rộng phạm vi đọc; ngoại lệ tạo đợt nhiều đơn vị được quy định tại API
     * 4.2.
     */
    public void requireReadableUnit(Long unitId) {
        Actor actor = currentUser.get();
        if (actor.canReadAllUnits()) {
            return;
        }
        requireUnit(unitId);
    }

    /** Quyền mặc định/giới hạn của vai trò; không dùng tập này để cấp quyền đang có hiệu lực. */
    public static List<String> permissionsForRole(String role) {
        return DEFAULT_ROLE_PERMISSIONS.getOrDefault(role, Set.of()).stream().sorted().toList();
    }

    /** Kiểm tra đúng mã quyền đã đăng ký; Admin cũng không được dùng mã quyền sai/chưa tồn tại. */
    public boolean hasPermission(String permission) {
        return rolePermissions.getGrantedPermissions(currentUser.get().role()).contains(permission);
    }

    /** Thiếu quyền thao tác trả 403; phạm vi đối tượng vẫn được kiểm tra riêng sau đó. */
    public void requirePermission(String permission) {
        if (!hasPermission(permission)) {
            throw BusinessException.forbidden();
        }
    }

    /**
     * Spring Security gọi trước thân Controller. Đọc annotation từ hàm thật của proxy, sau đó mới
     * dùng khai báo lớp nếu hàm chưa khai. Thiếu/rỗng quyền luôn bị chặn, không mặc định cho qua.
     */
    public boolean hasDeclaredPrivilege(MethodInvocation invocation) {
        Class<?> targetClass = ClassUtils.getUserClass(invocation.getThis());
        var method = AopUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
        var required = AnnotatedElementUtils.findMergedAnnotation(method, RequirePrivilege.class);
        if (required == null) {
            required =
                    AnnotatedElementUtils.findMergedAnnotation(targetClass, RequirePrivilege.class);
        }
        if (required == null || required.value().length == 0) {
            return false;
        }
        var granted = rolePermissions.getGrantedPermissions(currentUser.get().role());
        for (String permission : required.value()) {
            if (granted.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Set<String>> createDefaultRolePermissions() {
        Set<String> shared =
                Set.of(
                        Permission.PROFILE_READ,
                        Permission.PROFILE_UPDATE,
                        Permission.MANAGEMENT_UNIT_READ,
                        Permission.BUILDING_READ,
                        Permission.ROOM_TYPE_READ,
                        Permission.SEMESTER_READ,
                        Permission.PRIORITY_CATEGORY_READ,
                        Permission.REGISTRATION_PERIOD_READ,
                        Permission.NOTIFICATION_READ);
        var staff = new LinkedHashSet<>(shared);
        for (String resource :
                List.of(
                        Resource.ROOM_TYPE,
                        Resource.SEMESTER,
                        Resource.ROOM,
                        Resource.BED,
                        Resource.REGISTRATION_PERIOD,
                        Resource.ROOM_APPLICATION,
                        Resource.CONTRACT,
                        Resource.CONTRACT_EXTENSION,
                        Resource.ROOM_TRANSFER,
                        Resource.MOVE_OUT,
                        Resource.SUMMER_REGISTRATION,
                        Resource.STORAGE,
                        Resource.UTILITY_RATE,
                        Resource.UTILITY_READING,
                        Resource.INVOICE,
                        Resource.DEPOSIT,
                        Resource.MAINTENANCE,
                        Resource.NOTIFICATION)) {
            grantCrud(staff, resource);
        }
        staff.addAll(
                Set.of(
                        Permission.STUDENT_READ,
                        Permission.STUDENT_UPDATE,
                        Permission.STUDENT_RESET_PASSWORD,
                        Permission.DASHBOARD_READ));
        // Đơn/yêu cầu do sinh viên tạo: Staff xử lý, không tạo hoặc hủy thay sinh viên.
        staff.removeAll(
                Set.of(
                        Permission.ROOM_APPLICATION_CREATE,
                        Permission.ROOM_APPLICATION_DELETE,
                        Permission.CONTRACT_EXTENSION_CREATE,
                        Permission.CONTRACT_EXTENSION_DELETE,
                        Permission.ROOM_TRANSFER_CREATE,
                        Permission.ROOM_TRANSFER_DELETE,
                        Permission.SUMMER_REGISTRATION_CREATE,
                        Permission.SUMMER_REGISTRATION_UPDATE,
                        Permission.SUMMER_REGISTRATION_DELETE));
        var student = new LinkedHashSet<>(shared);
        for (String resource :
                List.of(
                        Resource.ROOM_APPLICATION,
                        Resource.CONTRACT_EXTENSION,
                        Resource.ROOM_TRANSFER,
                        Resource.SUMMER_REGISTRATION,
                        Resource.MAINTENANCE)) {
            grantCrud(student, resource);
        }
        student.addAll(
                Set.of(
                        Permission.ROOM_READ,
                        Permission.BED_READ,
                        Permission.CONTRACT_READ,
                        Permission.INVOICE_READ,
                        Permission.DEPOSIT_READ,
                        Permission.STORAGE_READ));
        var technician = new LinkedHashSet<>(shared);
        technician.addAll(Set.of(Permission.MAINTENANCE_READ, Permission.MAINTENANCE_UPDATE));
        // ADMIN lấy toàn bộ danh mục, kể cả quyền mới; ma trận động không thể thu hồi quyền ADMIN.
        return Map.of(
                Role.ADMIN,
                Permission.ALL,
                Role.STAFF,
                Set.copyOf(staff),
                Role.STUDENT,
                Set.copyOf(student),
                Role.TECHNICIAN,
                Set.copyOf(technician));
    }

    private static void grantCrud(Set<String> permissions, String resource) {
        for (String action : Action.CRUD) {
            permissions.add(resource + DmsConstants.PERMISSION_SEPARATOR + action);
        }
    }
}
