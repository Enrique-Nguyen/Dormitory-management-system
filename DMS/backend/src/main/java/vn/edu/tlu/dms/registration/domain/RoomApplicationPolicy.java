package vn.edu.tlu.dms.registration.domain;

import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;

import java.util.List;
import java.util.Map;

/**
 * Tập hành động đã có endpoint trong khung. Phân giường/giải phóng giữ chỗ sẽ bổ sung qua dịch vụ
 * hợp đồng.
 */
public final class RoomApplicationPolicy {

    private RoomApplicationPolicy() {}

    public static List<String> allowedActions(RoomApplication application, Actor actor) {
        if (actor.is("STUDENT") && actor.id().equals(application.getStudentId())) {
            return switch (application.getStatus()) {
                case "DRAFT" -> List.of("edit", "submit", "cancel");
                case "SUBMITTED", "APPROVED" -> List.of("cancel");
                default -> List.of();
            };
        }
        if (actor.is("STAFF")
                && actor.unitId().equals(application.getPreferredUnitId())
                && application.getStatus().equals("SUBMITTED")) {
            return List.of("approve", "reject");
        }
        return List.of();
    }

    public static void requireAction(RoomApplication application, Actor actor, String action) {
        if (!allowedActions(application, actor).contains(action)) {
            throw new BusinessException(
                    "INVALID_STATE_TRANSITION",
                    409,
                    Map.of("currentStatus", application.getStatus()));
        }
    }
}
