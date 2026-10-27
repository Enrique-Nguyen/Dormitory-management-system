package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApplicationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.registration.domain.RoomApplication;
import vn.edu.tlu.dms.registration.domain.RoomApplicationPolicy;

/**
 * Ma trận allowedActions của đơn đăng ký theo trạng thái, vai trò, hợp đồng còn sống và tình trạng
 * giữ chỗ (SRS mục 10, API mục 6): duyệt/từ chối, xác nhận diện ưu tiên, phân giường, hủy giữ chỗ
 * và hủy đơn. Policy không kiểm tra đơn vị; Service kiểm tra phạm vi trước khi gọi.
 */
class RoomApplicationPolicyTest {

    private static final Long STUDENT_ID = 10L;

    private static final Long UNIT = 1L;

    private static final Actor STUDENT = new Actor(STUDENT_ID, Role.STUDENT, null, false, false);

    private static final Actor OTHER_STUDENT =
            new Actor(11L, Role.STUDENT, null, false, false);

    private static final Actor STAFF = new Actor(20L, Role.STAFF, UNIT, false, false);

    private static final Actor OTHER_UNIT_STAFF =
            new Actor(21L, Role.STAFF, 2L, false, false);

    private static final Actor ADMIN = new Actor(40L, Role.ADMIN, null, false, false);

    @Test
    void submittedApplicationLetsStaffApproveRejectAndVerifyPriority() {
        var application = application(ApplicationStatus.SUBMITTED);
        assertThat(RoomApplicationPolicy.allowedActions(application, STAFF, false, false))
                .containsExactly(
                        BusinessAction.APPROVE,
                        BusinessAction.REJECT,
                        BusinessAction.VERIFY_PRIORITY);
        assertThat(RoomApplicationPolicy.allowedActions(application, ADMIN, false, false))
                .contains(
                        BusinessAction.APPROVE,
                        BusinessAction.REJECT,
                        BusinessAction.VERIFY_PRIORITY,
                        BusinessAction.CANCEL);
        assertThat(RoomApplicationPolicy.allowedActions(application, STUDENT, false, false))
                .containsExactly(BusinessAction.CANCEL);
        assertThat(RoomApplicationPolicy.allowedActions(application, OTHER_STUDENT, false, false))
                .isEmpty();
        assertThat(RoomApplicationPolicy.allowedActions(application, OTHER_UNIT_STAFF, false, false))
                .isEmpty();
    }

    @Test
    void approvedApplicationWithoutContractCanBeAssigned() {
        var application = application(ApplicationStatus.APPROVED);
        assertThat(RoomApplicationPolicy.allowedActions(application, STAFF, false, false))
                .containsExactly(BusinessAction.ASSIGN);
        assertThat(RoomApplicationPolicy.allowedActions(application, ADMIN, false, false))
                .containsExactly(BusinessAction.ASSIGN, BusinessAction.CANCEL);
        assertThat(RoomApplicationPolicy.allowedActions(application, STUDENT, false, false))
                .containsExactly(BusinessAction.CANCEL);
    }

    @Test
    void approvedApplicationWithLiveContractCannotBeAssignedOrCancelled() {
        var application = application(ApplicationStatus.APPROVED);
        assertThat(RoomApplicationPolicy.allowedActions(application, STAFF, true, true)).isEmpty();
        assertThat(RoomApplicationPolicy.allowedActions(application, STUDENT, true, true)).isEmpty();
        assertThat(RoomApplicationPolicy.allowedActions(application, ADMIN, true, true)).isEmpty();
    }

    @Test
    void assignedApplicationWithHoldCanBeReleased() {
        var application = application(ApplicationStatus.ASSIGNED);
        assertThat(RoomApplicationPolicy.allowedActions(application, STAFF, true, true))
                .containsExactly(BusinessAction.RELEASE_HOLD);
        assertThat(RoomApplicationPolicy.allowedActions(application, STUDENT, true, true))
                .containsExactly(BusinessAction.CANCEL);
        assertThat(RoomApplicationPolicy.allowedActions(application, ADMIN, true, true))
                .containsExactly(BusinessAction.RELEASE_HOLD, BusinessAction.CANCEL);
    }

    @Test
    void assignedApplicationWithActiveContractOffersNoReleaseOrCancel() {
        var application = application(ApplicationStatus.ASSIGNED);
        assertThat(RoomApplicationPolicy.allowedActions(application, STAFF, true, false)).isEmpty();
        assertThat(RoomApplicationPolicy.allowedActions(application, STUDENT, true, false)).isEmpty();
        assertThat(RoomApplicationPolicy.allowedActions(application, ADMIN, true, false)).isEmpty();
    }

    @Test
    void cancelledOrRejectedApplicationHasNoActions() {
        assertThat(
                        RoomApplicationPolicy.allowedActions(
                                application(ApplicationStatus.CANCELLED), ADMIN, false, false))
                .isEmpty();
        assertThat(
                        RoomApplicationPolicy.allowedActions(
                                application(ApplicationStatus.REJECTED), STAFF, false, false))
                .isEmpty();
    }

    @Test
    void requireActionReportsContractInsteadOfPlainStatusConflict() {
        var approved = application(ApplicationStatus.APPROVED);
        assertThatThrownBy(
                        () ->
                                RoomApplicationPolicy.requireAction(
                                        approved, STAFF, BusinessAction.ASSIGN, true, true))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.APPLICATION_HAS_CONTRACT));
        assertThatThrownBy(
                        () ->
                                RoomApplicationPolicy.requireAction(
                                        approved, STUDENT, BusinessAction.CANCEL, true, true))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.APPLICATION_HAS_CONTRACT));

        var assigned = application(ApplicationStatus.ASSIGNED);
        assertThatThrownBy(
                        () ->
                                RoomApplicationPolicy.requireAction(
                                        assigned, STAFF, BusinessAction.RELEASE_HOLD, true, false))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.APPLICATION_HAS_CONTRACT));
        assertThatThrownBy(
                        () ->
                                RoomApplicationPolicy.requireAction(
                                        assigned, STAFF, BusinessAction.ASSIGN, true, true))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
    }

    private static RoomApplication application(String status) {
        var application = new RoomApplication();
        application.setId(88L);
        application.setStudentId(STUDENT_ID);
        application.setPeriodId(12L);
        application.setPreferredUnitId(UNIT);
        application.setStatus(status);
        application.setPriorityScore(0);
        return application;
    }
}
