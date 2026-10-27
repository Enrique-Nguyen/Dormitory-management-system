package vn.edu.tlu.dms.contract.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import vn.edu.tlu.dms.billing.service.DepositService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.PaymentMethod;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.ConfigService;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.contract.api.ContractDtos;
import vn.edu.tlu.dms.contract.repository.DormitoryContractRepository;
import vn.edu.tlu.dms.contract.service.StorageService;
import vn.edu.tlu.dms.contract.service.SummerLeaveLookup;
import vn.edu.tlu.dms.room.service.BedOccupancyService;
import vn.edu.tlu.dms.room.service.OccupancyQuery;
import vn.edu.tlu.dms.room.service.RoomReferenceService;
import vn.edu.tlu.dms.support.BusinessFailures;
import vn.edu.tlu.dms.support.FakeJdbc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Các cổng kiểm tra của nghiệp vụ kích hoạt hợp đồng (FR-CON-01, BR-10, API 4.6): hợp đồng phải còn
 * PENDING, chưa quá hạn kích hoạt, còn giữ chỗ HOLD và số tiền cọc nộp vào phải đúng mức cọc của
 * tòa do module room cung cấp. Test thuần, không CSDL; nhánh thành công đầy đủ (thu cọc, chuyển
 * HOLD sang STAY, lịch sử, audit) thuộc ContractActivationIntegrationTest vì cần transaction thật.
 */
class ContractActivationValidationTest {

    private static final Long CONTRACT_ID = 21L;

    private static final Long STUDENT_ID = 10L;

    private static final Long APPLICATION_ID = 5L;

    private static final Long UNIT_ID = 1L;

    private static final Long STAFF_ID = 20L;

    private static final Long BUILDING_ID = 3L;

    private static final Long BED_ID = 41L;

    /** Mức cọc của tòa lấy từ module room; không được trùng với ảnh chụp trên hợp đồng. */
    private static final Long BUILDING_DEPOSIT = 500_000L;

    /** Ảnh chụp deposit_required trên hợp đồng, cố tình khác mức cọc hiện hành của tòa. */
    private static final Long CONTRACT_DEPOSIT_SNAPSHOT = 700_000L;

    private final Fixture fixture = new Fixture();

    @Test
    void activationRequiresSignedPaperContractBeforeReadingAnything() {
        assertThatThrownBy(
                        () ->
                                fixture.service.activate(
                                        CONTRACT_ID,
                                        request(false, BUILDING_DEPOSIT, PaymentMethod.CASH)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error -> {
                            assertThat(error.getErrorCode())
                                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
                            assertThat(error.getStatus()).isEqualTo(400);
                            assertThat(BusinessFailures.fieldErrors(error))
                                    .containsEntry(
                                            ApiField.PAPER_CONTRACT_SIGNED,
                                            ValidationMessage.PAPER_CONTRACT_REQUIRED);
                        });
        assertThat(fixture.jdbc.statements())
                .as("Chưa được đọc hay ghi dữ liệu khi hợp đồng giấy chưa ký")
                .isEmpty();
    }

    @Test
    void activationRequiresPositiveDepositAmount() {
        assertThatThrownBy(
                        () ->
                                fixture.service.activate(
                                        CONTRACT_ID,
                                        request(true, 0L, PaymentMethod.CASH)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(BusinessFailures.fieldErrors(error))
                                        .containsEntry(
                                                ApiField.DEPOSIT_AMOUNT,
                                                ValidationMessage.DEPOSIT_AMOUNT_REQUIRED));
    }

    @Test
    void activationRejectsUnsupportedPaymentMethod() {
        assertThatThrownBy(
                        () ->
                                fixture.service.activate(
                                        CONTRACT_ID,
                                        request(true, BUILDING_DEPOSIT, "MOMO")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(BusinessFailures.fieldErrors(error))
                                        .containsEntry(
                                                ApiField.PAYMENT_METHOD,
                                                ValidationMessage.PAYMENT_METHOD_INVALID));
    }

    @Test
    void cancelledContractCannotBeActivated() {
        fixture.givenContractStatus(ContractStatus.CANCELLED);

        assertThatThrownBy(
                        () ->
                                fixture.service.activate(
                                        CONTRACT_ID,
                                        request(true, BUILDING_DEPOSIT, PaymentMethod.CASH)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error -> {
                            assertThat(error.getErrorCode())
                                    .as("Hợp đồng đã tự hủy thì không cho kích hoạt lại")
                                    .isEqualTo(ErrorCode.CONTRACT_AUTO_CANCELLED);
                            assertThat(error.getStatus()).isEqualTo(409);
                        });
        assertThat(fixture.jdbc.writes()).isEmpty();
    }

    @Test
    void contractAlreadyActiveCannotBeActivatedAgain() {
        fixture.givenContractStatus(ContractStatus.ACTIVE);

        assertThatThrownBy(
                        () ->
                                fixture.service.activate(
                                        CONTRACT_ID,
                                        request(true, BUILDING_DEPOSIT, PaymentMethod.CASH)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
        verify(fixture.deposits, never()).collectInitial(any(), any(), any());
    }

    @Test
    void activationAfterDeadlineIsRejected() {
        fixture.givenDeadline(OffsetDateTime.now().minusDays(1));

        assertThatThrownBy(
                        () ->
                                fixture.service.activate(
                                        CONTRACT_ID,
                                        request(true, BUILDING_DEPOSIT, PaymentMethod.CASH)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getErrorCode())
                                        .as("Quá hạn kích hoạt phải báo mã riêng, không chung với sai trạng thái")
                                        .isEqualTo(ErrorCode.CONTRACT_ACTIVATION_EXPIRED));
        assertThat(fixture.jdbc.writes()).isEmpty();
    }

    @Test
    void activationRequiresAnOpenHoldAssignment() {
        when(fixture.occupancy.findOpenAssignmentByContract(CONTRACT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                fixture.service.activate(
                                        CONTRACT_ID,
                                        request(true, BUILDING_DEPOSIT, PaymentMethod.CASH)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getErrorCode())
                                        .isEqualTo(ErrorCode.NO_HOLD_ASSIGNMENT));
        assertThat(fixture.jdbc.writes()).isEmpty();
    }

    @Test
    void activationRejectsDepositAmountBelowBuildingLevel() {
        fixture.givenBuildingDeposit(BUILDING_DEPOSIT);

        assertThatThrownBy(
                        () ->
                                fixture.service.activate(
                                        CONTRACT_ID,
                                        request(true, 300_000L, PaymentMethod.CASH)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error -> {
                            assertThat(error.getErrorCode())
                                    .isEqualTo(ErrorCode.DEPOSIT_AMOUNT_MISMATCH);
                            assertThat(error.getStatus()).isEqualTo(400);
                            assertThat(error.getDetails())
                                    .as("Phải trả đúng mức cọc để giao diện báo cho người nộp")
                                    .containsEntry("required", BUILDING_DEPOSIT);
                        });
        verify(fixture.rooms).getBuildingDepositAmount(BUILDING_ID);
        verify(fixture.deposits, never()).collectInitial(any(), any(), any());
        verify(fixture.occupancyService, never()).convertHoldToStay(any(), any());
        assertThat(fixture.jdbc.writes()).isEmpty();
    }

    @Test
    void activationRejectsSnapshotDepositOfTheContractItself() {
        fixture.givenBuildingDeposit(BUILDING_DEPOSIT);

        assertThatThrownBy(
                        () ->
                                fixture.service.activate(
                                        CONTRACT_ID,
                                        request(
                                                true,
                                                CONTRACT_DEPOSIT_SNAPSHOT,
                                                PaymentMethod.TRANSFER)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getDetails())
                                        .as("Mức cọc lấy theo tòa đang áp dụng, không theo ảnh chụp cũ")
                                        .containsEntry("required", BUILDING_DEPOSIT));
    }

    @Test
    void missingBuildingDepositIsReportedAsZeroInsteadOfCrashing() {
        fixture.givenBuildingDeposit(null);

        assertThatThrownBy(
                        () ->
                                fixture.service.activate(
                                        CONTRACT_ID,
                                        request(true, 300_000L, PaymentMethod.CASH)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error -> {
                            assertThat(error.getErrorCode())
                                    .isEqualTo(ErrorCode.DEPOSIT_AMOUNT_MISMATCH);
                            assertThat(error.getDetails()).containsEntry("required", 0L);
                        });
    }

    @Test
    void correctDepositIsWrittenFromRoomServiceValueBeforeActivation() {
        fixture.givenBuildingDeposit(BUILDING_DEPOSIT);
        // Không có dòng nào bị cập nhật: mô phỏng hợp đồng đổi trạng thái ngay sau khi đọc.
        fixture.jdbc.affecting(0);

        assertThatThrownBy(
                        () ->
                                fixture.service.activate(
                                        CONTRACT_ID,
                                        request(true, BUILDING_DEPOSIT, PaymentMethod.CASH)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getErrorCode())
                                        .as("Tiền cọc đúng mức của tòa thì được xét tiếp, không bị chặn ở bước kiểm cọc")
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
        assertThat(fixture.jdbc.writeContaining("UPDATE dormitory_contract SET deposit_required=?"))
                .as("Mức cọc ghi xuống hợp đồng là mức đọc từ module room")
                .contains("| " + BUILDING_DEPOSIT);
        verify(fixture.deposits, never()).collectInitial(any(), any(), any());
        verify(fixture.occupancyService, never()).convertHoldToStay(any(), any());
    }

    private static ContractDtos.ActivateRequest request(
            boolean paperSigned, Long depositAmount, String paymentMethod) {
        return new ContractDtos.ActivateRequest(paperSigned, depositAmount, paymentMethod, "Kích hoạt");
    }

    /** Dựng Service với toàn bộ cổng ngoài là mock; chỉ dữ liệu đọc là FakeJdbc. */
    private static final class Fixture {

        private final FakeJdbc jdbc = new FakeJdbc();

        private final CurrentUser current = mock(CurrentUser.class);

        private final ScopeGuard guard = mock(ScopeGuard.class);

        private final StatusHistoryService history = mock(StatusHistoryService.class);

        private final DormitoryContractRepository repository =
                mock(DormitoryContractRepository.class);

        private final OccupancyQuery occupancy = mock(OccupancyQuery.class);

        private final SummerLeaveLookup summerLeaves = mock(SummerLeaveLookup.class);

        private final BedOccupancyService occupancyService = mock(BedOccupancyService.class);

        private final RoomReferenceService rooms = mock(RoomReferenceService.class);

        private final DepositService deposits = mock(DepositService.class);

        private final StorageService storage = mock(StorageService.class);

        private final ConfigService configs = mock(ConfigService.class);

        private final AuditService audit = mock(AuditService.class);

        private final ContractServiceImpl service;

        private OffsetDateTime deadline = OffsetDateTime.now().plusDays(7);

        private String lockedStatus = ContractStatus.PENDING;

        private Fixture() {
            service =
                    new ContractServiceImpl(
                            repository,
                            current,
                            guard,
                            history,
                            jdbc,
                            occupancy,
                            summerLeaves,
                            occupancyService,
                            rooms,
                            deposits,
                            storage,
                            configs,
                            audit);
            when(current.get()).thenReturn(new Actor(STAFF_ID, Role.STAFF, UNIT_ID, false, false));
            when(occupancy.findOpenAssignmentByContract(CONTRACT_ID))
                    .thenReturn(Optional.of(holdBed()));
            // Câu có FOR UPDATE phải đăng ký trước vì SQL đọc không khóa là chuỗi con của nó.
            jdbc.returning("FOR UPDATE OF c", List.of(contractRow(lockedStatus)));
            jdbc.returning("WHERE c.id=?", List.of(contractRow(ContractStatus.PENDING)));
        }

        /** Trạng thái đọc lại sau khi khóa hợp đồng, tức bản mà transaction sẽ tác động. */
        private void givenContractStatus(String status) {
            this.lockedStatus = status;
            jdbc.returning("FOR UPDATE OF c", List.of(contractRow(status)));
        }

        private void givenDeadline(OffsetDateTime value) {
            this.deadline = value;
            jdbc.returning("FOR UPDATE OF c", List.of(contractRow(lockedStatus)));
        }

        private void givenBuildingDeposit(Long amount) {
            when(rooms.getBuildingDepositAmount(BUILDING_ID)).thenReturn(amount);
        }

        private Map<String, Object> contractRow(String status) {
            return FakeJdbc.row(
                    "id", CONTRACT_ID,
                    "code", "HD-0001",
                    "student_id", STUDENT_ID,
                    "application_id", APPLICATION_ID,
                    "start_date", LocalDate.of(2025, 12, 1),
                    "end_date", LocalDate.of(2026, 6, 30),
                    "status", status,
                    "activation_deadline", deadline,
                    "deposit_required", CONTRACT_DEPOSIT_SNAPSHOT,
                    "preferred_unit_id", UNIT_ID);
        }

        private OccupancyQuery.CurrentBed holdBed() {
            return new OccupancyQuery.CurrentBed(
                    9L,
                    STUDENT_ID,
                    CONTRACT_ID,
                    AssignmentKind.HOLD,
                    BED_ID,
                    "03",
                    30L,
                    "A101",
                    BUILDING_ID,
                    "K1",
                    UNIT_ID);
        }
    }
}
