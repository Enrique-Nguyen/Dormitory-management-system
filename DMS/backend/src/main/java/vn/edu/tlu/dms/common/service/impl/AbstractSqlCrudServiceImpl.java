package vn.edu.tlu.dms.common.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;

import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.support.SqlPage;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Khung CRUD cho màn hình chưa có nghiệp vụ chi tiết. Tên bảng/cột do server khai báo; không ghép
 * tên trường, ORDER BY hoặc SQL do client cung cấp vào truy vấn. Service module phải kiểm tra quyền
 * và trạng thái trước khi gọi thao tác ghi.
 */
public abstract class AbstractSqlCrudServiceImpl {

    protected final JdbcTemplate jdbc;

    protected final SqlPage queries;

    protected final CurrentUser current;

    protected final AuditService audit;

    protected AbstractSqlCrudServiceImpl(
            JdbcTemplate jdbc, SqlPage queries, CurrentUser current, AuditService audit) {
        this.jdbc = jdbc;
        this.queries = queries;
        this.current = current;
        this.audit = audit;
    }

    protected Object insertRecord(
            String table,
            Map<String, String> fields,
            Set<String> required,
            Map<String, Object> input,
            Map<String, Object> defaults) {
        var columns = convertFields(fields, input);
        for (String field : required) {
            if (!input.containsKey(field)
                    || input.get(field) == null
                    || input.get(field) instanceof String text && text.isBlank()) {
                throw BusinessException.invalid(field, ValidationMessage.REQUIRED);
            }
        }
        columns.putAll(defaults);
        String names = String.join(",", columns.keySet());
        String placeholders = String.join(",", Collections.nCopies(columns.size(), "?"));
        Long id =
                jdbc.queryForObject(
                        "INSERT INTO "
                                + table
                                + " ("
                                + names
                                + ") VALUES ("
                                + placeholders
                                + ") RETURNING id",
                        Long.class,
                        columns.values().toArray());
        audit.record(
                current.get().id(),
                AuditAction.CRUD_CREATE,
                table.toUpperCase(Locale.ROOT),
                id,
                null,
                input,
                null);
        return queries.one("SELECT * FROM " + table + " WHERE id=?", id);
    }

    protected Object updateRecord(
            String table, Long id, Map<String, String> fields, Map<String, Object> input) {
        var values = convertFields(fields, input);
        if (values.isEmpty()) {
            throw BusinessException.invalid("form", "Chưa có trường để cập nhật");
        }
        var before = queries.one("SELECT * FROM " + table + " WHERE id=?", id);
        var args = new ArrayList<>(values.values());
        args.add(id);
        String assignments =
                String.join(",", values.keySet().stream().map(key -> key + "=?").toList());
        jdbc.update("UPDATE " + table + " SET " + assignments + " WHERE id=?", args.toArray());
        audit.record(
                current.get().id(),
                AuditAction.CRUD_UPDATE,
                table.toUpperCase(Locale.ROOT),
                id,
                before,
                input,
                null);
        return queries.one("SELECT * FROM " + table + " WHERE id=?", id);
    }

    protected void deleteRecord(String table, Long id) {
        var before = queries.one("SELECT * FROM " + table + " WHERE id=?", id);
        jdbc.update("DELETE FROM " + table + " WHERE id=?", id);
        audit.record(
                current.get().id(),
                AuditAction.CRUD_DELETE,
                table.toUpperCase(Locale.ROOT),
                id,
                before,
                null,
                null);
    }

    /**
     * Kiểu trường được định nghĩa tại server: :date/:time/:long/:text. NULL không ghi đè khi PATCH.
     */
    private Map<String, Object> convertFields(
            Map<String, String> fields, Map<String, Object> input) {
        var converted = new LinkedHashMap<String, Object>();
        for (var entry : input.entrySet()) {
            String definition = fields.get(entry.getKey());
            if (definition == null) {
                throw BusinessException.invalid(entry.getKey(), "Trường không được phép ghi");
            }
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            String[] parts = definition.split(":");
            try {
                value =
                        switch (parts[1]) {
                            case "date" -> LocalDate.parse(value.toString());
                            case "time" -> OffsetDateTime.parse(value.toString());
                            case "long" -> Long.parseLong(value.toString());
                            case "text" -> value.toString().trim();
                            default -> throw new IllegalArgumentException("Kiểu chưa hỗ trợ");
                        };
            } catch (IllegalArgumentException | DateTimeParseException e) {
                throw BusinessException.invalid(entry.getKey(), "Giá trị không đúng định dạng");
            }
            if (value instanceof String text && (text.isBlank() || text.length() > 500)) {
                throw BusinessException.invalid(entry.getKey(), "Nhập từ 1 đến 500 ký tự");
            }
            converted.put(parts[0], value);
        }
        return converted;
    }
}
