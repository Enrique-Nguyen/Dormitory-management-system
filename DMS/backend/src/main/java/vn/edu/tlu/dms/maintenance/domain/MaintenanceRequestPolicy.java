package vn.edu.tlu.dms.maintenance.domain;

import org.springframework.http.HttpStatus;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.MaintenanceStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;

import java.util.List;
import java.util.Map;

/**
 * Policy được dùng chung khi dựng allowedActions và khi xử lý lệnh; UI không quyết định quyền
 * nghiệp vụ.
 */
public final class MaintenanceRequestPolicy {

    private MaintenanceRequestPolicy() {}

    public static List<String> allowedActions(
            MaintenanceRequest request, Actor actor, boolean staffCanAccept) {
        // ADMIN thực hiện mọi hành động phù hợp trạng thái; lịch sử vẫn ghi chính ADMIN.
        if (actor.isAdmin()) {
            return switch (request.getStatus()) {
                case MaintenanceStatus.OPEN ->
                        List.of(
                                BusinessAction.ASSIGN,
                                BusinessAction.REJECT,
                                BusinessAction.CANCEL);
                case MaintenanceStatus.ASSIGNED -> List.of(BusinessAction.START);
                case MaintenanceStatus.IN_PROGRESS ->
                        List.of(BusinessAction.ADD_NOTE, BusinessAction.RESOLVE);
                case MaintenanceStatus.RESOLVED ->
                        List.of(BusinessAction.ACCEPT, BusinessAction.REOPEN);
                default -> List.of();
            };
        }
        return switch (request.getStatus()) {
            case MaintenanceStatus.OPEN ->
                    actor.is(Role.STAFF)
                            ? List.of(BusinessAction.ASSIGN, BusinessAction.REJECT)
                            : actor.is(Role.STUDENT) && actor.id().equals(request.getRequesterId())
                                    ? List.of(BusinessAction.CANCEL)
                                    : List.of();
            case MaintenanceStatus.ASSIGNED ->
                    actor.is(Role.TECHNICIAN) && actor.id().equals(request.getAssignedTo())
                            ? List.of(BusinessAction.START)
                            : List.of();
            case MaintenanceStatus.IN_PROGRESS ->
                    actor.is(Role.TECHNICIAN) && actor.id().equals(request.getAssignedTo())
                            ? List.of(BusinessAction.ADD_NOTE, BusinessAction.RESOLVE)
                            : List.of();
            case MaintenanceStatus.RESOLVED ->
                    actor.is(Role.STUDENT) && actor.id().equals(request.getRequesterId())
                                    || actor.is(Role.STAFF) && staffCanAccept
                            ? List.of(BusinessAction.ACCEPT, BusinessAction.REOPEN)
                            : List.of();
            default -> List.of();
        };
    }

    public static void requireAction(
            MaintenanceRequest request, Actor actor, boolean staffCanAccept, String action) {
        if (!allowedActions(request, actor, staffCanAccept).contains(action)) {
            throw new BusinessException(
                    ErrorCode.INVALID_STATE_TRANSITION,
                    HttpStatus.CONFLICT,
                    Map.of(ApiField.CURRENT_STATUS, request.getStatus()));
        }
    }
}
