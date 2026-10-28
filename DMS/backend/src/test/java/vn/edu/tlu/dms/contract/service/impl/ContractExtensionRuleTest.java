package vn.edu.tlu.dms.contract.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.ExtensionSource;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.StayRequestStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.ConfigService;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.contract.service.ContractService;
import vn.edu.tlu.dms.room.service.OccupancyQuery;
import vn.edu.tlu.dms.support.BusinessFailures;
import vn.edu.tlu.dms.support.FakeJdbc;
import vn.edu.tlu.dms.support.FakeSqlPage;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Điều kiện xét duyệt yêu cầu gia hạn hợp đồng (FR-CON-05, BR-09): chỉ yêu cầu còn PENDING mới quyết
 * định được, ngày gia hạn phải sau ngày kết thúc hiện tại, khoảng ở mới không được chồng với hợp
 * đồng khác còn sống của cùng sinh viên, và hủy/xóa chỉ áp dụng cho bản chưa quyết định. Test thuần
 * theo kiểu ghi giả; rollback thật khi có xung đột thuộc ContractExtensionIntegrationTest.
 */
class ContractExtensionRuleTest {

    private static final Long REQUEST_ID = 66L;

    private static final Long CONTRACT_ID = 21L;

    private static final Long STUDENT_ID = 10L;

    private static final Long UNIT_ID = 1L;

    private static final Long STAFF_ID = 20L;

    private static final LocalDate CONTRACT_START = LocalDate.of(2025, 12, 1);

    private static final LocalDate CONTRACT_END = LocalDate.of(2026, 6, 30);

    private static final LocalDate NEW_END = LocalDate.of(2026, 8, 31);

    private static final String REASON = "Ở lại ký túc xá hết học kỳ hè";

    private final Fixture fixture = new Fixture();

    @Test
    void approvedExtensionMovesContractEndDateAndClosesTheRequest() {
        fixture.givenRequest(StayRequestStatus.PENDING, NEW_END);
        fixture.givenContract(ContractStatus.ACTIVE, CONTRACT_END);
        fixture.givenOverlappingContract(null);

        fixture.service.approveExtension(REQUEST_ID);

        verify(fixture.contracts).extendEndDate(CONTRACT_ID, NEW_END, REASON);
        String update =
                fixture.jdbc.writeContaining("UPDATE contract_extension_request SET status=?");
        assertThat(update)
                .as("Chỉ chuyển bản đang PENDING sang APPROVED, có điều kiện trạng thái trong WHERE")
                .contains("| " + StayRequestStatus.APPROVED)
                .contains("| " + STAFF_ID)
                .contains("| " + StayRequestStatus.PENDING);
        verify(fixture.history)
                .record(
                        eq(EntityType.EXTENSION),
                        eq(REQUEST_ID),
                        eq(StayRequestStatus.PENDING),
                        eq(StayRequestStatus.APPROVED),
                        eq(STAFF_ID),
                        eq(REASON));
        verify(fixture.audit)
                .record(
                        eq(STAFF_ID),
                        eq(AuditAction.CONTRACT_EXTENSION_APPROVE),
                        eq(EntityType.EXTENSION),
                        eq(REQUEST_ID),
                        any(),
                        any(),
                        eq(REASON));
    }

    @Test
    void decidedExtensionCannotBeApprovedAgain() {
        fixture.givenRequest(StayRequestStatus.APPROVED, NEW_END);
        fixture.givenContract(ContractStatus.ACTIVE, CONTRACT_END);

        assertThatThrownBy(() -> fixture.service.approveExtension(REQUEST_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error -> {
                            assertThat(error.getErrorCode())
                                    .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
                            assertThat(error.getDetails()).isEmpty();
                        });
        verifyNoInteractions(fixture.contracts);
        assertThat(fixture.jdbc.writes()).isEmpty();
    }

    @Test
    void extensionOfClosedContractIsRejected() {
        fixture.givenRequest(StayRequestStatus.PENDING, NEW_END);
        fixture.givenContract(ContractStatus.EXPIRED, CONTRACT_END);

        assertThatThrownBy(() -> fixture.service.approveExtension(REQUEST_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getErrorCode())
                                        .as("Hết hạn thì phải làm hợp đồng mới, không gia hạn")
                                        .isEqualTo(ErrorCode.CONTRACT_NOT_ACTIVE));
        verifyNoInteractions(fixture.contracts);
    }

    @Test
    void newEndDateMustBeAfterTheCurrentContractEnd() {
        fixture.givenRequest(StayRequestStatus.PENDING, CONTRACT_END);
        fixture.givenContract(ContractStatus.ACTIVE, CONTRACT_END);

        assertThatThrownBy(() -> fixture.service.approveExtension(REQUEST_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error -> {
                            assertThat(error.getErrorCode())
                                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
                            assertThat(BusinessFailures.fieldErrors(error))
                                    .as("Ngày bằng ngày hiện tại cũng không được chấp nhận")
                                    .containsEntry(
                                            ApiField.NEW_END_DATE,
                                            ValidationMessage.EXTENSION_DATE_AFTER_CURRENT);
                        });
        verifyNoInteractions(fixture.contracts);
        assertThat(fixture.jdbc.writes()).isEmpty();
    }

    @Test
    void overlappingStayOfTheSameStudentBlocksApproval() {
        fixture.givenRequest(StayRequestStatus.PENDING, NEW_END);
        fixture.givenContract(ContractStatus.ACTIVE, CONTRACT_END);
        fixture.givenOverlappingContract("HD-0009");

        assertThatThrownBy(() -> fixture.service.approveExtension(REQUEST_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error -> {
                            assertThat(error.getErrorCode())
                                    .isEqualTo(ErrorCode.CONTRACT_PERIOD_OVERLAP);
                            assertThat(error.getStatus()).isEqualTo(409);
                            assertThat(error.getDetails())
                                    .as("Phải nêu mã hợp đồng đang chồng khoảng")
                                    .containsEntry(ApiField.CONTRACT_CODE, "HD-0009");
                        });
        verifyNoInteractions(fixture.contracts);
        assertThat(fixture.jdbc.writes()).isEmpty();
    }

    @Test
    void approvalStopsWhenTheRequestLeftPendingBetweenLockAndUpdate() {
        fixture.givenRequest(StayRequestStatus.PENDING, NEW_END);
        fixture.givenContract(ContractStatus.ACTIVE, CONTRACT_END);
        fixture.givenOverlappingContract(null);
        // UPDATE có điều kiện status=PENDING không còn cập nhật dòng nào: có ai đó quyết định trước.
        fixture.jdbc.affecting(0);

        assertThatThrownBy(() -> fixture.service.approveExtension(REQUEST_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
        assertThat(fixture.jdbc.writeContaining("UPDATE contract_extension_request SET status=?"))
                .as("Câu UPDATE phải có điều kiện trạng thái để không ghi đè bản đã quyết định")
                .contains("AND status=?");
    }

    @Test
    void rejectionRequiresAReasonBeforeReadingAnything() {
        assertThatThrownBy(() -> fixture.service.rejectExtension(REQUEST_ID, "   "))
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
        assertThat(fixture.jdbc.statements()).isEmpty();
    }

    @Test
    void alreadyApprovedExtensionCannotBeCancelledByStudent() {
        fixture.givenRequest(StayRequestStatus.APPROVED, NEW_END);

        assertThatThrownBy(() -> fixture.service.cancelExtension(REQUEST_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getErrorCode())
                                        .as("Chỉ bản PENDING mới hủy được")
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
        assertThat(fixture.jdbc.writes()).isEmpty();
    }

    @Test
    void deletingAlreadyDecidedExtensionReportsResourceInUse() {
        fixture.givenRequest(StayRequestStatus.REJECTED, NEW_END);

        assertThatThrownBy(() -> fixture.service.deletePendingStayRequest(true, REQUEST_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getErrorCode())
                                        .as("Bản đã quyết định là dữ liệu lịch sử, không xóa được")
                                        .isEqualTo(ErrorCode.RESOURCE_IN_USE));
        assertThat(fixture.jdbc.writes()).isEmpty();
    }

    /** Bộ mock + dữ liệu giả; mỗi test tự đăng ký trạng thái yêu cầu và hợp đồng nguồn. */
    private static final class Fixture {

        private final FakeJdbc jdbc = new FakeJdbc();

        private final FakeSqlPage queries = new FakeSqlPage(jdbc);

        private final CurrentUser current = mock(CurrentUser.class);

        private final AuditService audit = mock(AuditService.class);

        private final ScopeGuard guard = mock(ScopeGuard.class);

        private final ConfigService configs = mock(ConfigService.class);

        private final OccupancyQuery occupancy = mock(OccupancyQuery.class);

        private final StatusHistoryService history = mock(StatusHistoryService.class);

        private final ContractService contracts = mock(ContractService.class);

        private final ContractCrudServiceImpl service;

        private LocalDate contractEndDate = CONTRACT_END;

        private String contractStatus = ContractStatus.ACTIVE;

        private LocalDate requestNewEndDate = NEW_END;

        private String requestStatus = StayRequestStatus.PENDING;

        private Fixture() {
            service =
                    new ContractCrudServiceImpl(
                            jdbc, queries, current, audit, guard, configs, occupancy, history, contracts);
            when(current.get()).thenReturn(new Actor(STAFF_ID, Role.STAFF, UNIT_ID, false, false));
            when(guard.hasPermission(
                            vn.edu.tlu.dms.common.constants.DmsConstants.Permission
                                    .CONTRACT_EXTENSION_UPDATE))
                    .thenReturn(true);
            when(occupancy.findOpenAssignmentByContract(CONTRACT_ID))
                    .thenReturn(Optional.of(stayBed()));
            refreshRequestRow();
            refreshContractLock();
            givenOverlappingContract(null);
        }

        /** Trạng thái và ngày của yêu cầu gia hạn; ảnh hưởng cả đọc thường lẫn đọc có khóa. */
        private void givenRequest(String status, LocalDate newEndDate) {
            this.requestStatus = status;
            this.requestNewEndDate = newEndDate;
            refreshRequestRow();
            refreshContractLock();
        }

        /** Trạng thái và ngày kết thúc của hợp đồng tại thời điểm xét duyệt. */
        private void givenContract(String status, LocalDate endDate) {
            this.contractStatus = status;
            this.contractEndDate = endDate;
            refreshContractLock();
        }

        /** Mã hợp đồng khác còn sống chồng khoảng của cùng sinh viên; null nghĩa là không chồng. */
        private void givenOverlappingContract(String contractCode) {
            jdbc.returning(
                    "SELECT code FROM dormitory_contract",
                    contractCode == null ? List.of() : List.of(FakeJdbc.row("code", contractCode)));
        }

        private void refreshRequestRow() {
            Map<String, Object> request = extensionRow();
            // Đăng ký bản có khóa trước vì SQL đọc thường là chuỗi con của SQL đọc có khóa.
            jdbc.returningOne("FOR UPDATE OF e", request);
            jdbc.returningOne("WHERE e.id=?", request);
        }

        private void refreshContractLock() {
            jdbc.returningOne(
                    "dormitory_contract WHERE id = ? FOR UPDATE",
                    FakeJdbc.row(
                            "id", CONTRACT_ID,
                            "student_id", STUDENT_ID,
                            "start_date", CONTRACT_START,
                            "end_date", contractEndDate,
                            "status", contractStatus));
            // Bản đọc qua SqlPage cho nhánh xóa yêu cầu (SELECT * của bảng yêu cầu và hợp đồng).
            jdbc.returningOne(
                    "contract_extension_request WHERE id=? FOR UPDATE",
                    FakeJdbc.row(
                            "id", REQUEST_ID,
                            "contractId", CONTRACT_ID,
                            "newEndDate", requestNewEndDate,
                            "oldEndDate", contractEndDate,
                            "reason", REASON,
                            "status", requestStatus));
            jdbc.returningOne(
                    "FROM dormitory_contract c JOIN",
                    FakeJdbc.row(
                            "id", CONTRACT_ID,
                            "studentId", STUDENT_ID,
                            "preferredUnitId", UNIT_ID,
                            "status", contractStatus,
                            "code", "HD-0001",
                            "startDate", CONTRACT_START,
                            "endDate", contractEndDate));
        }

        private Map<String, Object> extensionRow() {
            return FakeJdbc.row(
                    "id", REQUEST_ID,
                    "contract_id", CONTRACT_ID,
                    "contract_code", "HD-0001",
                    "student_id", STUDENT_ID,
                    "student_code", "20650001",
                    "full_name", "Nguyễn Văn A",
                    "source", ExtensionSource.STUDENT,
                    "old_end_date", contractEndDate,
                    "new_end_date", requestNewEndDate,
                    "reason", REASON,
                    "status", requestStatus,
                    "decided_by", null,
                    "decided_at", null,
                    "decision_reason", null,
                    "created_at", java.time.OffsetDateTime.now(),
                    "preferred_unit_id", UNIT_ID,
                    "start_date", CONTRACT_START,
                    "end_date", contractEndDate,
                    "contract_status", contractStatus);
        }

        private OccupancyQuery.CurrentBed stayBed() {
            return new OccupancyQuery.CurrentBed(
                    9L,
                    STUDENT_ID,
                    CONTRACT_ID,
                    AssignmentKind.STAY,
                    41L,
                    "03",
                    30L,
                    "A101",
                    3L,
                    "K1",
                    UNIT_ID);
        }
    }
}
