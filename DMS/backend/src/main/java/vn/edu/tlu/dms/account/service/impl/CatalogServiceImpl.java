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
import vn.edu.tlu.dms.common.constants.DmsConstants.CatalogValue;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;

import java.time.LocalDate;
import java.time.Year;
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

    private enum FieldType { TEXT, BOOLEAN, NUMBER, DATE }

    /** Trường bổ sung của một loại danh mục: tên JSON, cột CSDL, kiểu và bắt buộc khi tạo. */
    private record Extra(String field, String column, FieldType type, boolean requiredOnCreate) {}

    private static final Extra NOTE = new Extra(ApiField.NOTE, "note", FieldType.TEXT, false);

    /** Danh sách trường bổ sung theo loại (đặc tả trường nghiệp vụ đầy đủ mục 8–17, migration V18). */
    private static final Map<String, List<Extra>> EXTRAS = Map.of(
            Catalog.SCHOOLS, List.of(
                    new Extra("shortName", "short_name", FieldType.TEXT, false),
                    new Extra("moetCode", "moet_code", FieldType.TEXT, false),
                    new Extra("address", "address", FieldType.TEXT, false),
                    new Extra("homeSchool", "is_home_school", FieldType.BOOLEAN, false),
                    NOTE),
            Catalog.FACULTIES, List.of(
                    new Extra("shortName", "short_name", FieldType.TEXT, false),
                    new Extra("phone", "phone", FieldType.TEXT, false),
                    new Extra("email", "email", FieldType.TEXT, false),
                    NOTE),
            Catalog.COHORTS, List.of(
                    new Extra("enrollmentYear", "enrollment_year", FieldType.NUMBER, true),
                    new Extra("expectedGraduationYear", "expected_graduation_year", FieldType.NUMBER, false),
                    NOTE),
            Catalog.HOMETOWNS, List.of(
                    new Extra("adminLevel", "admin_level", FieldType.TEXT, false),
                    new Extra("gsoCode", "gso_code", FieldType.TEXT, false),
                    new Extra("priorityArea", "priority_area", FieldType.TEXT, false),
                    new Extra("hardshipArea", "hardship_area", FieldType.BOOLEAN, false),
                    NOTE),
            Catalog.ACADEMIC_YEARS, List.of(
                    new Extra(ApiField.START_DATE, "start_date", FieldType.DATE, true),
                    new Extra(ApiField.END_DATE, "end_date", FieldType.DATE, true),
                    NOTE),
            Catalog.AMENITIES, List.of(
                    new Extra("appliesTo", "applies_to", FieldType.TEXT, false),
                    new Extra("icon", "icon", FieldType.TEXT, false),
                    new Extra("description", "description", FieldType.TEXT, false),
                    NOTE),
            Catalog.MAINTENANCE_CATEGORIES, List.of(
                    new Extra("defaultSeverity", "default_severity", FieldType.TEXT, false),
                    new Extra("slaHours", "sla_hours", FieldType.NUMBER, false),
                    new Extra("skill", "skill", FieldType.TEXT, false),
                    new Extra("description", "description", FieldType.TEXT, false),
                    NOTE));

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
        if (input.code() != null && !input.code().isBlank()) {
            where.append(" AND strpos(lower(code), lower(:code)) > 0");
            parameters.addValue("code", input.code().trim());
        }
        if (input.name() != null && !input.name().isBlank()) {
            where.append(" AND strpos(lower(name), lower(:name)) > 0");
            parameters.addValue("name", input.name().trim());
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
        return new PageResponse<>(rows.stream().map(item -> response(kind, item)).toList(), page, size,
                total, (int) ((total + size - 1) / size));
    }

    @Override
    public Map<String, Object> getById(String kind, Long id) {
        guard.requirePermission(Permission.CATALOG_READ);
        return response(kind, row(definition(kind), id, ""));
    }

    @Override
    public Map<String, Object> getReferenceById(String kind, Long id) {
        return response(kind, row(definition(kind), id, ""));
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
        if (creating && Catalog.ACADEMIC_YEARS.equals(kind)
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
        Map<String, Object> extras = mergeExtras(kind, input, before);
        validateExtras(kind, input, before, relation, extras);
        var parameters = new MapSqlParameterSource().addValue("code", code).addValue("name", name)
                .addValue("active", active).addValue("relation", relation).addValue("id", input.id());
        extras.forEach(parameters::addValue);
        StringBuilder insertColumns = new StringBuilder();
        StringBuilder insertValues = new StringBuilder();
        StringBuilder updates = new StringBuilder();
        for (String column : extras.keySet()) {
            insertColumns.append(",").append(column);
            insertValues.append(",:").append(column);
            updates.append(", ").append(column).append("=:").append(column);
        }
        Long id;
        if (creating) {
            id = jdbc.queryForObject("INSERT INTO " + d.table() + " (code,name,active"
                    + (d.relation() == null ? "" : "," + d.relation()) + insertColumns + ") VALUES (:code,:name,:active"
                    + (d.relation() == null ? "" : ",:relation") + insertValues + ") RETURNING id", parameters, Long.class);
        } else {
            jdbc.update("UPDATE " + d.table() + " SET name=:name, active=:active"
                    + (d.relation() == null ? "" : "," + d.relation() + "=:relation") + updates + " WHERE id=:id",
                    parameters);
            id = input.id();
        }
        Map<String, Object> after = row(d, id, "");
        audit.record(currentUser.get().id(), creating ? AuditAction.CATALOG_CREATE : AuditAction.CATALOG_UPDATE,
                EntityType.CATALOG, id, auditValue(kind, before), auditValue(kind, after), null);
        return response(kind, after);
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
        return response(kind, rows.get(0));
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

    private Map<String, Object> response(String kind, Map<String, Object> row) {
        var result = new LinkedHashMap<>(row);
        // Cột bổ sung trả theo tên JSON; ngày SQL đổi sang LocalDate để serialize dạng yyyy-MM-dd.
        for (Extra extra : EXTRAS.getOrDefault(kind, List.of())) {
            if (result.containsKey(extra.column())) {
                Object value = result.remove(extra.column());
                result.put(extra.field(), value instanceof java.sql.Date date ? date.toLocalDate() : value);
            }
        }
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

    @Override
    public void requireWithinAcademicYear(String code, LocalDate startDate, LocalDate endDate) {
        var rows = jdbc.queryForList("SELECT start_date, end_date FROM academic_year WHERE code=:code",
                Map.of("code", code));
        if (rows.isEmpty() || rows.get(0).get("start_date") == null || rows.get(0).get("end_date") == null) {
            return;
        }
        LocalDate yearStart = toDate(rows.get(0).get("start_date"));
        LocalDate yearEnd = toDate(rows.get(0).get("end_date"));
        if (startDate.isBefore(yearStart) || endDate.isAfter(yearEnd)) {
            throw BusinessException.invalid(ApiField.START_DATE, Catalog.SEMESTER_OUTSIDE_YEAR);
        }
    }

    /**
     * Gộp trường bổ sung theo quy tắc sửa từng phần: null giữ giá trị cũ, chuỗi rỗng xóa trường văn bản
     * tùy chọn. Trường không thuộc loại danh mục bị từ chối để không ghi ngầm sang bảng khác.
     */
    private Map<String, Object> mergeExtras(String kind, CatalogRequest input, Map<String, Object> before) {
        List<Extra> extras = EXTRAS.getOrDefault(kind, List.of());
        Map<String, Object> requested = requestedExtras(input);
        for (var entry : requested.entrySet()) {
            if (entry.getValue() != null && extras.stream().noneMatch(extra -> extra.field().equals(entry.getKey()))) {
                throw BusinessException.invalid(entry.getKey(), Catalog.FIELD_NOT_APPLICABLE);
            }
        }
        var result = new LinkedHashMap<String, Object>();
        for (Extra extra : extras) {
            Object value = requested.get(extra.field());
            Object current = before == null ? null : before.get(extra.column());
            Object merged;
            if (value == null) {
                merged = current instanceof java.sql.Date date ? date.toLocalDate() : current;
            } else if (value instanceof String text) {
                String trimmed = text.trim();
                merged = trimmed.isEmpty() ? null : trimmed;
            } else {
                merged = value;
            }
            if (merged == null && extra.requiredOnCreate() && (before == null || value != null)) {
                throw BusinessException.invalid(extra.field(), ValidationMessage.REQUIRED);
            }
            if (merged == null && extra.type() == FieldType.BOOLEAN) {
                merged = Boolean.FALSE;
            }
            result.put(extra.column(), merged);
        }
        return result;
    }

    /** Kiểm tra ràng buộc chéo giữa các trường bổ sung trước khi ghi. */
    private void validateExtras(String kind, CatalogRequest input, Map<String, Object> before, Long relation,
            Map<String, Object> extras) {
        if (Catalog.COHORTS.equals(kind)) {
            Number enrollment = (Number) extras.get("enrollment_year");
            Number graduation = (Number) extras.get("expected_graduation_year");
            if (enrollment != null && input.enrollmentYear() != null
                    && enrollment.intValue() > Year.now().getValue() + 1) {
                throw BusinessException.invalid("enrollmentYear", Catalog.YEAR_RANGE);
            }
            if (enrollment != null && graduation != null && graduation.intValue() <= enrollment.intValue()) {
                throw BusinessException.invalid("expectedGraduationYear", Catalog.GRADUATION_AFTER_ENROLLMENT);
            }
        }
        if (Catalog.HOMETOWNS.equals(kind)) {
            String level = (String) extras.get("admin_level");
            boolean relationChanged = before == null || !Objects.equals(relation, before.get("parent_id"));
            if (level == null) {
                // Không khai cấp thì suy theo quan hệ: có cấp cha là xã, không có là tỉnh.
                level = relation == null ? CatalogValue.ADMIN_LEVEL_PROVINCE : CatalogValue.ADMIN_LEVEL_COMMUNE;
                extras.put("admin_level", level);
            }
            if (input.adminLevel() != null || relationChanged) {
                boolean province = CatalogValue.ADMIN_LEVEL_PROVINCE.equals(level);
                if (province != (relation == null) || (!province && !CatalogValue.ADMIN_LEVEL_PROVINCE.equals(
                        jdbc.queryForObject("SELECT admin_level FROM hometown WHERE id=:id",
                                Map.of("id", relation), String.class)))) {
                    throw BusinessException.invalid("adminLevel", Catalog.ADMIN_LEVEL_PARENT);
                }
            }
        }
        if (Catalog.AMENITIES.equals(kind) && extras.get("applies_to") == null) {
            extras.put("applies_to", CatalogValue.APPLIES_TO_ROOM);
        }
        if (Catalog.ACADEMIC_YEARS.equals(kind)) {
            LocalDate start = (LocalDate) extras.get("start_date");
            LocalDate end = (LocalDate) extras.get("end_date");
            String code = before == null ? input.code().trim() : (String) before.get(ApiField.CODE);
            if (start != null && end != null && !end.isAfter(start)) {
                throw BusinessException.invalid(ApiField.END_DATE, ValidationMessage.END_DATE_AFTER_START);
            }
            if (start != null && code.matches("[0-9]{4}-[0-9]{4}")
                    && start.getYear() != Integer.parseInt(code.substring(0, 4))) {
                throw BusinessException.invalid(ApiField.START_DATE, Catalog.START_YEAR_MISMATCH);
            }
        }
    }

    /** Ánh xạ tên JSON → giá trị gửi lên của mọi trường bổ sung trong body. */
    private static Map<String, Object> requestedExtras(CatalogRequest input) {
        var values = new LinkedHashMap<String, Object>();
        values.put("shortName", input.shortName());
        values.put("moetCode", input.moetCode());
        values.put("address", input.address());
        values.put("homeSchool", input.homeSchool());
        values.put("phone", input.phone());
        values.put("email", input.email());
        values.put("enrollmentYear", input.enrollmentYear());
        values.put("expectedGraduationYear", input.expectedGraduationYear());
        values.put("adminLevel", input.adminLevel());
        values.put("gsoCode", input.gsoCode());
        values.put("priorityArea", input.priorityArea());
        values.put("hardshipArea", input.hardshipArea());
        values.put(ApiField.START_DATE, input.startDate());
        values.put(ApiField.END_DATE, input.endDate());
        values.put("appliesTo", input.appliesTo());
        values.put("icon", input.icon());
        values.put("description", input.description());
        values.put("defaultSeverity", input.defaultSeverity());
        values.put("slaHours", input.slaHours());
        values.put("skill", input.skill());
        values.put(ApiField.NOTE, input.note());
        return values;
    }

    private static LocalDate toDate(Object value) {
        return value instanceof java.sql.Date date ? date.toLocalDate() : (LocalDate) value;
    }
}
