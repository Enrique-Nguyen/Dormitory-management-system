package vn.edu.tlu.dms.contract.service.impl;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public StorageLocationServiceImpl(NamedParameterJdbcTemplate jdbc, ScopeGuard guard,
            CurrentUser current, AuditService audit, ManagementUnitService units) {
        this.jdbc = jdbc;
        this.guard = guard;
        this.current = current;
        this.audit = audit;
        this.units = units;
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
        var args = new MapSqlParameterSource().addValue("unit", unitId).addValue("code", code)
                .addValue("name", name).addValue("active", active).addValue("id", input.id());
        Long id;
        try {
            if (creating) {
                id = jdbc.queryForObject("INSERT INTO storage_location(management_unit_id,code,name,active)"
                        + " VALUES(:unit,:code,:name,:active) RETURNING id", args, Long.class);
            } else {
                jdbc.update("UPDATE storage_location SET name=:name,active=:active WHERE id=:id", args);
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
        return new StorageLocationDtos.Location(((Number) row.get(ApiField.ID)).longValue(), unit(row),
                (String) row.get(ApiField.CODE), (String) row.get(ApiField.NAME),
                Boolean.TRUE.equals(row.get(ApiField.ACTIVE)), List.copyOf(actions));
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
            if (parts.length > 2 || !Set.of("id", "code", "name", "active", "managementUnitId").contains(parts[0])) {
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
