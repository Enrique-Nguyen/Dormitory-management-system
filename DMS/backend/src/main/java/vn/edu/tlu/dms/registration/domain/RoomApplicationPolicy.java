package vn.edu.tlu.dms.registration.domain;

import org.springframework.http.HttpStatus;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApplicationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bảng chuyển trạng thái của đơn đăng ký (SRS mục 10): duyệt/từ chối, xác nhận diện ưu tiên, phân
 * giường, hủy giữ chỗ và hủy đơn. Policy chỉ quyết định theo trạng thái, vai trò và tình trạng giữ
 * chỗ; phạm vi đơn vị do Service kiểm tra trước khi gọi.
 */
public final class RoomApplicationPolicy {

    private RoomApplicationPolicy() {}

    /**
     * Hành động khả dụng theo vai trò, trạng thái đơn, hợp đồng còn sống và giường đang giữ.
     *
     * <ul>
     *   <li>Đơn APPROVED chưa có hợp đồng PENDING/ACTIVE: Staff cùng đơn vị được {@code assign};
     *       đã có hợp đồng thì không phân giường hoặc hủy trực tiếp để tránh hợp đồng mồ côi.
     *   <li>Đơn ASSIGNED còn HOLD: Staff được {@code release-hold}, người sở hữu/Admin được hủy.
     *       Hợp đồng đã ACTIVE (không còn HOLD) thì không mở release-hold hay hủy.
     *   <li>Đơn SUBMITTED: Staff cùng đơn vị được duyệt, từ chối và xác nhận diện ưu tiên.
     * </ul>
     *
     * @param hasLiveContract đơn đã có hợp đồng PENDING/ACTIVE liên kết
     * @param holdActive hợp đồng còn phân phòng HOLD đang mở (chưa kích hoạt)
     */
    public static List<String> allowedActions(
            RoomApplication application,
            Actor actor,
            boolean hasLiveContract,
            boolean holdActive) {
        var actions = new ArrayList<>(roleActions(application, actor));
        if (ApplicationStatus.APPROVED.equals(application.getStatus()) && hasLiveContract) {
            actions.removeAll(List.of(BusinessAction.ASSIGN, BusinessAction.CANCEL));
        }
        if (ApplicationStatus.ASSIGNED.equals(application.getStatus()) && !holdActive) {
            actions.removeAll(List.of(BusinessAction.RELEASE_HOLD, BusinessAction.CANCEL));
        }
        return List.copyOf(actions);
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
                                BusinessAction.VERIFY_PRIORITY,
                                BusinessAction.CANCEL);
                case ApplicationStatus.APPROVED ->
                        List.of(BusinessAction.ASSIGN, BusinessAction.CANCEL);
                case ApplicationStatus.ASSIGNED ->
                        List.of(BusinessAction.RELEASE_HOLD, BusinessAction.CANCEL);
                default -> List.of();
            };
        }
        if (actor.is(Role.STUDENT) && actor.id().equals(application.getStudentId())) {
            return switch (application.getStatus()) {
                case ApplicationStatus.DRAFT ->
                        List.of(BusinessAction.EDIT, BusinessAction.SUBMIT, BusinessAction.CANCEL);
                case ApplicationStatus.SUBMITTED,
                                ApplicationStatus.APPROVED,
                                ApplicationStatus.ASSIGNED ->
                        List.of(BusinessAction.CANCEL);
                default -> List.of();
            };
        }
        if (actor.is(Role.STAFF)
                && Objects.equals(application.getPreferredUnitId(), actor.unitId())) {
            return switch (application.getStatus()) {
                case ApplicationStatus.SUBMITTED ->
                        List.of(
                                BusinessAction.APPROVE,
                                BusinessAction.REJECT,
                                BusinessAction.VERIFY_PRIORITY);
                case ApplicationStatus.APPROVED -> List.of(BusinessAction.ASSIGN);
                case ApplicationStatus.ASSIGNED -> List.of(BusinessAction.RELEASE_HOLD);
                default -> List.of();
            };
        }
        return List.of();
    }

    /**
     * Kiểm tra hành động có nằm trong bảng chuyển trạng thái hay không. Trường hợp người dùng có
     * quyền hủy/giải phóng giữ chỗ nhưng hợp đồng đã sống hoặc chưa có HOLD thì trả mã lỗi riêng
     * để giao diện hiển thị đúng nguyên nhân thay vì chỉ báo sai trạng thái.
     */
    public static void requireAction(
            RoomApplication application,
            Actor actor,
            String action,
            boolean hasLiveContract,
            boolean holdActive) {
        if (allowedActions(application, actor, hasLiveContract, holdActive).contains(action)) {
            return;
        }
        var baseActions = roleActions(application, actor);
        if (BusinessAction.CANCEL.equals(action)
                && hasLiveContract
                && baseActions.contains(action)) {
            throw BusinessException.conflict(ErrorCode.APPLICATION_HAS_CONTRACT);
        }
        if (BusinessAction.ASSIGN.equals(action)
                && hasLiveContract
                && baseActions.contains(action)) {
            throw BusinessException.conflict(ErrorCode.APPLICATION_HAS_CONTRACT);
        }
        if (BusinessAction.RELEASE_HOLD.equals(action) && baseActions.contains(action)) {
            throw BusinessException.conflict(
                    hasLiveContract ? ErrorCode.APPLICATION_HAS_CONTRACT
                            : ErrorCode.NO_HOLD_ASSIGNMENT);
        }
        throw new BusinessException(
                ErrorCode.INVALID_STATE_TRANSITION,
                HttpStatus.CONFLICT,
                Map.of(ApiField.CURRENT_STATUS, application.getStatus()));
    }
}
