package vn.edu.tlu.dms.registration.domain;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApplicationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
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
        // ADMIN quản lý đơn của mọi sinh viên, nhưng vẫn phải tuân theo trạng thái nghiệp vụ.
        if (actor.isAdmin()) {
            return switch (application.getStatus()) {
                case ApplicationStatus.DRAFT ->
                        List.of(BusinessAction.EDIT, BusinessAction.SUBMIT, BusinessAction.CANCEL);
                case ApplicationStatus.SUBMITTED ->
                        List.of(
                                BusinessAction.APPROVE,
                                BusinessAction.REJECT,
                                BusinessAction.CANCEL);
                case ApplicationStatus.APPROVED -> List.of(BusinessAction.CANCEL);
                default -> List.of();
            };
        }
        if (actor.is(Role.STUDENT) && actor.id().equals(application.getStudentId())) {
            return switch (application.getStatus()) {
                case ApplicationStatus.DRAFT ->
                        List.of(BusinessAction.EDIT, BusinessAction.SUBMIT, BusinessAction.CANCEL);
                case ApplicationStatus.SUBMITTED, ApplicationStatus.APPROVED ->
                        List.of(BusinessAction.CANCEL);
                default -> List.of();
            };
        }
        if (actor.is(Role.STAFF)
                && application.getPreferredUnitId().equals(actor.unitId())
                && application.getStatus().equals(ApplicationStatus.SUBMITTED)) {
            return List.of(BusinessAction.APPROVE, BusinessAction.REJECT);
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
