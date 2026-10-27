package vn.edu.tlu.dms.contract.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentEndReason;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.StayRequestStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.contract.api.StorageDtos;
import vn.edu.tlu.dms.contract.domain.StorageRecord;
import vn.edu.tlu.dms.contract.domain.StorageRecordChangedEvent;
import vn.edu.tlu.dms.contract.repository.StorageRecordRepository;
import vn.edu.tlu.dms.contract.service.StorageService;
import vn.edu.tlu.dms.contract.service.SummerLeaveLookup;
import vn.edu.tlu.dms.contract.service.SummerLeaveLookup.SummerLeave;
import vn.edu.tlu.dms.room.service.BedOccupancyService;
import vn.edu.tlu.dms.room.service.OccupancyQuery;
import vn.edu.tlu.dms.room.service.OccupancyQuery.CurrentBed;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cài đặt nghiệp vụ gửi đồ hè (FR-CON-11) và nhận lại đồ (FR-CON-12). Chỉ tạo phiếu khi lựa chọn
 * TEMP_LEAVE đã duyệt và hợp đồng đã được gia hạn qua học kỳ hè; hợp đồng vẫn ACTIVE sau khi đóng
 * phân phòng.
 */
@Service
@Transactional(readOnly = true)
public class StorageServiceImpl implements StorageService {

    private static final String STORAGE_REASON = "Tạm rời hè";

    private static final String RETURN_NOTE = "Nhận lại đồ gửi hè";

    private static final String SELECT_CONTRACT =
            """
            SELECT c.id, c.code, c.student_id, c.end_date, c.status, ap.preferred_unit_id
            FROM dormitory_contract c
            JOIN room_application ap ON ap.id = c.application_id
            """;

    private static final String SELECT_STORAGE =
            """
            SELECT s.id, s.contract_id, s.summer_choice_id, s.previous_bed_id, s.package_count,
                   s.description, s.location, s.deposited_at, s.received_by, s.returned_at,
                   s.returned_by, c.student_id, c.status AS contract_status
            FROM storage_record s
            JOIN dormitory_contract c ON c.id = s.contract_id
            WHERE s.id = ?
            """;

    private final JdbcTemplate jdbc;

    private final StorageRecordRepository storageRecords;

    private final SummerLeaveLookup summerLeaves;

    private final OccupancyQuery occupancyQuery;

    private final BedOccupancyService occupancy;

    private final AuditService audit;

    private final ApplicationEventPublisher events;

    private final ScopeGuard guard;

    private final CurrentUser current;

    public StorageServiceImpl(
            JdbcTemplate jdbc,
            StorageRecordRepository storageRecords,
            SummerLeaveLookup summerLeaves,
            OccupancyQuery occupancyQuery,
            BedOccupancyService occupancy,
            AuditService audit,
            ApplicationEventPublisher events,
            ScopeGuard guard,
            CurrentUser current) {
        this.jdbc = jdbc;
        this.storageRecords = storageRecords;
        this.summerLeaves = summerLeaves;
        this.occupancyQuery = occupancyQuery;
        this.occupancy = occupancy;
        this.audit = audit;
        this.events = events;
        this.guard = guard;
        this.current = current;
    }

    @Override
    @Transactional
    public StorageDtos.Record create(Long contractId, StorageDtos.CreateRequest input) {
        guard.requirePermission(Permission.STORAGE_CREATE);
        guard.requireRole(Role.STAFF);
        validateCreate(input);

        ContractRow contract = loadContract(contractId);
        CurrentBed bed = occupancyQuery.findOpenAssignmentByContract(contractId).orElse(null);
        guard.requireUnit(bed == null ? contract.preferredUnitId() : bed.managementUnitId());
        requireActive(contract);
        SummerLeave summerLeave = requireSummerLeave(contractId);
        requireExtended(contract, summerLeave);
        requireNoStorage(contractId, summerLeave.summerChoiceId());
        if (bed == null) {
            throw BusinessException.conflict(ErrorCode.NOT_STAYING);
        }

        // Thứ tự khóa: giường → sinh viên → hợp đồng; mọi điều kiện được kiểm tra lại sau khóa.
        occupancy.lockOpenAssignment(contractId, contract.studentId());
        ContractRow locked = lockContract(contractId);
        requireActive(locked);
        CurrentBed lockedBed =
                occupancyQuery
                        .findOpenAssignmentByContract(contractId)
                        .orElseThrow(() -> BusinessException.conflict(ErrorCode.NOT_STAYING));
        SummerLeave lockedLeave = requireSummerLeave(contractId);
        requireExtended(locked, lockedLeave);
        requireNoStorage(contractId, lockedLeave.summerChoiceId());

        Actor actor = current.get();
        StorageRecord record = new StorageRecord();
        record.setContractId(contractId);
        record.setSummerChoiceId(lockedLeave.summerChoiceId());
        record.setPreviousBedId(lockedBed.bedId());
        record.setPackageCount(input.packageCount());
        record.setDescription(input.description().strip());
        record.setLocation(input.location().strip());
        record.setDepositedAt(input.depositedAt());
        record.setReceivedBy(actor.id());
        record = storageRecords.saveAndFlush(record);

        // Hợp đồng giữ ACTIVE; chỉ phân phòng bị đóng để giường dùng được trong hè.
        occupancy
                .release(contractId, AssignmentEndReason.SUMMER_LEAVE, actor.id(), STORAGE_REASON)
                .orElseThrow(() -> BusinessException.conflict(ErrorCode.NOT_STAYING));

        audit.record(
                actor.id(),
                AuditAction.STORAGE_CREATE,
                EntityType.STORAGE,
                record.getId(),
                null,
                storageAuditValue(record),
                STORAGE_REASON);
        events.publishEvent(
                new StorageRecordChangedEvent(
                        record.getId(),
                        contractId,
                        locked.studentId(),
                        BusinessAction.CREATE_STORAGE_RECORD));
        return toRecord(record, locked.studentId());
    }

    @Override
    @Transactional
    public StorageDtos.Record returnStorage(
            Long storageRecordId, StorageDtos.ReturnRequest input) {
        guard.requirePermission(Permission.STORAGE_UPDATE);
        guard.requireRole(Role.STAFF);
        validateReturn(input);

        StorageRow row = lockStorage(storageRecordId);
        var previousBed =
                occupancyQuery
                        .findBedLocation(row.previousBedId())
                        .orElseThrow(BusinessException::notFound);
        guard.requireUnit(previousBed.managementUnitId());
        if (row.returnedAt() != null) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
        if (!ContractStatus.ACTIVE.equals(row.contractStatus())) {
            throw BusinessException.conflict(ErrorCode.CONTRACT_NOT_ACTIVE);
        }
        var bedLocation =
                occupancyQuery
                        .findBedLocation(input.bedId())
                        .orElseThrow(BusinessException::notFound);
        guard.requireUnit(bedLocation.managementUnitId());

        occupancy.occupyStay(
                input.bedId(), row.studentId(), row.contractId(), RETURN_NOTE);

        Actor actor = current.get();
        int updated =
                jdbc.update(
                        "UPDATE storage_record SET returned_at=?,returned_by=?"
                                + " WHERE id=? AND returned_at IS NULL",
                        input.returnedAt(),
                        actor.id(),
                        storageRecordId);
        if (updated == 0) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }

        Map<String, Object> before = new LinkedHashMap<>();
        before.put(ApiField.CONTRACT_ID, row.contractId());
        before.put(ApiField.BED_ID, row.previousBedId());
        Map<String, Object> after = new LinkedHashMap<>(before);
        after.put(ApiField.BED_ID, input.bedId());
        after.put(ApiField.RETURNED_AT, input.returnedAt().toString());
        after.put("returnedBy", actor.id());
        audit.record(
                actor.id(),
                AuditAction.STORAGE_RETURN,
                EntityType.STORAGE,
                storageRecordId,
                before,
                after,
                RETURN_NOTE);
        events.publishEvent(
                new StorageRecordChangedEvent(
                        storageRecordId,
                        row.contractId(),
                        row.studentId(),
                        BusinessAction.RETURN_STORAGE));

        return new StorageDtos.Record(
                row.id(),
                row.contractId(),
                row.studentId(),
                row.summerChoiceId(),
                row.previousBedId(),
                row.packageCount(),
                row.description(),
                row.location(),
                row.depositedAt(),
                row.receivedBy(),
                input.returnedAt(),
                actor.id());
    }

    /** Phiếu đang mở là phiếu chưa ghi returned_at; mỗi hợp đồng tối đa một phiếu mở. */
    @Override
    public Long findOpenRecordIdByContract(Long contractId) {
        if (contractId == null) {
            return null;
        }
        List<Long> ids =
                jdbc.queryForList(
                        "SELECT id FROM storage_record WHERE contract_id=?"
                                + " AND returned_at IS NULL ORDER BY id DESC",
                        Long.class,
                        contractId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private ContractRow loadContract(Long contractId) {
        return jdbc
                .query(SELECT_CONTRACT + " WHERE c.id=?", this::mapContract, contractId)
                .stream()
                .findFirst()
                .orElseThrow(BusinessException::notFound);
    }

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
                rs.getObject("end_date", LocalDate.class),
                rs.getString("status"),
                rs.getLong("preferred_unit_id"));
    }

    private StorageRow lockStorage(Long storageRecordId) {
        return jdbc
                .query(
                        SELECT_STORAGE + " FOR UPDATE OF s", this::mapStorage, storageRecordId)
                .stream()
                .findFirst()
                .orElseThrow(BusinessException::notFound);
    }

    private StorageRow mapStorage(ResultSet rs, int rowNum) throws SQLException {
        return new StorageRow(
                rs.getLong("id"),
                rs.getLong("contract_id"),
                rs.getLong("student_id"),
                rs.getLong("summer_choice_id"),
                rs.getLong("previous_bed_id"),
                rs.getShort("package_count"),
                rs.getString("description"),
                rs.getString("location"),
                rs.getObject("deposited_at", LocalDate.class),
                rs.getLong("received_by"),
                rs.getObject("returned_at", LocalDate.class),
                rs.getObject("returned_by", Long.class),
                rs.getString("contract_status"));
    }

    private SummerLeave requireSummerLeave(Long contractId) {
        return summerLeaves
                .findApprovedTempLeave(contractId)
                .orElseThrow(() -> BusinessException.conflict(ErrorCode.SUMMER_CHOICE_NOT_APPROVED));
    }

    /** Hợp đồng phải còn hiệu lực tới sau học kỳ hè, hoặc có yêu cầu gia hạn đã duyệt vượt mốc. */
    private void requireExtended(ContractRow contract, SummerLeave summerLeave) {
        if (contract.endDate().isAfter(summerLeave.semesterEnd())) {
            return;
        }
        Boolean approved =
                jdbc.queryForObject(
                        """
                        SELECT EXISTS (
                            SELECT 1 FROM contract_extension_request
                            WHERE contract_id=? AND status=? AND new_end_date>?)
                        """,
                        Boolean.class,
                        contract.id(),
                        StayRequestStatus.APPROVED,
                        summerLeave.semesterEnd());
        if (!Boolean.TRUE.equals(approved)) {
            throw BusinessException.conflict(ErrorCode.TEMP_LEAVE_NOT_EXTENDED);
        }
    }

    /** Một lựa chọn hè chỉ sinh một phiếu và mỗi hợp đồng tối đa một phiếu chưa nhận lại. */
    private void requireNoStorage(Long contractId, Long summerChoiceId) {
        Boolean exists =
                jdbc.queryForObject(
                        """
                        SELECT EXISTS (
                            SELECT 1 FROM storage_record
                            WHERE summer_choice_id=? OR (contract_id=? AND returned_at IS NULL))
                        """,
                        Boolean.class,
                        summerChoiceId,
                        contractId);
        if (Boolean.TRUE.equals(exists)) {
            throw BusinessException.conflict(ErrorCode.STORAGE_RECORD_EXISTS);
        }
    }

    private static void requireActive(ContractRow contract) {
        if (!ContractStatus.ACTIVE.equals(contract.status())) {
            throw BusinessException.conflict(ErrorCode.CONTRACT_NOT_ACTIVE);
        }
    }

    private static void validateCreate(StorageDtos.CreateRequest input) {
        if (input == null || input.packageCount() == null || input.packageCount() <= 0) {
            throw BusinessException.invalid(
                    ApiField.PACKAGE_COUNT, ValidationMessage.PACKAGE_COUNT_POSITIVE);
        }
        if (input.description() == null || input.description().isBlank()) {
            throw BusinessException.invalid(
                    ApiField.DESCRIPTION, ValidationMessage.STORAGE_DESCRIPTION_REQUIRED);
        }
        if (input.location() == null || input.location().isBlank()) {
            throw BusinessException.invalid(
                    ApiField.LOCATION, ValidationMessage.STORAGE_LOCATION_REQUIRED);
        }
        if (input.depositedAt() == null) {
            throw BusinessException.invalid(
                    ApiField.DEPOSITED_AT, ValidationMessage.DEPOSITED_DATE_REQUIRED);
        }
    }

    private static void validateReturn(StorageDtos.ReturnRequest input) {
        if (input == null || input.bedId() == null) {
            throw BusinessException.invalid(ApiField.BED_ID, ValidationMessage.BED_REQUIRED);
        }
        if (input.returnedAt() == null) {
            throw BusinessException.invalid(
                    ApiField.RETURNED_AT, ValidationMessage.RETURNED_DATE_REQUIRED);
        }
    }

    private static Map<String, Object> storageAuditValue(StorageRecord record) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put(ApiField.CONTRACT_ID, record.getContractId());
        value.put("summerChoiceId", record.getSummerChoiceId());
        value.put(ApiField.BED_ID, record.getPreviousBedId());
        value.put(ApiField.PACKAGE_COUNT, record.getPackageCount());
        value.put(ApiField.DESCRIPTION, record.getDescription());
        value.put(ApiField.LOCATION, record.getLocation());
        value.put(ApiField.DEPOSITED_AT, record.getDepositedAt().toString());
        return value;
    }

    private static StorageDtos.Record toRecord(StorageRecord record, Long studentId) {
        return new StorageDtos.Record(
                record.getId(),
                record.getContractId(),
                studentId,
                record.getSummerChoiceId(),
                record.getPreviousBedId(),
                record.getPackageCount(),
                record.getDescription(),
                record.getLocation(),
                record.getDepositedAt(),
                record.getReceivedBy(),
                record.getReturnedAt(),
                record.getReturnedBy());
    }

    /** Hợp đồng và đơn vị nguyện vọng đọc cùng lúc; không trả entity ra ngoài service. */
    private record ContractRow(
            Long id,
            String code,
            Long studentId,
            LocalDate endDate,
            String status,
            Long preferredUnitId) {}

    /** Dòng phiếu gửi đồ kèm sinh viên và trạng thái hợp đồng. */
    private record StorageRow(
            Long id,
            Long contractId,
            Long studentId,
            Long summerChoiceId,
            Long previousBedId,
            Short packageCount,
            String description,
            String location,
            LocalDate depositedAt,
            Long receivedBy,
            LocalDate returnedAt,
            Long returnedBy,
            String contractStatus) {}
}
