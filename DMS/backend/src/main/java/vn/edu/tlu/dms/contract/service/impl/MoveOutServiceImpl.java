package vn.edu.tlu.dms.contract.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.service.DebtQuery;
import vn.edu.tlu.dms.billing.service.DebtQuery.UnpaidInvoice;
import vn.edu.tlu.dms.billing.service.DepositService;
import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentEndReason;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
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
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.contract.api.MoveOutDtos;
import vn.edu.tlu.dms.contract.domain.AssetCondition;
import vn.edu.tlu.dms.contract.domain.ContractMovedOutEvent;
import vn.edu.tlu.dms.contract.domain.MoveOutRecord;
import vn.edu.tlu.dms.contract.repository.MoveOutRecordRepository;
import vn.edu.tlu.dms.contract.service.MoveOutService;
import vn.edu.tlu.dms.contract.service.MoveOutWarningLookup;
import vn.edu.tlu.dms.contract.service.MoveOutWarningLookup.UnsettledTicket;
import vn.edu.tlu.dms.room.service.BedOccupancyService;
import vn.edu.tlu.dms.room.service.OccupancyQuery;
import vn.edu.tlu.dms.room.service.OccupancyQuery.CurrentBed;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cài đặt nghiệp vụ trả phòng (FR-CON-09, FR-CON-10). Preview chỉ đọc; execute khóa giường/sinh
 * viên rồi hợp đồng, ghi biên bản và hoàn cọc trong cùng transaction để không lệch sổ cọc.
 */
@Service
@Transactional(readOnly = true)
public class MoveOutServiceImpl implements MoveOutService {

    private static final String DEFAULT_MOVE_OUT_REASON = "Trả phòng";

    private static final String SELECT_CONTRACT =
            """
            SELECT c.id, c.code, c.student_id, c.start_date, c.end_date, c.status,
                   ap.preferred_unit_id
            FROM dormitory_contract c
            JOIN room_application ap ON ap.id = c.application_id
            """;

    private final JdbcTemplate jdbc;

    private final DepositService deposits;

    private final DebtQuery debts;

    private final MoveOutWarningLookup warnings;

    private final OccupancyQuery occupancyQuery;

    private final BedOccupancyService occupancy;

    private final MoveOutRecordRepository moveOutRecords;

    private final StatusHistoryService history;

    private final AuditService audit;

    private final ApplicationEventPublisher events;

    private final ScopeGuard guard;

    private final CurrentUser current;

    public MoveOutServiceImpl(
            JdbcTemplate jdbc,
            DepositService deposits,
            DebtQuery debts,
            MoveOutWarningLookup warnings,
            OccupancyQuery occupancyQuery,
            BedOccupancyService occupancy,
            MoveOutRecordRepository moveOutRecords,
            StatusHistoryService history,
            AuditService audit,
            ApplicationEventPublisher events,
            ScopeGuard guard,
            CurrentUser current) {
        this.jdbc = jdbc;
        this.deposits = deposits;
        this.debts = debts;
        this.warnings = warnings;
        this.occupancyQuery = occupancyQuery;
        this.occupancy = occupancy;
        this.moveOutRecords = moveOutRecords;
        this.history = history;
        this.audit = audit;
        this.events = events;
        this.guard = guard;
        this.current = current;
    }

    @Override
    public MoveOutDtos.Preview preview(Long contractId, LocalDate moveOutDate) {
        guard.requirePermission(Permission.MOVE_OUT_READ);
        guard.requireRole(Role.STAFF);
        requireMoveOutDate(moveOutDate);
        ContractRow contract = loadContract(contractId);
        CurrentBed bed = occupancyQuery.findOpenAssignmentByContract(contractId).orElse(null);
        guard.requireUnit(bed == null ? contract.preferredUnitId() : bed.managementUnitId());
        requireActive(contract);
        return buildPreview(contract, bed, moveOutDate);
    }

    @Override
    @Transactional
    public MoveOutDtos.Result execute(Long contractId, MoveOutDtos.Request input) {
        guard.requirePermission(Permission.MOVE_OUT_CREATE);
        guard.requireRole(Role.STAFF);
        AssetCondition assetCondition = validate(input);

        ContractRow contract = loadContract(contractId);
        CurrentBed bed = occupancyQuery.findOpenAssignmentByContract(contractId).orElse(null);
        guard.requireUnit(bed == null ? contract.preferredUnitId() : bed.managementUnitId());
        requireActive(contract);
        if (bed == null) {
            throw BusinessException.conflict(ErrorCode.NOT_STAYING);
        }

        MoveOutDtos.Preview before = buildPreview(contract, bed, input.moveOutDate());
        if (before.warnings() && !Boolean.TRUE.equals(input.acknowledgedWarnings())) {
            throw warningsNotAcknowledged(before);
        }

        // Thứ tự khóa toàn cục: giường → sinh viên → hợp đồng; đọc lại trạng thái sau khi khóa.
        occupancy.lockOpenAssignment(contractId, contract.studentId());
        ContractRow locked = lockContract(contractId);
        requireActive(locked);
        CurrentBed lockedBed =
                occupancyQuery
                        .findOpenAssignmentByContract(contractId)
                        .orElseThrow(() -> BusinessException.conflict(ErrorCode.NOT_STAYING));

        // Tính lại sau khóa để biên bản và số tiền hoàn khớp sổ cọc tại thời điểm ghi.
        MoveOutDtos.Preview snapshot = buildPreview(locked, lockedBed, input.moveOutDate());
        // Công nợ/ticket có thể phát sinh trong lúc chờ khóa; buộc xác nhận lại trên số liệu mới.
        if (snapshot.warnings() && !Boolean.TRUE.equals(input.acknowledgedWarnings())) {
            throw warningsNotAcknowledged(snapshot);
        }
        long unpaidRoomFee = valueOrZero(debts.getStudentDebt(locked.studentId()).unpaidRoomFee());
        String reason = DEFAULT_MOVE_OUT_REASON;
        String assetNote = normalizeNote(input.assetNote());
        Actor actor = current.get();
        OffsetDateTime now = OffsetDateTime.now();

        int updated =
                jdbc.update(
                        "UPDATE dormitory_contract"
                                + " SET status=?,closed_at=?,status_reason=? WHERE id=? AND status=?",
                        snapshot.endStatus(),
                        now,
                        reason,
                        contractId,
                        ContractStatus.ACTIVE);
        if (updated == 0) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
        occupancy
                .release(contractId, AssignmentEndReason.MOVE_OUT, actor.id(), reason)
                .orElseThrow(() -> BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION));

        MoveOutRecord record = new MoveOutRecord();
        record.setContractId(contractId);
        record.setMovedOutAt(now);
        record.setAssetCondition(assetCondition.name());
        record.setAssetNote(assetNote);
        record.setUnpaidRoomFee(unpaidRoomFee);
        record.setDepositBalance(snapshot.depositBalance());
        record.setRefundAmount(snapshot.refundAmount());
        record.setConfirmedBy(actor.id());
        record = moveOutRecords.saveAndFlush(record);

        // Hợp đồng đã chuyển trạng thái nên DepositService.refund chấp nhận và chống hoàn trùng.
        if (snapshot.refundAmount() > 0) {
            deposits.refund(contractId, snapshot.refundAmount(), reason);
        }

        history.record(
                EntityType.CONTRACT,
                contractId,
                ContractStatus.ACTIVE,
                snapshot.endStatus(),
                actor.id(),
                reason);
        audit.record(
                actor.id(),
                AuditAction.CONTRACT_MOVE_OUT,
                EntityType.MOVE_OUT,
                record.getId(),
                null,
                auditValue(contractId, snapshot, unpaidRoomFee, assetCondition.name(), assetNote),
                reason);
        events.publishEvent(
                new ContractMovedOutEvent(contractId, locked.studentId(), snapshot.endStatus()));

        return new MoveOutDtos.Result(
                record.getId(),
                contractId,
                locked.code(),
                snapshot.endStatus(),
                input.moveOutDate(),
                snapshot.endStatus(),
                snapshot.depositBalance(),
                snapshot.refundAmount(),
                snapshot.negativeBalanceDebt(),
                unpaidRoomFee,
                assetCondition.name(),
                assetNote,
                actor.id(),
                record.getMovedOutAt(),
                locked.endDate(),
                snapshot.warnings());
    }

    /** Đọc hợp đồng kèm đơn vị nguyện vọng; không khóa để kiểm tra phạm vi trước. */
    private ContractRow loadContract(Long contractId) {
        return jdbc
                .query(SELECT_CONTRACT + " WHERE c.id=?", this::mapContract, contractId)
                .stream()
                .findFirst()
                .orElseThrow(BusinessException::notFound);
    }

    /** Khóa hợp đồng FOR UPDATE và đọc lại trạng thái/ngày sau khi các khóa giường đã giữ. */
    private ContractRow lockContract(Long contractId) {
        return jdbc
                .query(
                        SELECT_CONTRACT + " WHERE c.id=? FOR UPDATE OF c",
                        this::mapContract,
                        contractId)
                .stream()
                .findFirst()
                .orElseThrow(BusinessException::notFound);
    }

    private ContractRow mapContract(ResultSet rs, int rowNum) throws SQLException {
        return new ContractRow(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getLong("student_id"),
                rs.getObject("start_date", LocalDate.class),
                rs.getObject("end_date", LocalDate.class),
                rs.getString("status"),
                rs.getLong("preferred_unit_id"));
    }

    /** Tổng hợp cảnh báo và số tiền từ port của module billing/maintenance; chỉ trả DTO. */
    private MoveOutDtos.Preview buildPreview(
            ContractRow contract, CurrentBed bed, LocalDate moveOutDate) {
        long balance = depositBalance(contract.id());
        List<UnpaidInvoice> roomFee = debts.findUnpaidRoomFeeInvoices(contract.studentId());
        List<UnpaidInvoice> utility =
                bed == null ? List.of() : debts.findUnpaidUtilityInvoices(bed.roomId());
        List<UnsettledTicket> unsettled =
                bed == null
                        ? List.of()
                        : warnings.findUnsettledTickets(
                                contract.studentId(),
                                bed.roomId(),
                                OffsetDateTime.now(ZoneId.of(DmsConstants.APPLICATION_TIME_ZONE)));
        boolean leader = bed != null && occupancyQuery.isRoomLeader(bed.roomId(), contract.studentId());
        boolean hasWarnings = !roomFee.isEmpty() || !utility.isEmpty() || !unsettled.isEmpty();
        String endStatus =
                moveOutDate.isBefore(contract.endDate())
                        ? ContractStatus.TERMINATED
                        : ContractStatus.EXPIRED;
        return new MoveOutDtos.Preview(
                endStatus,
                balance,
                Math.max(balance, 0L),
                Math.max(-balance, 0L),
                roomFee,
                utility,
                unsettled,
                leader,
                hasWarnings);
    }

    private long depositBalance(Long contractId) {
        return valueOrZero(deposits.getBalanceView(contractId).balance());
    }

    private static long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private static void requireActive(ContractRow contract) {
        if (!ContractStatus.ACTIVE.equals(contract.status())) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
    }

    private static void requireMoveOutDate(LocalDate moveOutDate) {
        if (moveOutDate == null) {
            throw BusinessException.invalid(
                    ApiField.MOVE_OUT_DATE, ValidationMessage.MOVE_OUT_DATE_REQUIRED);
        }
    }

    /** Kiểm tra tình trạng tài sản; DAMAGED bắt buộc ghi chú và không tự trừ cọc. */
    private static AssetCondition validate(MoveOutDtos.Request input) {
        if (input == null) {
            throw BusinessException.invalid(
                    ApiField.ASSET_CONDITION, ValidationMessage.ASSET_CONDITION_INVALID);
        }
        requireMoveOutDate(input.moveOutDate());
        AssetCondition condition = AssetCondition.from(input.assetCondition());
        if (condition == null) {
            throw BusinessException.invalid(
                    ApiField.ASSET_CONDITION, ValidationMessage.ASSET_CONDITION_INVALID);
        }
        if (AssetCondition.DAMAGED.equals(condition)
                && (input.assetNote() == null || input.assetNote().isBlank())) {
            throw BusinessException.invalid(
                    ApiField.ASSET_NOTE, ValidationMessage.ASSET_NOTE_REQUIRED);
        }
        return condition;
    }

    private static String normalizeNote(String note) {
        return note == null || note.isBlank() ? null : note.strip();
    }

    /** details giống preview để giao diện hiển thị lại đúng số liệu trước khi xác nhận. */
    private static BusinessException warningsNotAcknowledged(MoveOutDtos.Preview preview) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("endStatus", preview.endStatus());
        details.put("depositBalance", preview.depositBalance());
        details.put("refundAmount", preview.refundAmount());
        details.put("negativeBalanceDebt", preview.negativeBalanceDebt());
        details.put("unpaidRoomFeeInvoices", preview.unpaidRoomFeeInvoices());
        details.put("unpaidRoomUtilityInvoices", preview.unpaidRoomUtilityInvoices());
        details.put("unsettledTickets", preview.unsettledTickets());
        details.put("isRoomLeader", preview.isRoomLeader());
        details.put("warnings", preview.warnings());
        return new BusinessException(
                ErrorCode.MOVE_OUT_WARNINGS_NOT_ACKNOWLEDGED, HttpStatus.CONFLICT, details);
    }

    private static Map<String, Object> auditValue(
            Long contractId,
            MoveOutDtos.Preview preview,
            long unpaidRoomFee,
            String assetCondition,
            String assetNote) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put(ApiField.CONTRACT_ID, contractId);
        value.put(ApiField.STATUS, preview.endStatus());
        value.put("depositBalance", preview.depositBalance());
        value.put("refundAmount", preview.refundAmount());
        value.put("unpaidRoomFee", unpaidRoomFee);
        value.put(ApiField.ASSET_CONDITION, assetCondition);
        if (assetNote != null) {
            value.put(ApiField.ASSET_NOTE, assetNote);
        }
        return value;
    }

    /** Hợp đồng và đơn vị nguyện vọng đọc trong một lần; không lộ entity ra ngoài service. */
    private record ContractRow(
            Long id,
            String code,
            Long studentId,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            Long preferredUnitId) {}
}
