package vn.edu.tlu.dms.common.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import vn.edu.tlu.dms.common.dto.PageResponse;
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
