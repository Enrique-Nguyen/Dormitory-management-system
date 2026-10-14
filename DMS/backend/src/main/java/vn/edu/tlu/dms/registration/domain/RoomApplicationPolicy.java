package vn.edu.tlu.dms.registration.domain;

import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;

import java.util.List;
import java.util.Objects;
import java.util.Map;

/**
 * Tập hành động đã có endpoint trong khung. Phân giường/giải phóng giữ chỗ sẽ bổ sung qua dịch vụ
 * hợp đồng.
 */
public final class RoomApplicationPolicy {

    private RoomApplicationPolicy() {}

    /**
     * @param hasLiveContract đơn đã sinh hợp đồng PENDING/ACTIVE; khi đó sinh viên không được hủy
     *     đơn vì sẽ để lại hợp đồng mồ côi (phải xử lý hợp đồng trước)
     */
    public static List<String> allowedActions(
            RoomApplication application, Actor actor, boolean hasLiveContract) {
        if (actor.is("STUDENT") && actor.id().equals(application.getStudentId())) {
            return switch (application.getStatus()) {
                case "DRAFT" -> List.of("edit", "submit", "cancel");
                case "SUBMITTED" -> List.of("cancel");
                case "APPROVED" -> hasLiveContract ? List.of() : List.of("cancel");
                default -> List.of();
            };
        }
        if (actor.is("STAFF")
                && Objects.equals(actor.unitId(), application.getPreferredUnitId())
                && application.getStatus().equals("SUBMITTED")) {
            return List.of("approve", "reject");
        }
        return List.of();
    }

    public static void requireAction(
            RoomApplication application, Actor actor, String action, boolean hasLiveContract) {
        if (!allowedActions(application, actor, hasLiveContract).contains(action)) {
            throw new BusinessException(
                    "INVALID_STATE_TRANSITION",
                    409,
                    Map.of("currentStatus", application.getStatus()));
        }
    }
}
