package vn.edu.tlu.dms.account.api;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.security.RequirePrivilege;
import vn.edu.tlu.dms.common.service.RolePermissionService;
import vn.edu.tlu.dms.common.service.RolePermissionService.Matrix;
import vn.edu.tlu.dms.common.service.RolePermissionService.Update;

@RestController
@RequestMapping("/api/admin/role-permissions")
public class RolePermissionController {

    private final RolePermissionService service;

    public RolePermissionController(RolePermissionService service) {
        this.service = service;
    }

    /** Xem ma trận quyền theo vai trò; kiểm tra ADMIN tiếp tục thực hiện trong Service Impl. */
    @RequirePrivilege(Permission.ROLE_PERMISSION_READ)
    @GetMapping("/{role}")
    public Matrix getRolePermissionMatrix(@PathVariable String role) {
        return service.getRolePermissionMatrix(role);
    }

    /** Lưu các ô đã tick với phiên bản đã đọc; backend áp dụng ở request tiếp theo. */
    @RequirePrivilege(Permission.ROLE_PERMISSION_UPDATE)
    @PutMapping("/{role}")
    public Matrix updateRolePermissions(
            @PathVariable String role, @Valid @RequestBody Update input) {
        return service.updateRolePermissions(role, input);
    }
}
