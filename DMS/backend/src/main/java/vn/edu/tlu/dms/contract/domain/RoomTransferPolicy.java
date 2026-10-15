package vn.edu.tlu.dms.contract.domain;

import org.springframework.http.HttpStatus;

import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.StayRequestStatus;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bảng chuyển trạng thái yêu cầu chuyển phòng (SRS mục 10, FR-CON-07): chỉ yêu cầu PENDING mới có
 * hành động. Sinh viên chủ yêu cầu được hủy; Staff cùng đơn vị của giường hiện tại và ADMIN được
 * duyệt hoặc từ chối.
 */
public final class RoomTransferPolicy {

    private RoomTransferPolicy() {}

    /**
     * Hành động hợp lệ theo vai trò và trạng thái; chưa lọc theo quyền đang cấp của vai trò.
     *
     * @param unitId đơn vị quản lý của giường sinh viên đang ở
     */
    public static List<String> allowedActions(
            String status, Long ownerStudentId, Long unitId, Actor actor) {
        if (!StayRequestStatus.PENDING.equals(status)) {
            return List.of();
        }
        if (actor.isAdmin()) {
            return List.of(BusinessAction.APPROVE, BusinessAction.REJECT);
        }
        if (actor.is(Role.STUDENT) && actor.id().equals(ownerStudentId)) {
            return List.of(BusinessAction.CANCEL);
        }
        if (actor.is(Role.STAFF) && Objects.equals(actor.unitId(), unitId)) {
            return List.of(BusinessAction.APPROVE, BusinessAction.REJECT);
        }
        return List.of();
    }

    /** Ném 409 INVALID_STATE_TRANSITION kèm trạng thái hiện tại khi hành động không hợp lệ. */
    public static void requireAction(
            String status, Long ownerStudentId, Long unitId, Actor actor, String action) {
        if (allowedActions(status, ownerStudentId, unitId, actor).contains(action)) {
            return;
        }
        throw new BusinessException(
                ErrorCode.INVALID_STATE_TRANSITION,
                HttpStatus.CONFLICT,
                Map.of("currentStatus", status));
    }
}
