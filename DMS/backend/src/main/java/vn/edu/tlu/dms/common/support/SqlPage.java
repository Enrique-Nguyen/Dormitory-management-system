package vn.edu.tlu.dms.common.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Truy vấn màn quản lý: SQL do server định nghĩa, mọi giá trị từ client đều bind bằng dấu ?. */
@Component
public class SqlPage {

    private final JdbcTemplate jdbc;

    public SqlPage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PageResponse<Map<String, Object>> search(String sql, List<?> args, int page, int size) {
        PageQuery.of(page, size, null, Set.of("id"), "id,desc");
        Long count =
                jdbc.queryForObject(
                        "SELECT count(*) FROM (" + sql + ") results", Long.class, args.toArray());
        var parameters = new ArrayList<Object>(args);
        parameters.add(size);
        parameters.add((long) page * size);
        var rows =
                jdbc.query(
                        "SELECT * FROM (" + sql + ") results ORDER BY id DESC LIMIT ? OFFSET ?",
                        SqlPage::mapRow,
                        parameters.toArray());
        return new PageResponse<>(rows, page, size, count, (int) ((count + size - 1) / size));
    }

    /** Phân trang và sắp xếp tại CSDL; chỉ cột server cho phép mới được đưa vào ORDER BY. */
    public PageResponse<Map<String, Object>> search(
            String sql, List<?> args, SearchRequest input, Set<String> sortable, String fallback) {
        var pageable = PageQuery.of(input.page(), input.size(), input.sort(), sortable, fallback);
        String ordering =
                pageable.getSort().stream()
                        .map(
                                order ->
                                        toSqlColumn(order.getProperty())
                                                + " "
                                                + order.getDirection().name())
                        .collect(Collectors.joining(", "));
        Long count =
                jdbc.queryForObject(
                        "SELECT count(*) FROM (" + sql + ") results", Long.class, args.toArray());
        var parameters = new ArrayList<Object>(args);
        parameters.add(input.size());
        parameters.add(pageable.getOffset());
        var rows =
                jdbc.query(
                        "SELECT * FROM ("
                                + sql
                                + ") results ORDER BY "
                                + ordering
                                + " LIMIT ? OFFSET ?",
                        SqlPage::mapRow,
                        parameters.toArray());
        return new PageResponse<>(
                rows,
                input.page(),
                input.size(),
                count,
                (int) ((count + input.size() - 1) / input.size()));
    }

    /** Tên thuộc tính đã qua whitelist; chuẩn hóa camelCase về alias snake_case trong SQL. */
    private static String toSqlColumn(String property) {
        return property.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
    }

    public Map<String, Object> one(String sql, Object... args) {
        return jdbc.query(sql, SqlPage::mapRow, args).stream()
                .findFirst()
                .orElseThrow(BusinessException::notFound);
    }

    public List<Map<String, Object>> list(String sql, Object... args) {
        return jdbc.query(sql, SqlPage::mapRow, args);
    }

    /** Chuẩn hóa snake_case sang camelCase và ngày/giờ sang chuỗi JSON; không trả mật khẩu. */
    private static Map<String, Object> mapRow(ResultSet row, int index) throws SQLException {
        var result = new LinkedHashMap<String, Object>();
        var metadata = row.getMetaData();
        for (int column = 1; column <= metadata.getColumnCount(); column++) {
            String label = metadata.getColumnLabel(column);
            StringBuilder key = new StringBuilder();
            boolean uppercase = false;
            for (char ch : label.toCharArray()) {
                if (ch == '_') {
                    uppercase = true;
                } else {
                    key.append(uppercase ? Character.toUpperCase(ch) : ch);
                    uppercase = false;
                }
            }
            Object value =
                    metadata.getColumnTypeName(column).equals("timestamptz")
                            ? row.getObject(column, OffsetDateTime.class)
                            : switch (metadata.getColumnType(column)) {
                                case Types.DATE -> row.getObject(column, LocalDate.class);
                                case Types.TIMESTAMP_WITH_TIMEZONE ->
                                        row.getObject(column, OffsetDateTime.class);
                                default -> row.getObject(column);
                            };
            result.put(key.toString(), value);
        }
        return result;
    }
}
