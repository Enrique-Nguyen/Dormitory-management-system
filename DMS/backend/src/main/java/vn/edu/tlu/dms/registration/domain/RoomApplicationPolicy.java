package vn.edu.tlu.dms.registration.domain;

import org.springframework.http.HttpStatus;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApplicationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Tập hành động đã có endpoint trong khung. Phân giường/giải phóng giữ chỗ sẽ bổ sung qua dịch vụ
 * hợp đồng.
 */
public final class RoomApplicationPolicy {

    private RoomApplicationPolicy() {}

    /**
     * Hành động theo vai trò và trạng thái đơn. Đơn APPROVED đã sinh hợp đồng PENDING/ACTIVE không
     * được hủy trực tiếp vì sẽ để lại hợp đồng mồ côi (phải xử lý hợp đồng trước).
     *
     * @param hasLiveContract đơn đã có hợp đồng PENDING/ACTIVE liên kết
     */
    public static List<String> allowedActions(
            RoomApplication application, Actor actor, boolean hasLiveContract) {
        var actions = roleActions(application, actor);
        if (hasLiveContract && ApplicationStatus.APPROVED.equals(application.getStatus())) {
            return actions.stream().filter(action -> !BusinessAction.CANCEL.equals(action)).toList();
        }
        return actions;
    }

    private static List<String> roleActions(RoomApplication application, Actor actor) {
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
                && Objects.equals(application.getPreferredUnitId(), actor.unitId())
                && application.getStatus().equals(ApplicationStatus.SUBMITTED)) {
            return List.of(BusinessAction.APPROVE, BusinessAction.REJECT);
        }
        return List.of();
    }

    public static void requireAction(
            RoomApplication application, Actor actor, String action, boolean hasLiveContract) {
        if (allowedActions(application, actor, hasLiveContract).contains(action)) {
            return;
        }
        // Người được phép hủy nhưng đơn còn hợp đồng sống: báo rõ nguyên nhân thay vì chuyển trạng thái.
        if (BusinessAction.CANCEL.equals(action)
                && hasLiveContract
                && roleActions(application, actor).contains(action)) {
            throw BusinessException.conflict(ErrorCode.APPLICATION_HAS_CONTRACT);
        }
        throw new BusinessException(
                ErrorCode.INVALID_STATE_TRANSITION,
                HttpStatus.CONFLICT,
                Map.of(ApiField.CURRENT_STATUS, application.getStatus()));
    }
}
