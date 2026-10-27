package vn.edu.tlu.dms.registration.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.SemesterType;
import vn.edu.tlu.dms.common.constants.DmsConstants.SummerChoice;
import vn.edu.tlu.dms.common.constants.DmsConstants.SummerStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.contract.service.ContractService;
import vn.edu.tlu.dms.registration.service.SummerCrudService;
import vn.edu.tlu.dms.room.service.RoomReferenceService;
import vn.edu.tlu.dms.support.BusinessFailures;
import vn.edu.tlu.dms.support.FakeJdbc;
import vn.edu.tlu.dms.support.FakeSqlPage;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Nghiệp vụ xét duyệt lựa chọn hè (FR-REG-06, BR-11): chỉ bản ghi PENDING mới quyết định được; STAY
 * phải tạo bản gia hạn qua hè trên hợp đồng ACTIVE, TEMP_LEAVE/LEAVE chỉ đổi trạng thái. Test thuần,
 * không Spring context và không CSDL.
 */
class SummerChoiceApprovalTest {

    private static final Long CHOICE_ID = 7L;

    private static final Long CONTRACT_ID = 21L;

    private static final Long STUDENT_ID = 10L;

    private static final Long UNIT_ID = 1L;

    private static final Long STAFF_ID = 20L;

    private static final Long SEMESTER_ID = 5L;

    private static final LocalDate CONTRACT_END = LocalDate.of(2026, 6, 30);

    private static final LocalDate SEMESTER_END = LocalDate.of(2026, 8, 31);

    /** Hợp đồng ACTIVE và học kỳ hè kết thúc sau hợp đồng: điều kiện đủ để duyệt mọi lựa chọn. */
    private final Fixture ready = new Fixture();

    @Test
    void stayApprovalCreatesSummerExtensionOnTheContract() {
        ready.givenChoice(SummerStatus.PENDING, SummerChoice.STAY);

        ready.service.approve(CHOICE_ID);

        verify(ready.contracts)
                .recordSummerExtension(CONTRACT_ID, SEMESTER_END, STAFF_ID, null);
        // recordSummerExtension đã tự gia hạn end_date; gọi thêm extendEndDate sẽ gia hạn hai lần.
        verify(ready.contracts, never()).extendEndDate(any(), any(), any());
        assertThat(ready.jdbc.writeContaining("UPDATE summer_choice SET status=?"))
                .as("Lựa chọn hè phải chuyển sang APPROVED trong cùng transaction")
                .contains(SummerStatus.APPROVED)
                .contains("| " + CHOICE_ID);
        verify(ready.history)
                .record(
                        eq(EntityType.SUMMER_CHOICE),
                        eq(CHOICE_ID),
                        eq(SummerStatus.PENDING),
                        eq(SummerStatus.APPROVED),
                        eq(STAFF_ID),
                        isNull());
        verify(ready.audit)
                .record(
                        eq(STAFF_ID),
                        eq(AuditAction.SUMMER_CHOICE_APPROVE),
                        eq(EntityType.SUMMER_CHOICE),
                        eq(CHOICE_ID),
                        eq(Map.of(ApiField.STATUS, SummerStatus.PENDING, ApiField.CHOICE, SummerChoice.STAY)),
                        eq(Map.of(ApiField.STATUS, SummerStatus.APPROVED, ApiField.DECIDED_BY, STAFF_ID)),
                        isNull());
    }

    @Test
    void tempLeaveAndLeaveApprovalsOnlyChangeChoiceStatus() {
        for (String option : List.of(SummerChoice.TEMP_LEAVE, SummerChoice.LEAVE)) {
            Fixture fixture = new Fixture();
            fixture.givenChoice(SummerStatus.PENDING, option);

            fixture.service.approve(CHOICE_ID);

            verifyNoInteractions(fixture.contracts);
            assertThat(fixture.jdbc.writeContaining("UPDATE summer_choice SET status=?"))
                    .as("%s chỉ xét duyệt, không được động vào hợp đồng", option)
                    .contains(SummerStatus.APPROVED);
        }
    }

    @Test
    void approvedChoiceCannotBeDecidedAgain() {
        ready.givenChoice(SummerStatus.APPROVED, SummerChoice.STAY);

        assertThatThrownBy(() -> ready.service.approve(CHOICE_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getErrorCode())
                                        .as("Bản đã duyệt không cho quyết định lại")
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
        assertThat(ready.jdbc.writes()).isEmpty();
        verifyNoInteractions(ready.contracts);
    }

    @Test
    void rejectedChoiceCannotBeApprovedLater() {
        ready.givenChoice(SummerStatus.REJECTED, SummerChoice.STAY);

        assertThatThrownBy(() -> ready.service.approve(CHOICE_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
        assertThat(ready.jdbc.writes()).isEmpty();
    }

    @Test
    void stayApprovalIsBlockedWhenContractIsNotActive() {
        Fixture fixture = new Fixture();
        fixture.givenChoice(SummerStatus.PENDING, SummerChoice.STAY);
        fixture.givenContractStatus(ContractStatus.EXPIRED);

        assertThatThrownBy(() -> fixture.service.approve(CHOICE_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getErrorCode())
                                        .as("Chỉ gia hạn được trên hợp đồng còn hiệu lực")
                                        .isEqualTo(ErrorCode.CONTRACT_NOT_ACTIVE));
        assertThat(fixture.jdbc.writes()).isEmpty();
        verifyNoInteractions(fixture.contracts);
    }

    @Test
    void stayApprovalRequiresASummerSemester() {
        Fixture fixture = new Fixture();
        fixture.givenChoice(SummerStatus.PENDING, SummerChoice.STAY);
        when(fixture.rooms.getSemesterReference(SEMESTER_ID))
                .thenReturn(
                        new RoomReferenceService.SemesterRef(
                                SEMESTER_ID, SemesterType.HK2, LocalDate.of(2026, 1, 1), SEMESTER_END));

        assertThatThrownBy(() -> fixture.service.approve(CHOICE_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error -> {
                            assertThat(error.getErrorCode())
                                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
                            assertThat(BusinessFailures.fieldErrors(error))
                                    .containsEntry(
                                            ApiField.SEMESTER_ID,
                                            ValidationMessage.SUMMER_SEMESTER_REQUIRED);
                        });
        verifyNoInteractions(fixture.contracts);
    }

    @Test
    void stayApprovalRequiresSemesterEndingAfterContract() {
        Fixture fixture = new Fixture();
        fixture.givenChoice(SummerStatus.PENDING, SummerChoice.STAY);
        when(fixture.rooms.getSemesterReference(SEMESTER_ID))
                .thenReturn(
                        new RoomReferenceService.SemesterRef(
                                SEMESTER_ID,
                                SemesterType.SUMMER,
                                LocalDate.of(2026, 6, 1),
                                CONTRACT_END));

        assertThatThrownBy(() -> fixture.service.approve(CHOICE_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(BusinessFailures.fieldErrors(error))
                                        .containsEntry(
                                                ApiField.SEMESTER_ID,
                                                ValidationMessage.SUMMER_SEMESTER_AFTER_CONTRACT));
        assertThat(fixture.jdbc.writes()).isEmpty();
        verifyNoInteractions(fixture.contracts);
    }

    @Test
    void unknownChoiceOptionIsNotApprovable() {
        ready.givenChoice(SummerStatus.PENDING, "MOVING_OUT");

        assertThatThrownBy(() -> ready.service.approve(CHOICE_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getErrorCode())
                                        .as("Lựa chọn ngoài danh nghiệp vụ phải bị chặn")
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
        assertThat(ready.jdbc.writes()).isEmpty();
    }

    @Test
    void rejectWithShortReasonIsRefusedBeforeTouchingData() {
        Fixture fixture = new Fixture();

        assertThatThrownBy(() -> fixture.service.reject(CHOICE_ID, "ngắn"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error -> {
                            assertThat(error.getErrorCode())
                                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
                            assertThat(BusinessFailures.fieldErrors(error))
                                    .containsEntry(
                                            ApiField.REASON,
                                            ValidationMessage.REJECT_REASON_REQUIRED);
                        });
        assertThat(fixture.queries.statements())
                .as("Chưa đọc bản ghi nào vì lý do từ chối không hợp lệ")
                .isEmpty();
        assertThat(fixture.jdbc.writes()).isEmpty();
    }

    @Test
    void rejectStoresTrimmedReasonInHistoryAndAudit() {
        ready.givenChoice(SummerStatus.PENDING, SummerChoice.LEAVE);

        ready.service.reject(CHOICE_ID, "  Dãy phòng đóng cửa hè  ");

        assertThat(ready.jdbc.writeContaining("UPDATE summer_choice SET status=?"))
                .contains(SummerStatus.REJECTED);
        verify(ready.history)
                .record(
                        eq(EntityType.SUMMER_CHOICE),
                        eq(CHOICE_ID),
                        eq(SummerStatus.PENDING),
                        eq(SummerStatus.REJECTED),
                        eq(STAFF_ID),
                        eq("Dãy phòng đóng cửa hè"));
        verify(ready.audit)
                .record(
                        eq(STAFF_ID),
                        eq(AuditAction.SUMMER_CHOICE_REJECT),
                        eq(EntityType.SUMMER_CHOICE),
                        eq(CHOICE_ID),
                        any(),
                        any(),
                        eq("Dãy phòng đóng cửa hè"));
    }

    /** Bộ mock + dữ liệu giả cho một lần gọi Service; mỗi test tạo bản riêng để không lẫn verify. */
    private static final class Fixture {

        private final FakeJdbc jdbc = new FakeJdbc();

        private final FakeSqlPage queries = new FakeSqlPage(jdbc);

        private final CurrentUser current = mock(CurrentUser.class);

        private final AuditService audit = mock(AuditService.class);

        private final ScopeGuard guard = mock(ScopeGuard.class);

        private final StatusHistoryService history = mock(StatusHistoryService.class);

        private final ContractService contracts = mock(ContractService.class);

        private final RoomReferenceService rooms = mock(RoomReferenceService.class);

        private final SummerCrudService service;

        private Fixture() {
            service =
                    new SummerCrudServiceImpl(
                            jdbc, queries, current, audit, guard, history, contracts, rooms);
            when(current.get()).thenReturn(new Actor(STAFF_ID, Role.STAFF, UNIT_ID, false, false));
            when(guard.hasPermission(Permission.SUMMER_REGISTRATION_UPDATE)).thenReturn(true);
            when(rooms.getSemesterReference(SEMESTER_ID))
                    .thenReturn(
                            new RoomReferenceService.SemesterRef(
                                    SEMESTER_ID,
                                    SemesterType.SUMMER,
                                    LocalDate.of(2026, 6, 1),
                                    SEMESTER_END));
            givenContract(ContractStatus.ACTIVE);
        }

        /** Đăng ký bản lựa chọn hè theo trạng thái và lựa chọn cần xét duyệt. */
        private void givenChoice(String status, String option) {
            Map<String, Object> choice =
                    FakeJdbc.row(
                            "id", CHOICE_ID,
                            "contractId", CONTRACT_ID,
                            "semesterId", SEMESTER_ID,
                            "choice", option,
                            "status", status);
            // Bản có FOR UPDATE phải đăng ký trước vì câu đọc thường là chuỗi con của nó.
            jdbc.returning("summer_choice WHERE id=? FOR UPDATE", List.of(choice));
            jdbc.returning("FROM summer_choice WHERE id=?", List.of(choice));
        }

        /** Đổi trạng thái hợp đồng nguồn; đăng ký lại cùng marker sẽ thay thế dữ liệu cũ. */
        private void givenContractStatus(String contractStatus) {
            givenContract(contractStatus);
        }

        private void givenContract(String contractStatus) {
            jdbc.returningOne(
                    "COALESCE",
                    FakeJdbc.row(
                            "studentCode", "20650001",
                            "studentName", "Nguyễn Văn A",
                            "semesterName", "Học kỳ hè 2026"));
            jdbc.returningOne(
                    "FROM dormitory_contract c JOIN",
                    FakeJdbc.row(
                            "id", CONTRACT_ID,
                            "studentId", STUDENT_ID,
                            "preferredUnitId", UNIT_ID,
                            "status", contractStatus,
                            "code", "HD-0001",
                            "startDate", CONTRACT_END.minusYears(1),
                            "endDate", CONTRACT_END));
        }
    }
}
