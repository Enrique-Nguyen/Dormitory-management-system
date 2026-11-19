package vn.edu.tlu.dms.room.service.impl;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.Catalog;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Facility;
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
import vn.edu.tlu.dms.room.api.FloorRequest;
import vn.edu.tlu.dms.room.service.FloorService;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class FloorServiceImpl implements FloorService {
    private static final String SELECT = "SELECT f.id,f.building_id,f.floor,f.name,f.active,f.gender,f.shared_bathroom,f.common_room,f.note,(SELECT COUNT(r.id) FROM room r WHERE r.building_id=f.building_id AND r.floor=f.floor) AS room_count,b.name AS building_name,b.code AS building_code,b.management_unit_id FROM building_floor f JOIN building b ON b.id=f.building_id";
    private final JdbcTemplate jdbc;
    private final SqlPage queries;
    private final CurrentUser current;
    private final ScopeGuard guard;
    private final AuditService audit;

    public FloorServiceImpl(JdbcTemplate jdbc, SqlPage queries, CurrentUser current, ScopeGuard guard, AuditService audit) {
        this.jdbc = jdbc;
        this.queries = queries;
        this.current = current;
        this.guard = guard;
        this.audit = audit;
    }

    @Override
    public PageResponse<Map<String, Object>> searchByPage(SearchRequest input) {
        guard.requirePermission(Permission.CATALOG_READ);
        guard.requireRole(Role.STAFF);
        StringBuilder sql = new StringBuilder(SELECT + " WHERE true");
        var args = new ArrayList<Object>();
        if (!current.get().canReadAllUnits()) {
            sql.append(" AND b.management_unit_id=?");
            args.add(current.get().unitId());
        }
        if (input.buildingId() != null) {
            sql.append(" AND f.building_id=?");
            args.add(input.buildingId());
        }
        if (input.floor() != null) {
            sql.append(" AND f.floor=?");
            args.add(input.floor());
        }
        if (input.name() != null && !input.name().isBlank()) {
            sql.append(" AND strpos(lower(f.name),lower(?))>0");
            args.add(input.name().trim());
        }
        if (input.active() != null) {
            sql.append(" AND f.active=?");
            args.add(input.active());
        }
        sql.append(" AND (f.name ILIKE ? OR f.floor::text ILIKE ? OR b.code ILIKE ?)");
        String keyword = LikePattern.contains(input.q().trim());
        args.add(keyword);
        args.add(keyword);
        args.add(keyword);
        return queries.search(sql.toString(), args, input, Set.of("id", "name", "floor", "buildingId", "active"), "floor,asc");
    }

    @Override
    public Map<String, Object> getById(Long id) {
        guard.requirePermission(Permission.CATALOG_READ);
        guard.requireRole(Role.STAFF);
        return accessible(id, false);
    }

    @Override
    @Transactional
    public Map<String, Object> saveOrUpdate(FloorRequest input) {
        boolean creating = input.id() == null;
        guard.requirePermission(creating ? Permission.CATALOG_CREATE : Permission.CATALOG_UPDATE);
        guard.requireRole(Role.STAFF);
        Map<String, Object> before = creating ? null : accessible(input.id(), true);
        Long buildingId = creating ? input.buildingId() : ((Number) before.get(ApiField.BUILDING_ID)).longValue();
        Short floor = creating ? input.floor() : ((Number) before.get("floor")).shortValue();
        if (buildingId == null) throw BusinessException.invalid(ApiField.BUILDING_ID, ValidationMessage.REQUIRED);
        if (floor == null || floor < 0) throw BusinessException.invalid("floor", ValidationMessage.REQUIRED);
        if (!creating && ((input.buildingId() != null && !Objects.equals(input.buildingId(), buildingId))
                || (input.floor() != null && !Objects.equals(input.floor(), floor)))) {
            throw BusinessException.invalid(ApiField.BUILDING_ID, ValidationMessage.ROOM_BUILDING_IMMUTABLE);
        }
        var building = queries.one(
                "SELECT management_unit_id,floor_count,has_ground_floor,gender_policy FROM building"
                        + " WHERE id=? FOR SHARE",
                buildingId);
        guard.requireUnit(((Number) building.get("managementUnitId")).longValue());
        int floorCount = ((Number) building.get("floorCount")).intValue();
        if (creating && (floor > floorCount || floor == 0 && !Boolean.TRUE.equals(building.get("hasGroundFloor")))) {
            // Tầng phải nằm trong quy mô tòa đã khai: 1..số tầng, tầng 0 chỉ khi tòa có tầng trệt.
            throw new BusinessException(ErrorCode.FLOOR_OUT_OF_RANGE, HttpStatus.BAD_REQUEST);
        }
        String name = input.name() != null ? input.name().trim() : creating ? null : (String) before.get(ApiField.NAME);
        if (name == null || name.isBlank()) throw BusinessException.invalid(ApiField.NAME, ValidationMessage.NOT_BLANK);
        boolean active = input.active() != null ? input.active() : creating || Boolean.TRUE.equals(before.get(ApiField.ACTIVE));
        String gender = input.gender() != null ? emptyToNull(input.gender()) : creating ? null : (String) before.get(ApiField.GENDER);
        String policy = (String) building.get("genderPolicy");
        if (gender != null && !Facility.GENDER_MIXED.equals(policy) && !gender.equals(policy)) {
            throw BusinessException.conflict(ErrorCode.GENDER_MISMATCH);
        }
        if (gender != null && !creating && Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM room WHERE building_id=? AND floor=? AND gender<>?)",
                Boolean.class, buildingId, floor, gender))) {
            throw BusinessException.conflict(ErrorCode.GENDER_MISMATCH);
        }
        boolean sharedBathroom = input.sharedBathroom() != null ? input.sharedBathroom()
                : !creating && Boolean.TRUE.equals(before.get("sharedBathroom"));
        boolean commonRoom = input.commonRoom() != null ? input.commonRoom()
                : !creating && Boolean.TRUE.equals(before.get("commonRoom"));
        String note = input.note() != null ? emptyToNull(input.note()) : creating ? null : (String) before.get(ApiField.NOTE);
        Long id = input.id();
        if (creating) {
            id = jdbc.queryForObject(
                    "INSERT INTO building_floor(building_id,floor,name,active,gender,shared_bathroom,common_room,note)"
                            + " VALUES (?,?,?,?,?,?,?,?) RETURNING id",
                    Long.class, buildingId, floor, name, active, gender, sharedBathroom, commonRoom, note);
        } else {
            jdbc.update(
                    "UPDATE building_floor SET name=?,active=?,gender=?,shared_bathroom=?,common_room=?,note=? WHERE id=?",
                    name, active, gender, sharedBathroom, commonRoom, note, id);
        }
        var after = accessible(id, false);
        audit.record(current.get().id(), creating ? AuditAction.CATALOG_CREATE : AuditAction.CATALOG_UPDATE,
                EntityType.CATALOG, id, before, after, null);
        return after;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        guard.requirePermission(Permission.CATALOG_DELETE);
        guard.requireRole(Role.STAFF);
        var before = accessible(id, true);
        guard.requireUnit(((Number) before.get("managementUnitId")).longValue());
        jdbc.update("DELETE FROM building_floor WHERE id=?", id);
        audit.record(current.get().id(), AuditAction.CATALOG_DELETE, EntityType.CATALOG, id, before, null, null);
    }

    @Override
    @Transactional
    public void requireActiveFloor(Long buildingId, Short floor) {
        var rows = jdbc.queryForList("SELECT f.active,b.management_unit_id FROM building_floor f JOIN building b ON b.id=f.building_id WHERE f.building_id=? AND f.floor=? FOR SHARE OF f", buildingId, floor);
        if (rows.isEmpty() || !Boolean.TRUE.equals(rows.get(0).get(ApiField.ACTIVE))) {
            throw BusinessException.invalid("floor", Catalog.INVALID_VALUE);
        }
        guard.requireUnit(((Number) rows.get(0).get("management_unit_id")).longValue());
    }

    private Map<String, Object> accessible(Long id, boolean lock) {
        if (id == null || id <= 0) throw BusinessException.invalid(ApiField.ID, ValidationMessage.ID_POSITIVE);
        var row = queries.one(SELECT + " WHERE f.id=?" + (lock ? " FOR UPDATE OF f" : ""), id);
        guard.requireReadableUnit(((Number) row.get("managementUnitId")).longValue());
        return row;
    }

    /** Chuỗi rỗng xóa trường tùy chọn; chuỗi có nội dung được cắt khoảng trắng. */
    private static String emptyToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
