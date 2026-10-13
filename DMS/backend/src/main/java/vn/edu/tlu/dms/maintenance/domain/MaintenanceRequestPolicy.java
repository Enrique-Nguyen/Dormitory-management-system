package vn.edu.tlu.dms.maintenance.domain;

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
        return switch (request.getStatus()) {
            case "OPEN" ->
                    actor.is("STAFF")
                            ? List.of("assign", "reject")
                            : actor.is("STUDENT") && actor.id().equals(request.getRequesterId())
                                    ? List.of("cancel")
                                    : List.of();
            case "ASSIGNED" ->
                    actor.is("TECHNICIAN") && actor.id().equals(request.getAssignedTo())
                            ? List.of("start")
                            : List.of();
            case "IN_PROGRESS" ->
                    actor.is("TECHNICIAN") && actor.id().equals(request.getAssignedTo())
                            ? List.of("add-note", "resolve")
                            : List.of();
            case "RESOLVED" ->
                    actor.is("STUDENT") && actor.id().equals(request.getRequesterId())
                                    || actor.is("STAFF") && staffCanAccept
                            ? List.of("accept", "reopen")
                            : List.of();
            default -> List.of();
        };
    }

    public static void requireAction(
            MaintenanceRequest request, Actor actor, boolean staffCanAccept, String action) {
        if (!allowedActions(request, actor, staffCanAccept).contains(action)) {
            throw new BusinessException(
                    "INVALID_STATE_TRANSITION", 409, Map.of("currentStatus", request.getStatus()));
        }
    }
}
