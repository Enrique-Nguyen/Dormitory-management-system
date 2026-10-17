package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.StayRequestStatus;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.contract.domain.RoomTransferPolicy;

import java.util.List;

/** Bảng chuyển trạng thái yêu cầu chuyển phòng theo vai trò và trạng thái (FR-CON-07). */
class RoomTransferPolicyTest {

    private static final Long OWNER = 10L;

    private static final Long UNIT = 1L;

    @Test
    void pendingRequestActionsDependOnRoleOwnerAndUnit() {
        assertThat(pending(actor(OWNER, Role.STUDENT, null)))
                .containsExactly(BusinessAction.CANCEL);
        assertThat(pending(actor(11L, Role.STUDENT, null)))
                .isEmpty();
        assertThat(pending(actor(20L, Role.STAFF, UNIT)))
                .containsExactly(BusinessAction.APPROVE, BusinessAction.REJECT);
        assertThat(pending(actor(21L, Role.STAFF, 2L)))
                .isEmpty();
        assertThat(pending(actor(30L, Role.ADMIN, null)))
                .containsExactly(BusinessAction.APPROVE, BusinessAction.REJECT);
        assertThat(pending(actor(40L, Role.TECHNICIAN, UNIT)))
                .isEmpty();
    }

    @Test
    void decidedRequestsHaveNoActions() {
        for (String status :
                new String[] {
                    StayRequestStatus.APPROVED,
                    StayRequestStatus.REJECTED,
                    StayRequestStatus.CANCELLED
                }) {
            assertThat(RoomTransferPolicy.allowedActions(
                                    status, OWNER, UNIT, actor(30L, Role.ADMIN, null)))
                    .isEmpty();
            assertThat(RoomTransferPolicy.allowedActions(
                                    status, OWNER, UNIT, actor(OWNER, Role.STUDENT, null)))
                    .isEmpty();
        }
    }

    @Test
    void requireActionRejectsWithCurrentStatus() {
        assertThatThrownBy(
                        () ->
                                RoomTransferPolicy.requireAction(
                                        StayRequestStatus.REJECTED,
                                        OWNER,
                                        UNIT,
                                        actor(20L, Role.STAFF, UNIT),
                                        BusinessAction.APPROVE))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> {
                            assertThat(e.getErrorCode())
                                    .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
                            assertThat(e.getDetails())
                                    .containsEntry("currentStatus", StayRequestStatus.REJECTED);
                        });
        RoomTransferPolicy.requireAction(
                StayRequestStatus.PENDING,
                OWNER,
                UNIT,
                actor(OWNER, Role.STUDENT, null),
                BusinessAction.CANCEL);
    }

    private static List<String> pending(Actor actor) {
        return RoomTransferPolicy.allowedActions(StayRequestStatus.PENDING, OWNER, UNIT, actor);
    }

    private static Actor actor(Long id, String role, Long unitId) {
        return new Actor(id, role, unitId, false, false);
    }
}
