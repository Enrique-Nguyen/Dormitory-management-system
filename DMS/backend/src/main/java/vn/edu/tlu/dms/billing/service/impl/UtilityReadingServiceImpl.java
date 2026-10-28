package vn.edu.tlu.dms.billing.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.api.UtilityReadingController.Input;
import vn.edu.tlu.dms.billing.service.UtilityReadingService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.MeterType;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.support.LikePattern;
import vn.edu.tlu.dms.common.support.SqlPage;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class UtilityReadingServiceImpl implements UtilityReadingService {

    private final JdbcTemplate jdbc;

    private final SqlPage queries;

    private final CurrentUser current;

    private final ScopeGuard guard;

    private final RoomReferenceService rooms;

    private final AuditService audit;

    public UtilityReadingServiceImpl(
            JdbcTemplate jdbc,
            SqlPage queries,
            CurrentUser current,
            ScopeGuard guard,
            RoomReferenceService rooms,
            AuditService audit) {
        this.jdbc = jdbc;
        this.queries = queries;
        this.current = current;
        this.guard = guard;
        this.rooms = rooms;
        this.audit = audit;
    }

    @Override
    public PageResponse<Map<String, Object>> searchByPage(SearchRequest input) {
        guard.requirePermission(Permission.UTILITY_READING_READ);
        guard.requireRole(Role.STAFF);
        if (input.meterType() != null && !input.meterType().matches(MeterType.PATTERN)) {
            throw BusinessException.invalid(ApiField.METER_TYPE, ValidationMessage.METER_TYPE_INVALID);
        }
        var args = new ArrayList<Object>();
        String scope = current.get().canReadAllUnits() ? "TRUE" : "bd.management_unit_id=?";
        if (!current.get().canReadAllUnits()) {
            args.add(current.get().unitId());
        }
        args.add(LikePattern.contains(input.q()));
        String sql =
                "SELECT ur.*,r.code AS room_code,EXISTS(SELECT 1 FROM invoice_item ii WHERE"
                    + " ii.utility_reading_id=ur.id) AS invoiced,EXISTS(SELECT 1 FROM"
                    + " utility_reading later WHERE later.room_id=ur.room_id AND"
                    + " later.meter_type=ur.meter_type AND later.billing_month>ur.billing_month) AS"
                    + " has_following_month FROM utility_reading ur JOIN room r ON r.id=ur.room_id"
                    + " JOIN building bd ON bd.id=r.building_id WHERE "
                        + scope
                        + " AND r.code ILIKE ?";
        if (input.billingMonth() != null) {
            sql += " AND ur.billing_month=?";
            args.add(input.billingMonth().withDayOfMonth(1));
        }
        if (input.meterType() != null) {
            sql += " AND ur.meter_type=?";
            args.add(input.meterType());
        }
        if (input.buildingId() != null) {
            sql += " AND bd.id=?";
            args.add(input.buildingId());
        }
        return queries.search(
                sql,
                args,
                input,
                Set.of(
                        "id",
                        "roomId",
                        "roomCode",
                        "meterType",
                        "billingMonth",
                        "startReading",
                        "endReading",
                        "consumption",
                        "recordedAt",
                        "recordedBy",
                        "invoiced"),
                "id,desc");
    }

    @Override
    public Map<String, Object> getReading(Long id) {
        guard.requirePermission(Permission.UTILITY_READING_READ);
        guard.requireRole(Role.STAFF);
        var row =
                queries.one(
                        "SELECT ur.*,EXISTS(SELECT 1 FROM invoice_item ii WHERE"
                            + " ii.utility_reading_id=ur.id) AS invoiced,EXISTS(SELECT 1 FROM"
                            + " utility_reading later WHERE later.room_id=ur.room_id AND"
                            + " later.meter_type=ur.meter_type AND"
                            + " later.billing_month>ur.billing_month) AS has_following_month FROM"
                            + " utility_reading ur WHERE ur.id=?",
                        id);
        Long roomId = ((Number) row.get("roomId")).longValue();
        guard.requireReadableUnit(rooms.getRoomLocation(roomId).unitId());
        row.put(
                "allowedActions",
                !Boolean.TRUE.equals(row.get("invoiced"))
                                && !Boolean.TRUE.equals(row.get("hasFollowingMonth"))
                                && (current.get().isAdmin()
                                        || rooms.getRoomLocation(roomId)
                                                .unitId()
                                                .equals(current.get().unitId()))
                        ? Stream.of(BusinessAction.EDIT, BusinessAction.DELETE)
                                .filter(
                                        action ->
                                                guard.hasPermission(
                                                        action.equals(BusinessAction.EDIT)
                                                                ? Permission.UTILITY_READING_UPDATE
                                                                : Permission
                                                                        .UTILITY_READING_DELETE))
                                .toList()
                        : List.of());
        return row;
    }

    private void requireEditable(Map<String, Object> row) {
        guard.requireUnit(rooms.getRoomLocation(((Number) row.get("roomId")).longValue()).unitId());
        if (Boolean.TRUE.equals(row.get("invoiced"))) {
            throw BusinessException.conflict(ErrorCode.READING_LOCKED);
        }
        requireNoFollowingMonth(row.get("roomId"), row.get("meterType"), row.get("billingMonth"));
    }

    /** Chuỗi chỉ số theo tháng không được đứt: không thêm/sửa/xóa khi tháng sau đã có chỉ số. */
    private void requireNoFollowingMonth(Object roomId, Object meterType, Object billingMonth) {
        if (Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM utility_reading WHERE room_id=? AND"
                                + " meter_type=? AND billing_month>?)",
                        Boolean.class,
                        roomId,
                        meterType,
                        billingMonth))) {
            throw BusinessException.conflict(ErrorCode.READING_HAS_FOLLOWING_MONTH);
        }
    }

    @Transactional
    @Override
    public Map<String, Object> saveOrUpdate(Input input) {
        if (input == null) {
            throw BusinessException.invalid(ApiField.INPUT, ValidationMessage.READING_INPUT_REQUIRED);
        }
        Long id = input.id();
        guard.requirePermission(
                id == null ? Permission.UTILITY_READING_CREATE : Permission.UTILITY_READING_UPDATE);
        guard.requireRole(Role.STAFF);
        Map<String, Object> before = null;
        if (id == null) {
            validateRequiredInput(input);
            guard.requireUnit(rooms.getRoomLocation(input.roomId()).unitId());
            jdbc.queryForObject(
                    "SELECT id FROM room WHERE id=? FOR UPDATE", Long.class, input.roomId());
        } else {
            if (id <= 0) {
                throw BusinessException.invalid(ApiField.ID, ValidationMessage.ID_POSITIVE);
            }
            // Đọc phạm vi trước, nhưng phải tải lại dữ liệu sau khi lấy khóa để tránh bản cũ.
            before = getReading(id);
            Long roomId = ((Number) before.get("roomId")).longValue();
            guard.requireUnit(rooms.getRoomLocation(roomId).unitId());
            jdbc.queryForObject("SELECT id FROM room WHERE id=? FOR UPDATE", Long.class, roomId);
            lockReading(id);
            before = getReading(id);
            requireEditable(before);
            if (input.roomId() != null && !Objects.equals(input.roomId(), roomId)
                    || input.meterType() != null
                            && !Objects.equals(input.meterType(), before.get("meterType"))
                    || input.billingMonth() != null
                            && !Objects.equals(input.billingMonth(), before.get("billingMonth"))) {
                throw BusinessException.invalid(
                        ApiField.ROOM_ID, ValidationMessage.READING_KEY_IMMUTABLE);
            }
            input = mergeInput(input, before);
            validateRequiredInput(input);
        }
        if (input.billingMonth().getDayOfMonth() != 1) {
            throw BusinessException.invalid(ApiField.BILLING_MONTH, ValidationMessage.FIRST_DAY_OF_MONTH);
        }
        if (id == null) {
            requireNoFollowingMonth(input.roomId(), input.meterType(), input.billingMonth());
        }
        var previous =
                jdbc.query(
                        "SELECT end_reading FROM utility_reading WHERE room_id=? AND meter_type=?"
                                + " AND billing_month=?",
                        (r, i) -> r.getInt(1),
                        input.roomId(),
                        input.meterType(),
                        input.billingMonth().minusMonths(1));
        Integer start = previous.isEmpty() ? input.startReading() : previous.get(0);
        if (start == null) {
            throw BusinessException.invalid(ApiField.START_READING, ValidationMessage.START_READING_REQUIRED);
        }
        if (input.oldMeterFinal() == null
                && (input.newMeterStart() != null
                        || input.replacementReason() != null
                                && !input.replacementReason().isBlank())) {
            throw BusinessException.invalid(ApiField.OLD_METER_FINAL, ValidationMessage.OLD_METER_FINAL_REQUIRED);
        }
        String replacement = input.oldMeterFinal() == null ? null : input.replacementReason();
        if (input.oldMeterFinal() == null
                ? input.endReading() < start
                : input.newMeterStart() == null
                        || input.oldMeterFinal() < start
                        || input.endReading() < input.newMeterStart()
                        || replacement == null
                        || replacement.isBlank()) {
            throw BusinessException.conflict(ErrorCode.READING_INVALID);
        }
        if (id == null) {
            id =
                    jdbc.queryForObject(
                            "INSERT INTO"
                                + " utility_reading(room_id,meter_type,billing_month,start_reading,end_reading,old_meter_final,new_meter_start,replacement_reason,recorded_by)"
                                + " VALUES (?,?,?,?,?,?,?,?,?) RETURNING id",
                            Long.class,
                            input.roomId(),
                            input.meterType(),
                            input.billingMonth(),
                            start,
                            input.endReading(),
                            input.oldMeterFinal(),
                            input.newMeterStart(),
                            replacement,
                            current.get().id());
        } else {
            jdbc.update(
                    "UPDATE utility_reading SET"
                        + " start_reading=?,end_reading=?,old_meter_final=?,new_meter_start=?,replacement_reason=?,recorded_by=?,recorded_at=now()"
                        + " WHERE id=?",
                    start,
                    input.endReading(),
                    input.oldMeterFinal(),
                    input.newMeterStart(),
                    replacement,
                    current.get().id(),
                    id);
        }
        var after = getReading(id);
        audit.record(
                current.get().id(),
                before == null ? AuditAction.READING_CREATE : AuditAction.READING_UPDATE,
                "UTILITY_READING",
                id,
                before,
                after,
                null);
        return after;
    }

    @Transactional
    @Override
    public void deleteUnusedReading(Long id) {
        guard.requirePermission(Permission.UTILITY_READING_DELETE);
        guard.requireRole(Role.STAFF);
        var initial = getReading(id);
        Long roomId = ((Number) initial.get("roomId")).longValue();
        guard.requireUnit(rooms.getRoomLocation(roomId).unitId());
        jdbc.queryForObject("SELECT id FROM room WHERE id=? FOR UPDATE", Long.class, roomId);
        lockReading(id);
        var row = getReading(id);
        requireEditable(row);
        jdbc.update("DELETE FROM utility_reading WHERE id=?", id);
        audit.record(
                current.get().id(),
                AuditAction.READING_DELETE,
                "UTILITY_READING",
                id,
                row,
                null,
                null);
    }

    /** Kiểm tra bản ghi còn tồn tại sau khi chờ khóa, tránh biến bản ghi không có thành lỗi SQL. */
    private void lockReading(Long id) {
        if (jdbc.query(
                        "SELECT id FROM utility_reading WHERE id=? FOR UPDATE",
                        (row, index) -> row.getLong(1),
                        id)
                .isEmpty()) {
            throw BusinessException.notFound();
        }
    }

    private Input mergeInput(Input input, Map<String, Object> row) {
        return new Input(
                input.id(),
                ((Number) row.get("roomId")).longValue(),
                (String) row.get("meterType"),
                (LocalDate) row.get("billingMonth"),
                input.startReading() == null
                        ? integer(row.get("startReading"))
                        : input.startReading(),
                input.endReading() == null ? integer(row.get("endReading")) : input.endReading(),
                input.oldMeterFinal() == null
                        ? integer(row.get("oldMeterFinal"))
                        : input.oldMeterFinal(),
                input.newMeterStart() == null
                        ? integer(row.get("newMeterStart"))
                        : input.newMeterStart(),
                input.replacementReason() == null
                        ? (String) row.get("replacementReason")
                        : input.replacementReason());
    }

    private Integer integer(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    /** Nhánh tạo và dữ liệu sửa sau merge đều phải đủ trường và đúng giới hạn. */
    private void validateRequiredInput(Input input) {
        if (input.roomId() == null || input.roomId() <= 0) {
            throw BusinessException.invalid(ApiField.ROOM_ID, ValidationMessage.ROOM_REQUIRED);
        }
        if (input.meterType() == null || !input.meterType().matches(MeterType.PATTERN)) {
            throw BusinessException.invalid(ApiField.METER_TYPE, ValidationMessage.METER_TYPE_REQUIRED);
        }
        if (input.billingMonth() == null) {
            throw BusinessException.invalid(ApiField.BILLING_MONTH, ValidationMessage.READING_MONTH_REQUIRED);
        }
        if (input.endReading() == null) {
            throw BusinessException.invalid(ApiField.END_READING, ValidationMessage.END_READING_REQUIRED);
        }
        nonNegative("startReading", input.startReading());
        nonNegative("endReading", input.endReading());
        nonNegative("oldMeterFinal", input.oldMeterFinal());
        nonNegative("newMeterStart", input.newMeterStart());
    }

    private void nonNegative(String field, Integer value) {
        if (value != null && value < 0) {
            throw BusinessException.invalid(field, ValidationMessage.READING_NOT_NEGATIVE);
        }
    }
}
