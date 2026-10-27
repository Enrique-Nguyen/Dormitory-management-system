package vn.edu.tlu.dms.billing.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import vn.edu.tlu.dms.billing.api.DepositDtos;
import vn.edu.tlu.dms.billing.domain.DepositTransaction;
import vn.edu.tlu.dms.billing.repository.DepositTransactionRepository;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.DepositType;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.support.SqlPage;
import vn.edu.tlu.dms.support.BusinessFailures;
import vn.edu.tlu.dms.support.FakeJdbc;

import java.time.LocalDate;

/**
 * Cổng kiểm tra của nghiệp vụ bù cọc (BR-10, FR-BIL-09): chỉ hợp đồng ACTIVE còn thiếu cọc mới được
 * bù, số tiền bù không được vượt khoản còn thiếu và khoản còn thiếu luôn đọc từ sổ cọc, không phải
 * hằng số trong Service. Test thuần, không CSDL; khóa dòng và xung đột thật thuộc
 * DepositIntegrationTest vì cần transaction.
 */
class DepositTopupGuardTest {

    private static final Long CONTRACT_ID = 21L;

    private static final Long STUDENT_ID = 10L;

    private static final Long UNIT_ID = 1L;

    private static final Long STAFF_ID = 20L;

    private static final Long TRANSACTION_ID = 500L;

    private final Fixture fixture = new Fixture();

    @Test
    void topupRequiresPositiveAmountBeforeLockingTheContract() {
        assertThatThrownBy(() -> fixture.service.topup(CONTRACT_ID, 0L, "Bù cọc"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error -> {
                            assertThat(error.getErrorCode())
                                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
                            assertThat(BusinessFailures.fieldErrors(error))
                                    .containsEntry(
                                            ApiField.AMOUNT,
                                            ValidationMessage.TOPUP_AMOUNT_POSITIVE);
                        });
        assertThat(fixture.jdbc.statements())
                .as("Số tiền không hợp lệ thì chưa được khóa hợp đồng")
                .isEmpty();
    }

    @Test
    void topupIsRejectedWhenContractIsNotActive() {
        fixture.givenBalance(ContractStatus.PENDING, 0L);

        assertThatThrownBy(() -> fixture.service.topup(CONTRACT_ID, 300_000L, "Bù cọc"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getErrorCode())
                                        .as("Hợp đồng chưa kích hoạt thì báo rõ lý do, không phải 'không cần bù'")
                                        .isEqualTo(ErrorCode.CONTRACT_NOT_ACTIVE));
        verify(fixture.transactions, never()).saveAndFlush(any(DepositTransaction.class));
    }

    @Test
    void topupIsRejectedWhenDepositIsAlreadyComplete() {
        fixture.givenBalance(ContractStatus.ACTIVE, 0L);

        assertThatThrownBy(() -> fixture.service.topup(CONTRACT_ID, 300_000L, "Bù cọc"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getErrorCode())
                                        .isEqualTo(ErrorCode.NO_TOPUP_REQUIRED));
        assertNothingWritten();
    }

    @Test
    void topupCannotExceedTheAmountStillMissing() {
        fixture.givenBalance(ContractStatus.ACTIVE, 800_000L);

        assertThatThrownBy(() -> fixture.service.topup(CONTRACT_ID, 800_001L, "Bù cọc"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getErrorCode())
                                        .as("Nộp thừa làm lệch sổ cọc nên phải bị chặn")
                                        .isEqualTo(ErrorCode.TOPUP_EXCEEDS_REQUIRED));
        assertNothingWritten();
    }

    @Test
    void topupOfExactlyTheMissingAmountIsRecorded() {
        fixture.givenBalance(ContractStatus.ACTIVE, 1_700_000L);

        DepositDtos.Transaction result =
                fixture.service.topup(CONTRACT_ID, 1_700_000L, "  Nộp bù cọc kỳ hè  ");

        ArgumentCaptor<DepositTransaction> saved =
                ArgumentCaptor.forClass(DepositTransaction.class);
        verify(fixture.transactions).saveAndFlush(saved.capture());
        DepositTransaction entry = saved.getValue();
        assertThat(entry.getType())
                .as("Giao dịch ghi đúng loại bù cọc")
                .isEqualTo(DepositType.TOPUP);
        assertThat(entry.getAmount())
                .as("Số tiền ghi dương và đúng khoản còn thiếu, không lấy từ hằng số cứng")
                .isEqualTo(1_700_000L);
        assertThat(entry.getContractId()).isEqualTo(CONTRACT_ID);
        assertThat(entry.getConfirmedBy()).isEqualTo(STAFF_ID);
        assertThat(entry.getNote())
                .as("Ghi chú được cắt khoảng trắng trước khi lưu")
                .isEqualTo("Nộp bù cọc kỳ hè");
        assertThat(result.id()).isEqualTo(TRANSACTION_ID);
        assertThat(result.type()).isEqualTo(DepositType.TOPUP);
        verify(fixture.audit)
                .record(
                        eq(STAFF_ID),
                        eq(AuditAction.DEPOSIT_TOPUP),
                        eq(EntityType.DEPOSIT_TRANSACTION),
                        eq(TRANSACTION_ID),
                        isNull(),
                        any(),
                        any());
    }

    @Test
    void topupByStaffOfAnotherUnitIsHidden() {
        Fixture otherUnit = new Fixture(new Actor(STAFF_ID, Role.STAFF, UNIT_ID + 100, false, false));
        otherUnit.givenBalance(ContractStatus.ACTIVE, 300_000L);

        assertThatThrownBy(() -> otherUnit.service.topup(CONTRACT_ID, 300_000L, "Bù cọc"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getErrorCode())
                                        .as("Ngoài phạm vi đơn vị trả 404 theo quy ước RBAC")
                                        .isEqualTo(ErrorCode.NOT_FOUND));
        assertNothingWritten(otherUnit);
    }

    private void assertNothingWritten() {
        assertNothingWritten(fixture);
    }

    private void assertNothingWritten(Fixture target) {
        verify(target.transactions, never()).saveAndFlush(any(DepositTransaction.class));
        verify(target.audit, never()).record(any(), any(), any(), any(), any(), any(), any());
        assertThat(target.jdbc.writes()).isEmpty();
    }

    /** Bộ dữ liệu giả cho sổ cọc: một hợp đồng, một dòng v_deposit_balance và các cổng quyền mock. */
    private static final class Fixture {

        private final FakeJdbc jdbc = new FakeJdbc();

        private final DepositTransactionRepository transactions =
                mock(DepositTransactionRepository.class);

        private final ScopeGuard guard = mock(ScopeGuard.class);

        private final CurrentUser current = mock(CurrentUser.class);

        private final AuditService audit = mock(AuditService.class);

        private final DepositServiceImpl service;

        private Fixture() {
            this(new Actor(STAFF_ID, Role.STAFF, UNIT_ID, false, false));
        }

        private Fixture(Actor actor) {
            service =
                    new DepositServiceImpl(
                            transactions,
                            jdbc,
                            mock(SqlPage.class),
                            guard,
                            current,
                            audit);
            when(current.get()).thenReturn(actor);
            when(guard.hasPermission(Permission.DEPOSIT_CREATE)).thenReturn(true);
            when(transactions.saveAndFlush(any(DepositTransaction.class)))
                    .thenAnswer(
                            invocation -> {
                                DepositTransaction entry = invocation.getArgument(0);
                                entry.setId(TRANSACTION_ID);
                                return entry;
                            });
        }

        /** Số dư đọc từ v_deposit_balance: trạng thái hợp đồng và khoản còn thiếu phải bù. */
        private void givenBalance(String status, Long topupNeeded) {
            jdbc.returningOne(
                    "WHERE id = ? FOR UPDATE",
                    FakeJdbc.row("id", CONTRACT_ID));
            jdbc.returningOne(
                    "SELECT c.student_id, ap.preferred_unit_id",
                    FakeJdbc.row("student_id", STUDENT_ID, "preferred_unit_id", UNIT_ID));
            jdbc.returningOne(
                    "v_deposit_balance",
                    FakeJdbc.row(
                            "contract_id", CONTRACT_ID,
                            "student_id", STUDENT_ID,
                            "status", status,
                            "deposit_required", 2_000_000L,
                            "balance", 2_000_000L - topupNeeded,
                            "topup_needed", topupNeeded,
                            "oldest_unpaid_due", LocalDate.of(2026, 5, 5),
                            "topup_overdue", false,
                            "deposit_debt", 0L,
                            "contract_code", "HD-0001",
                            "student_name", "Nguyễn Văn A",
                            "student_code", "20650001",
                            "preferred_unit_id", UNIT_ID));
        }
    }
}
