package vn.edu.tlu.dms.account.service.impl;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.api.CatalogRequest;
import vn.edu.tlu.dms.account.api.CatalogSearch;
import vn.edu.tlu.dms.account.service.CatalogService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Action;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.Catalog;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional
public class CatalogServiceImpl implements CatalogService {
    private record Definition(String table, int codeLength, int nameLength, String relation) {}

    private static final Map<String, Definition> DEFINITIONS = Map.of(
            Catalog.SCHOOLS, new Definition("school", 30, 200, null),
            Catalog.FACULTIES, new Definition("faculty", 30, 100, "school_id"),
            Catalog.COHORTS, new Definition("student_cohort", 30, 10, null),
            Catalog.HOMETOWNS, new Definition("hometown", 30, 200, "parent_id"),
            Catalog.ACADEMIC_YEARS, new Definition("academic_year", 9, 200, null),
            Catalog.AMENITIES, new Definition("amenity", 30, 500, null),
            Catalog.MAINTENANCE_CATEGORIES, new Definition("maintenance_category", 30, 200, null));
    private final NamedParameterJdbcTemplate jdbc;
    private final ScopeGuard guard;
    private final CurrentUser currentUser;
    private final AuditService audit;

    public CatalogServiceImpl(NamedParameterJdbcTemplate jdbc, ScopeGuard guard,
            CurrentUser currentUser, AuditService audit) {
        this.jdbc = jdbc;
        this.guard = guard;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @Override
    public PageResponse<Map<String, Object>> searchByPage(String kind, CatalogSearch input) {
        guard.requirePermission(Permission.CATALOG_READ);
        Definition d = definition(kind);
        int page = input.page() == null ? 0 : input.page();
        int size = input.size() == null ? 20 : input.size();
        if (page < 0 || size < 1 || size > 100) {
            throw BusinessException.invalid(ApiField.PAGE, ValidationMessage.PAGE_RANGE);
        }
        validateRelations(kind, input.parentId(), input.schoolId());
        StringBuilder where = new StringBuilder(" WHERE true");
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (input.q() != null && !input.q().isBlank()) {
            where.append(" AND (strpos(lower(code), lower(:q)) > 0 OR strpos(lower(name), lower(:q)) > 0)");
            parameters.addValue("q", input.q().trim());
        }
        if (input.active() != null) {
            where.append(" AND active = :active");
            parameters.addValue("active", input.active());
        }
        Long relationId = input.parentId() != null ? input.parentId() : input.schoolId();
        if (relationId != null) {
            positive(relationId);
            where.append(" AND ").append(d.relation()).append(" = :relation");
            parameters.addValue("relation", relationId);
        }
        String order = sort(input.sort());
        Long total = jdbc.queryForObject("SELECT count(*) FROM " + d.table() + where, parameters, Long.class);
        parameters.addValue("limit", size).addValue("offset", (long) page * size);
        var rows = jdbc.queryForList("SELECT * FROM " + d.table() + where + " ORDER BY " + order
                + " LIMIT :limit OFFSET :offset", parameters);
        return new PageResponse<>(rows.stream().map(this::response).toList(), page, size,
                total, (int) ((total + size - 1) / size));
    }

    @Override
    public Map<String, Object> getById(String kind, Long id) {
        guard.requirePermission(Permission.CATALOG_READ);
        return response(row(definition(kind), id, ""));
    }

    @Override
    public Map<String, Object> getReferenceById(String kind, Long id) {
        return response(row(definition(kind), id, ""));
    }

    @Override
    public Map<String, Object> saveOrUpdate(String kind, CatalogRequest input) {
        boolean creating = input.id() == null;
        guard.requirePermission(creating ? Permission.CATALOG_CREATE : Permission.CATALOG_UPDATE);
        Definition d = definition(kind);
        validateRelations(kind, input.parentId(), input.schoolId());
        // Serialize hierarchy writers before acquiring row locks, including concurrent reparenting.
        jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtext(:kind))", Map.of("kind", "catalog:" + kind));
        Map<String, Object> before = creating ? null : row(d, input.id(), " FOR UPDATE");
        String code = text(input.code(), before, ApiField.CODE, d.codeLength());
        String name = text(input.name(), before, ApiField.NAME, d.nameLength());
        if (!creating && !code.equals(before.get(ApiField.CODE))) {
            throw BusinessException.invalid(ApiField.CODE, Catalog.IMMUTABLE_CODE);
        }
        if (Catalog.ACADEMIC_YEARS.equals(kind)
                && (!code.matches("[0-9]{4}-[0-9]{4}")
                || Integer.parseInt(code.substring(5)) != Integer.parseInt(code.substring(0, 4)) + 1)) {
            throw BusinessException.invalid(ApiField.CODE, Catalog.INVALID_YEAR);
        }
        boolean active = input.active() != null ? input.active()
                : creating || Boolean.TRUE.equals(before.get(ApiField.ACTIVE));
        Long relation = input.parentId() != null ? input.parentId() : input.schoolId();
        if (relation == null && !creating && d.relation() != null) {
            relation = (Long) before.get(d.relation());
        }
        if (!creating && Catalog.FACULTIES.equals(kind) && !Objects.equals(relation, before.get(d.relation()))
                && jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM student_profile WHERE faculty_id=:id)",
                        Map.of("id", input.id()), Boolean.class)) {
            throw BusinessException.conflict(ErrorCode.RESOURCE_IN_USE);
        }
        if (relation != null) {
            positive(relation);
            if (Catalog.HOMETOWNS.equals(kind)) {
                checkHierarchy(input.id(), relation);
                if (active) requireActive(Catalog.HOMETOWNS, relation);
            } else if (active) {
                requireActive(Catalog.SCHOOLS, relation);
            }
        }
        if (!active) checkActiveChildren(kind, input.id());
        var parameters = new MapSqlParameterSource().addValue("code", code).addValue("name", name)
                .addValue("active", active).addValue("relation", relation).addValue("id", input.id());
        Long id;
        if (creating) {
            id = jdbc.queryForObject("INSERT INTO " + d.table() + " (code,name,active"
                    + (d.relation() == null ? "" : "," + d.relation()) + ") VALUES (:code,:name,:active"
                    + (d.relation() == null ? "" : ",:relation") + ") RETURNING id", parameters, Long.class);
        } else {
            jdbc.update("UPDATE " + d.table() + " SET name=:name, active=:active"
                    + (d.relation() == null ? "" : "," + d.relation() + "=:relation") + " WHERE id=:id", parameters);
            id = input.id();
        }
        Map<String, Object> after = row(d, id, "");
        audit.record(currentUser.get().id(), creating ? AuditAction.CATALOG_CREATE : AuditAction.CATALOG_UPDATE,
                EntityType.CATALOG, id, auditValue(kind, before), auditValue(kind, after), null);
        return response(after);
    }

    @Override
    public void delete(String kind, Long id) {
        guard.requirePermission(Permission.CATALOG_DELETE);
        Definition d = definition(kind);
        jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtext(:kind))", Map.of("kind", "catalog:" + kind));
        Map<String, Object> before = row(d, id, " FOR UPDATE");
        jdbc.update("DELETE FROM " + d.table() + " WHERE id=:id", Map.of("id", id));
        audit.record(currentUser.get().id(), AuditAction.CATALOG_DELETE, EntityType.CATALOG, id,
                auditValue(kind, before), null, null);
    }

    @Override
    public Map<String, Object> requireActive(String kind, Long id) {
        Definition d = definition(kind);
        if (id == null) return null;
        positive(id);
        var rows = jdbc.queryForList("SELECT * FROM " + d.table() + " WHERE id=:id FOR SHARE", Map.of("id", id));
        if (rows.isEmpty() || !Boolean.TRUE.equals(rows.get(0).get(ApiField.ACTIVE))) {
            throw BusinessException.invalid(ApiField.ID, Catalog.INVALID_VALUE);
        }
        return response(rows.get(0));
    }

    @Override
    public Long resolveImport(String kind, String value) {
        Definition d = definition(kind);
        if (value == null || value.isBlank()) return null;
        var rows = jdbc.queryForList("SELECT * FROM " + d.table()
                + " WHERE (code=:value OR name=:value) FOR SHARE", Map.of("value", value));
        if (rows.size() != 1 || !Boolean.TRUE.equals(rows.get(0).get(ApiField.ACTIVE))) {
            throw BusinessException.invalid(ApiField.NAME, Catalog.INVALID_VALUE);
        }
        return ((Number) rows.get(0).get(ApiField.ID)).longValue();
    }

    @Override
    public void requireActiveCode(String kind, String code) {
        Definition d = definition(kind);
        if (code == null) throw BusinessException.invalid(ApiField.CODE, Catalog.INVALID_VALUE);
        var rows = jdbc.queryForList("SELECT * FROM " + d.table() + " WHERE code=:code FOR SHARE", Map.of("code", code));
        if (rows.size() != 1 || !Boolean.TRUE.equals(rows.get(0).get(ApiField.ACTIVE))) {
            throw BusinessException.invalid(ApiField.CODE, Catalog.INVALID_VALUE);
        }
    }

    private Definition definition(String kind) {
        Definition d = DEFINITIONS.get(kind);
        if (d == null) throw BusinessException.notFound();
        return d;
    }

    private Map<String, Object> row(Definition d, Long id, String lock) {
        positive(id);
        var rows = jdbc.queryForList("SELECT * FROM " + d.table() + " WHERE id=:id" + lock, Map.of("id", id));
        if (rows.isEmpty()) throw BusinessException.notFound();
        return rows.get(0);
    }

    private void positive(Long id) {
        if (id == null || id <= 0) throw BusinessException.invalid(ApiField.ID, ValidationMessage.ID_POSITIVE);
    }

    private String text(String value, Map<String, Object> before, String field, int max) {
        String result = value == null && before != null ? (String) before.get(field) : value;
        if (result == null || result.isBlank() || result.length() > max) {
            throw BusinessException.invalid(field, Catalog.INVALID_VALUE);
        }
        return value == null ? result : result.trim();
    }

    private void validateRelations(String kind, Long parentId, Long schoolId) {
        if ((parentId != null && !Catalog.HOMETOWNS.equals(kind))
                || (schoolId != null && !Catalog.FACULTIES.equals(kind))) {
            throw BusinessException.invalid(ApiField.PARENT_ID, Catalog.INVALID_HIERARCHY);
        }
    }

    private void checkHierarchy(Long id, Long parentId) {
        var ancestors = jdbc.queryForList("WITH RECURSIVE ancestors AS (SELECT id,parent_id FROM hometown WHERE id=:parent"
                + " UNION SELECT h.id,h.parent_id FROM hometown h JOIN ancestors a ON h.id=a.parent_id) SELECT id FROM ancestors",
                Map.of("parent", parentId), Long.class);
        if (ancestors.isEmpty() || (id != null && ancestors.contains(id))) {
            throw BusinessException.invalid(ApiField.PARENT_ID, Catalog.INVALID_HIERARCHY);
        }
    }

    private void checkActiveChildren(String kind, Long id) {
        if (id == null) return;
        String sql = Catalog.HOMETOWNS.equals(kind) ? "SELECT count(*) FROM hometown WHERE parent_id=:id AND active"
                : Catalog.SCHOOLS.equals(kind) ? "SELECT count(*) FROM faculty WHERE school_id=:id AND active" : null;
        if (sql != null && jdbc.queryForObject(sql, Map.of("id", id), Long.class) > 0) {
            throw BusinessException.conflict(ErrorCode.RESOURCE_IN_USE);
        }
    }

    private String sort(List<String> values) {
        if (values == null || values.isEmpty()) return "id ASC";
        if (values.size() > 4) throw BusinessException.invalid(ApiField.SORT, ValidationMessage.SORT_FIELD_INVALID);
        var orders = new ArrayList<String>();
        for (String value : values) orders.add(sortItem(value));
        return String.join(", ", orders) + ", id ASC";
    }

    private String sortItem(String value) {
        if (value == null) throw BusinessException.invalid(ApiField.SORT, ValidationMessage.SORT_REQUIRED);
        String[] parts = value.split(",", -1);
        if (parts.length > 2 || !Set.of("id", "code", "name", "active").contains(parts[0])) {
            throw BusinessException.invalid(ApiField.SORT, ValidationMessage.SORT_FIELD_INVALID);
        }
        String direction = parts.length == 1 ? "asc" : parts[1];
        if (!Set.of("asc", "desc").contains(direction)) {
            throw BusinessException.invalid(ApiField.SORT, ValidationMessage.SORT_DIRECTION_INVALID);
        }
        return parts[0] + " " + direction;
    }

    private Map<String, Object> response(Map<String, Object> row) {
        var result = new LinkedHashMap<>(row);
        if (result.containsKey("parent_id")) {
            Object parentId = result.remove("parent_id");
            result.put(ApiField.PARENT_ID, parentId);
            if (parentId != null) result.put(ApiField.PARENT_NAME, jdbc.queryForObject(
                    "SELECT name FROM hometown WHERE id=:id", Map.of("id", parentId), String.class));
        }
        if (result.containsKey("school_id")) {
            Object schoolId = result.remove("school_id");
            result.put(ApiField.SCHOOL_ID, schoolId);
            if (schoolId != null) result.put(ApiField.SCHOOL_NAME, jdbc.queryForObject(
                    "SELECT name FROM school WHERE id=:id", Map.of("id", schoolId), String.class));
        }
        var actions = new ArrayList<String>();
        if (guard.hasPermission(Permission.CATALOG_UPDATE)) actions.add(Action.UPDATE);
        if (guard.hasPermission(Permission.CATALOG_DELETE)) actions.add(Action.DELETE);
        result.put(ApiField.ALLOWED_ACTIONS, List.copyOf(actions));
        return result;
    }

    private Object auditValue(String kind, Map<String, Object> row) {
        if (row == null) return null;
        var value = new LinkedHashMap<String, Object>(row);
        value.put(ApiField.KIND, kind);
        return value;
    }
}
