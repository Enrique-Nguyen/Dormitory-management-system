package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.MaintenanceStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.maintenance.domain.MaintenanceRequest;
import vn.edu.tlu.dms.maintenance.domain.MaintenanceRequestPolicy;
import vn.edu.tlu.dms.maintenance.domain.MaintenanceRequestPolicy.Cause;

/**
 * Ma trận allowedActions của ticket sửa chữa theo trạng thái, vai trò và việc ticket đã kết luận
 * hay chưa (FR-MNT-01..10). Policy chỉ quyết định theo trạng thái; phạm vi đơn vị do Service lọc.
 */
class MaintenanceRequestPolicyTest {

    private static final Long REQUESTER = 10L;

    private static final Long ASSIGNEE = 30L;

    private static final Long UNIT = 1L;

    private static final Actor STUDENT = actor(REQUESTER, Role.STUDENT, null);

    private static final Actor OTHER_STUDENT = actor(11L, Role.STUDENT, null);

    private static final Actor STAFF = actor(20L, Role.STAFF, UNIT);

    private static final Actor TECHNICIAN = actor(ASSIGNEE, Role.TECHNICIAN, UNIT);

    private static final Actor ADMIN = actor(40L, Role.ADMIN, null);

    @Test
    void openTicketActionsRemainUnchanged() {
        var ticket = ticket(MaintenanceStatus.OPEN);
        assertThat(MaintenanceRequestPolicy.allowedActions(ticket, STAFF, false))
                .containsExactly(BusinessAction.ASSIGN, BusinessAction.REJECT);
        assertThat(MaintenanceRequestPolicy.allowedActions(ticket, STUDENT, false))
                .containsExactly(BusinessAction.CANCEL);
        assertThat(MaintenanceRequestPolicy.allowedActions(ticket, OTHER_STUDENT, false)).isEmpty();
        assertThat(MaintenanceRequestPolicy.allowedActions(ticket, TECHNICIAN, false)).isEmpty();
        assertThat(MaintenanceRequestPolicy.allowedActions(ticket, ADMIN, false))
                .containsExactly(
                        BusinessAction.ASSIGN, BusinessAction.REJECT, BusinessAction.CANCEL);
    }

    @Test
    void assignedAndInProgressActionsRemainUnchanged() {
        var assigned = ticket(MaintenanceStatus.ASSIGNED);
        assertThat(MaintenanceRequestPolicy.allowedActions(assigned, TECHNICIAN, false))
                .containsExactly(BusinessAction.START);
        assertThat(MaintenanceRequestPolicy.allowedActions(assigned, STAFF, false)).isEmpty();

        var inProgress = ticket(MaintenanceStatus.IN_PROGRESS);
        assertThat(MaintenanceRequestPolicy.allowedActions(inProgress, TECHNICIAN, false))
                .containsExactly(BusinessAction.ADD_NOTE, BusinessAction.RESOLVE);
        assertThat(MaintenanceRequestPolicy.allowedActions(inProgress, STUDENT, false)).isEmpty();
    }

    @Test
    void resolvedTicketActionsRemainUnchanged() {
        var resolved = ticket(MaintenanceStatus.RESOLVED);
        assertThat(MaintenanceRequestPolicy.allowedActions(resolved, STUDENT, false))
                .containsExactly(BusinessAction.ACCEPT, BusinessAction.REOPEN);
        assertThat(MaintenanceRequestPolicy.allowedActions(resolved, STAFF, true))
                .containsExactly(BusinessAction.ACCEPT, BusinessAction.REOPEN);
        assertThat(MaintenanceRequestPolicy.allowedActions(resolved, STAFF, false)).isEmpty();
    }

    @Test
    void onlyClosedRoomTicketWithoutCauseCanBeConcluded() {
        var closed = ticket(MaintenanceStatus.CLOSED);
        assertThat(MaintenanceRequestPolicy.allowedActions(closed, STAFF, false))
                .containsExactly(BusinessAction.CONCLUDE);
        assertThat(MaintenanceRequestPolicy.allowedActions(closed, ADMIN, false))
                .containsExactly(BusinessAction.CONCLUDE);
        assertThat(MaintenanceRequestPolicy.allowedActions(closed, STUDENT, false)).isEmpty();
        assertThat(MaintenanceRequestPolicy.allowedActions(closed, TECHNICIAN, false)).isEmpty();
    }

    @Test
    void concludedTicketCannotBeConcludedAgain() {
        var concluded = ticket(MaintenanceStatus.CLOSED);
        concluded.setCause(Cause.WEAR.name());
        assertThat(MaintenanceRequestPolicy.allowedActions(concluded, STAFF, false)).isEmpty();
        assertThat(MaintenanceRequestPolicy.allowedActions(concluded, ADMIN, false)).isEmpty();
    }

    @Test
    void commonAreaTicketCannotBeConcluded() {
        var commonArea = ticket(MaintenanceStatus.CLOSED);
        commonArea.setRoomId(null);
        commonArea.setBuildingId(5L);
        commonArea.setFloor((short) 2);
        assertThat(MaintenanceRequestPolicy.allowedActions(commonArea, STAFF, false)).isEmpty();
        assertThat(MaintenanceRequestPolicy.allowedActions(commonArea, ADMIN, false)).isEmpty();
    }

    @Test
    void openRoomTicketOffersNoConcludeAction() {
        var open = ticket(MaintenanceStatus.OPEN);
        assertThat(MaintenanceRequestPolicy.allowedActions(open, STAFF, false))
                .doesNotContain(BusinessAction.CONCLUDE);
        assertThat(MaintenanceRequestPolicy.allowedActions(open, ADMIN, false))
                .doesNotContain(BusinessAction.CONCLUDE);
    }

    @Test
    void requireActionRejectsConcludeWithCurrentStatus() {
        var closed = ticket(MaintenanceStatus.CLOSED);
        assertThatThrownBy(
                        () ->
                                MaintenanceRequestPolicy.requireAction(
                                        closed, STUDENT, false, BusinessAction.CONCLUDE))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> {
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
                            assertThat(exception.getDetails())
                                    .containsEntry("currentStatus", MaintenanceStatus.CLOSED);
                        });
        MaintenanceRequestPolicy.requireAction(
                closed, STAFF, false, BusinessAction.CONCLUDE);
    }

    @Test
    void causeFromAcceptsOnlyRegisteredCodes() {
        assertThat(Cause.from(Cause.WEAR.name())).isEqualTo(Cause.WEAR);
        assertThat(Cause.from("  " + Cause.MISUSE.name() + " ")).isEqualTo(Cause.MISUSE);
        assertThat(Cause.from(null)).isNull();
        assertThat(Cause.from("")).isNull();
        assertThat(Cause.from("misuse")).isNull();
        assertThat(Cause.from("OTHER")).isNull();
        assertThat(Cause.values()).containsExactly(Cause.WEAR, Cause.MISUSE);
    }

    private static MaintenanceRequest ticket(String status) {
        var request = new MaintenanceRequest();
        request.setId(100L);
        request.setRequesterId(REQUESTER);
        request.setRoomId(200L);
        request.setAssignedTo(ASSIGNEE);
        request.setStatus(status);
        return request;
    }

    private static Actor actor(Long id, String role, Long unitId) {
        return new Actor(id, role, unitId, false, false);
    }
}
