package vn.edu.tlu.dms.billing.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.api.UtilityReadingController.Input;
import vn.edu.tlu.dms.billing.service.UtilityReadingService;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.support.SqlPage;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public Object searchReadings(int page, int size, String q, LocalDate month) {
        guard.requireRole("STAFF");
        var args = new ArrayList<Object>();
        String scope = current.get().crossUnitAccess() ? "TRUE" : "bd.management_unit_id=?";
        if (!current.get().crossUnitAccess()) {
            args.add(current.get().unitId());
        }
        args.add("%" + q + "%");
        if (month != null) {
            args.add(month.withDayOfMonth(1));
        }
        return queries.search(
                "SELECT ur.*,r.code AS room_code,EXISTS(SELECT 1 FROM invoice_item ii WHERE"
                    + " ii.utility_reading_id=ur.id) AS invoiced FROM utility_reading ur JOIN room"
                    + " r ON r.id=ur.room_id JOIN building bd ON bd.id=r.building_id WHERE "
                        + scope
                        + " AND r.code ILIKE ?"
                        + (month == null ? "" : " AND ur.billing_month=?"),
                args,
                page,
                size);
    }

    @Override
    public Map<String, Object> getReading(Long id) {
        guard.requireRole("STAFF");
        var row =
                queries.one(
                        "SELECT ur.*,EXISTS(SELECT 1 FROM invoice_item ii WHERE"
                            + " ii.utility_reading_id=ur.id) AS invoiced FROM utility_reading ur"
                            + " WHERE ur.id=?",
                        id);
        Long roomId = ((Number) row.get("roomId")).longValue();
        guard.requireReadableUnit(rooms.getRoomLocation(roomId).unitId());
        row.put(
                "allowedActions",
                !Boolean.TRUE.equals(row.get("invoiced"))
                                && rooms.getRoomLocation(roomId)
                                        .unitId()
                                        .equals(current.get().unitId())
                        ? List.of("edit", "delete")
                        : List.of());
        return row;
    }

    private void requireEditable(Map<String, Object> row) {
        guard.requireUnit(rooms.getRoomLocation(((Number) row.get("roomId")).longValue()).unitId());
        if (Boolean.TRUE.equals(row.get("invoiced"))) {
            throw BusinessException.conflict("READING_LOCKED");
        }
        if (Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM utility_reading WHERE room_id=? AND"
                                + " meter_type=? AND billing_month>?)",
                        Boolean.class,
                        row.get("roomId"),
                        row.get("meterType"),
                        row.get("billingMonth")))) {
            throw BusinessException.conflict("READING_HAS_FOLLOWING_MONTH");
        }
    }

    @Transactional
    @Override
    public Object saveReading(Long id, Input input) {
        guard.requireRole("STAFF");
        guard.requireUnit(rooms.getRoomLocation(input.roomId()).unitId());
        jdbc.queryForObject(
                "SELECT id FROM room WHERE id=? FOR UPDATE", Long.class, input.roomId());
        if (input.billingMonth().getDayOfMonth() != 1) {
            throw BusinessException.invalid("billingMonth", "Dùng ngày đầu tháng");
        }
        Map<String, Object> before = null;
        if (id != null) {
            jdbc.queryForObject(
                    "SELECT id FROM utility_reading WHERE id=? FOR UPDATE", Long.class, id);
            before = getReading(id);
            requireEditable(before);
            if (!before.get("roomId").equals(input.roomId())
                    || !before.get("meterType").equals(input.meterType())
                    || !before.get("billingMonth").equals(input.billingMonth())) {
                throw BusinessException.invalid(
                        "roomId", "Không đổi phòng, loại đồng hồ hoặc kỳ của bản ghi");
            }
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
            throw BusinessException.invalid("startReading", "Tháng đầu phải nhập chỉ số đầu");
        }
        if (input.oldMeterFinal() == null
                && (input.newMeterStart() != null
                        || input.replacementReason() != null
                                && !input.replacementReason().isBlank())) {
            throw BusinessException.invalid("oldMeterFinal", "Thiếu chỉ số chốt đồng hồ cũ");
        }
        String replacement = input.oldMeterFinal() == null ? null : input.replacementReason();
        if (input.oldMeterFinal() == null
                ? input.endReading() < start
                : input.newMeterStart() == null
                        || input.oldMeterFinal() < start
                        || input.endReading() < input.newMeterStart()
                        || replacement == null
                        || replacement.isBlank()) {
            throw BusinessException.conflict("READING_INVALID");
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
                before == null ? "READING_CREATE" : "READING_UPDATE",
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
        guard.requireRole("STAFF");
        jdbc.queryForObject("SELECT id FROM utility_reading WHERE id=? FOR UPDATE", Long.class, id);
        var row = getReading(id);
        requireEditable(row);
        jdbc.update("DELETE FROM utility_reading WHERE id=?", id);
        audit.record(current.get().id(), "READING_DELETE", "UTILITY_READING", id, row, null, null);
    }
}
