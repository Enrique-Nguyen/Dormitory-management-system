package vn.edu.tlu.dms.contract.service.impl;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.service.AccountReferenceService;
import vn.edu.tlu.dms.account.service.ManagementUnitService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Action;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.Catalog;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.contract.api.StorageDtos;
import vn.edu.tlu.dms.contract.api.StorageLocationDtos;
import vn.edu.tlu.dms.contract.service.StorageLocationService;
import vn.edu.tlu.dms.room.service.RoomReferenceService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class StorageLocationServiceImpl implements StorageLocationService {
    private final NamedParameterJdbcTemplate jdbc;
    private final ScopeGuard guard;
    private final CurrentUser current;
    private final AuditService audit;
    private final ManagementUnitService units;
    private final AccountReferenceService accounts;
    private final RoomReferenceService rooms;

    public StorageLocationServiceImpl(NamedParameterJdbcTemplate jdbc, ScopeGuard guard,
            CurrentUser current, AuditService audit, ManagementUnitService units,
            AccountReferenceService accounts, RoomReferenceService rooms) {
        this.jdbc = jdbc;
        this.guard = guard;
        this.current = current;
        this.audit = audit;
        this.units = units;
        this.accounts = accounts;
        this.rooms = rooms;
    }

    @Override
    public PageResponse<StorageLocationDtos.Location> searchByPage(StorageLocationDtos.Search input) {
        requireRead();
        int page = input.page() == null ? 0 : input.page();
        int size = input.size() == null ? 20 : input.size();
        if (page < 0 || size < 1 || size > 100) {
            throw BusinessException.invalid(ApiField.PAGE, ValidationMessage.PAGE_RANGE);
        }
        StringBuilder where = new StringBuilder(" WHERE true");
        var args = new MapSqlParameterSource();
        var actor = current.get();
        if (!actor.canReadAllUnits()) {
            where.append(" AND management_unit_id=:scopeUnit");
            args.addValue("scopeUnit", actor.unitId());
        }
        if (input.managementUnitId() != null) {
            positive(input.managementUnitId(), ApiField.MANAGEMENT_UNIT_ID);
            where.append(" AND management_unit_id=:unit");
            args.addValue("unit", input.managementUnitId());
        }
        if (input.active() != null) {
            where.append(" AND active=:active");
            args.addValue("active", input.active());
        }
        if (input.code() != null && !input.code().isBlank()) {
            where.append(" AND strpos(lower(code),lower(:code))>0");
            args.addValue("code", input.code().strip());
        }
        if (input.name() != null && !input.name().isBlank()) {
            where.append(" AND strpos(lower(name),lower(:name))>0");
            args.addValue("name", input.name().strip());
        }
        if (input.q() != null && !input.q().isBlank()) {
            where.append(" AND (strpos(lower(code),lower(:q))>0 OR strpos(lower(name),lower(:q))>0)");
            args.addValue("q", input.q().strip());
        }
        String order = sort(input.sort());
        long total = jdbc.queryForObject("SELECT count(*) FROM storage_location" + where, args, Long.class);
        args.addValue("limit", size).addValue("offset", (long) page * size);
        var rows = jdbc.queryForList("SELECT * FROM storage_location" + where + " ORDER BY " + order
                + " LIMIT :limit OFFSET :offset", args);
        return new PageResponse<>(rows.stream().map(this::response).toList(), page, size,
                total, (int) ((total + size - 1) / size));
    }

    @Override
    public StorageLocationDtos.Location getById(Long id) {
        requireRead();
        var row = row(id, "");
        guard.requireReadableUnit(unit(row));
        return response(row);
    }

    @Override
    @Transactional
    public StorageLocationDtos.Location saveOrUpdate(StorageLocationDtos.SaveRequest input) {
        boolean creating = input.id() == null;
        guard.requirePermission(creating ? Permission.CATALOG_CREATE : Permission.CATALOG_UPDATE);
        guard.requireRole(Role.STAFF);
        var before = creating ? null : row(input.id(), " FOR UPDATE");
        Long unitId = creating ? input.managementUnitId() : unit(before);
        positive(unitId, ApiField.MANAGEMENT_UNIT_ID);
        guard.requireUnit(unitId);
        if (!creating && input.managementUnitId() != null && !unitId.equals(input.managementUnitId())) {
            throw BusinessException.invalid(ApiField.MANAGEMENT_UNIT_ID, Catalog.INVALID_VALUE);
        }
        String code = text(input.code(), before, ApiField.CODE, 30);
        String name = text(input.name(), before, ApiField.NAME, 100);
        if (!creating && !code.equals(before.get(ApiField.CODE))) {
            throw BusinessException.invalid(ApiField.CODE, Catalog.IMMUTABLE_CODE);
        }
        boolean active = input.active() != null ? input.active()
                : creating || Boolean.TRUE.equals(before.get(ApiField.ACTIVE));
        if (creating) units.getById(unitId);
        Long buildingId = input.buildingId() != null ? input.buildingId()
                : creating ? null : (Long) longValue(before.get("building_id"));
        if (input.buildingId() != null && !unitId.equals(rooms.getBuildingManagementUnitId(buildingId))) {
            // Kho chỉ đặt ở tòa thuộc cùng đơn vị quản lý.
            throw BusinessException.invalid(ApiField.BUILDING_ID, Catalog.INVALID_VALUE);
        }
        Long keeperId = input.keeperUserId() != null ? input.keeperUserId()
                : creating ? null : (Long) longValue(before.get("keeper_user_id"));
        if (input.keeperUserId() != null) {
            accounts.requireStaffOfUnit(keeperId, unitId);
        }
        Integer capacity = input.capacity() != null ? input.capacity()
                : creating ? null : (Integer) before.get("capacity");
        var args = new MapSqlParameterSource().addValue("unit", unitId).addValue("code", code)
                .addValue("name", name).addValue("active", active).addValue("id", input.id())
                .addValue("building", buildingId).addValue("keeper", keeperId).addValue("capacity", capacity)
                .addValue("place", optionalText(input.place(), before, "place"))
                .addValue("note", optionalText(input.note(), before, ApiField.NOTE));
        Long id;
        try {
            if (creating) {
                id = jdbc.queryForObject("INSERT INTO storage_location(management_unit_id,code,name,active,"
                        + "building_id,place,capacity,keeper_user_id,note)"
                        + " VALUES(:unit,:code,:name,:active,:building,:place,:capacity,:keeper,:note) RETURNING id",
                        args, Long.class);
            } else {
                jdbc.update("UPDATE storage_location SET name=:name,active=:active,building_id=:building,"
                        + "place=:place,capacity=:capacity,keeper_user_id=:keeper,note=:note WHERE id=:id", args);
                id = input.id();
            }
        } catch (DuplicateKeyException ex) {
            throw BusinessException.conflict(ErrorCode.DUPLICATE_VALUE);
        }
        var after = row(id, "");
        audit.record(current.get().id(), creating ? AuditAction.CATALOG_CREATE : AuditAction.CATALOG_UPDATE,
                EntityType.CATALOG, id, before, after, null);
        return response(after);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        guard.requirePermission(Permission.CATALOG_DELETE);
        guard.requireRole(Role.STAFF);
        var before = row(id, " FOR UPDATE");
        guard.requireUnit(unit(before));
        if (Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM storage_record WHERE location_id=:id)", Map.of("id", id), Boolean.class))) {
            throw BusinessException.conflict(ErrorCode.RESOURCE_IN_USE);
        }
        jdbc.update("DELETE FROM storage_location WHERE id=:id", Map.of("id", id));
        audit.record(current.get().id(), AuditAction.CATALOG_DELETE, EntityType.CATALOG, id, before, null, null);
    }

    @Override
    @Transactional
    public StorageLocationDtos.Location requireActiveForUnit(Long id, Long managementUnitId) {
        guard.requireRole(Role.STAFF);
        guard.requireUnit(managementUnitId);
        var location = row(id, " FOR SHARE");
        if (!managementUnitId.equals(unit(location))) throw BusinessException.notFound();
        if (!Boolean.TRUE.equals(location.get(ApiField.ACTIVE))) {
            throw BusinessException.invalid(StorageDtos.LOCATION_ID, Catalog.INVALID_VALUE);
        }
        return response(location);
    }

    private void requireRead() {
        guard.requirePermission(Permission.CATALOG_READ);
        guard.requireRole(Role.STAFF);
    }

    private Map<String, Object> row(Long id, String lock) {
        positive(id, ApiField.ID);
        var rows = jdbc.queryForList("SELECT * FROM storage_location WHERE id=:id" + lock, Map.of("id", id));
        if (rows.isEmpty()) throw BusinessException.notFound();
        return rows.get(0);
    }

    private static Long unit(Map<String, Object> row) {
        return ((Number) row.get("management_unit_id")).longValue();
    }

    private StorageLocationDtos.Location response(Map<String, Object> row) {
        var actor = current.get();
        boolean writable = actor.isAdmin() || (actor.unitId() != null && actor.unitId().equals(unit(row)));
        var actions = new ArrayList<String>();
        if (writable && guard.hasPermission(Permission.CATALOG_UPDATE)) actions.add(Action.UPDATE);
        if (writable && guard.hasPermission(Permission.CATALOG_DELETE)) actions.add(Action.DELETE);
        Long id = ((Number) row.get(ApiField.ID)).longValue();
        Long buildingId = longValue(row.get("building_id"));
        Long keeperId = longValue(row.get("keeper_user_id"));
        // Số kiện đang giữ là giá trị suy diễn từ phiếu chưa trả, không lưu vào danh mục.
        Long held = jdbc.queryForObject("SELECT COALESCE(SUM(package_count),0) FROM storage_record"
                + " WHERE location_id=:id AND returned_at IS NULL", Map.of("id", id), Long.class);
        return new StorageLocationDtos.Location(id, unit(row),
                (String) row.get(ApiField.CODE), (String) row.get(ApiField.NAME),
                Boolean.TRUE.equals(row.get(ApiField.ACTIVE)), List.copyOf(actions),
                accounts.unitNames(List.of(unit(row))).get(unit(row)),
                buildingId,
                buildingId == null ? null : rooms.getBuildingNames(Set.of(buildingId)).get(buildingId),
                (String) row.get("place"),
                row.get("capacity") == null ? null : ((Number) row.get("capacity")).intValue(),
                keeperId,
                keeperId == null ? null : accounts.user(keeperId).fullName(),
                (String) row.get(ApiField.NOTE),
                held == null ? 0 : held);
    }

    private static Long longValue(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    /** Null giữ giá trị cũ, chuỗi rỗng xóa trường văn bản tùy chọn. */
    private static String optionalText(String input, Map<String, Object> before, String column) {
        if (input == null) {
            return before == null ? null : (String) before.get(column);
        }
        String trimmed = input.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void positive(Long id, String field) {
        if (id == null || id <= 0) throw BusinessException.invalid(field, ValidationMessage.ID_POSITIVE);
    }

    private static String text(String input, Map<String, Object> before, String field, int max) {
        String value = input == null && before != null ? (String) before.get(field) : input;
        if (value == null || value.isBlank() || value.length() > max) {
            throw BusinessException.invalid(field, Catalog.INVALID_VALUE);
        }
        return input == null ? value : value.strip();
    }

    private static String sort(List<String> input) {
        if (input == null || input.isEmpty()) return "id ASC";
        if (input.size() > 4) throw BusinessException.invalid(ApiField.SORT, ValidationMessage.SORT_FIELD_INVALID);
        var orders = new ArrayList<String>();
        for (String value : input) {
            if (value == null) throw BusinessException.invalid(ApiField.SORT, ValidationMessage.SORT_REQUIRED);
            String[] parts = value.split(",", -1);
            if (parts.length > 2 || !Set.of("id", "code", "name", "active", "managementUnitId", "capacity").contains(parts[0])) {
                throw BusinessException.invalid(ApiField.SORT, ValidationMessage.SORT_FIELD_INVALID);
            }
            String direction = parts.length == 1 ? "asc" : parts[1];
            if (!Set.of("asc", "desc").contains(direction)) {
                throw BusinessException.invalid(ApiField.SORT, ValidationMessage.SORT_DIRECTION_INVALID);
            }
            orders.add((ApiField.MANAGEMENT_UNIT_ID.equals(parts[0]) ? "management_unit_id" : parts[0]) + " " + direction);
        }
        return String.join(", ", orders) + ", id ASC";
    }
}
