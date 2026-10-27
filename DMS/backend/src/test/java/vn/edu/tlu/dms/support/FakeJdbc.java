package vn.edu.tlu.dms.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * JdbcTemplate giả cho unit test thuần (không Spring context, không CSDL). Mỗi câu SQL được khai báo
 * trước một tập dòng dựng sẵn thông qua dấu nhận dạng trong câu SQL; mọi câu lệnh đã thực thi được
 * ghi lại để test kiểm tra số lần ghi và tham số.
 *
 * <p>Dấu nhận dạng được xét theo thứ tự đăng ký, đăng ký sớm hơn thì ưu tiên, nhờ vậy có thể khai
 * "FOR UPDATE" trước rồi khai bản đọc không khóa.
 */
public class FakeJdbc extends JdbcTemplate {

    private final List<Route> routes = new ArrayList<>();

    private final List<String> statements = new ArrayList<>();

    private int affectedRows = 1;

    /**
     * Đăng ký tập dòng trả về cho mọi câu SQL có chứa {@code marker}.
     *
     * @param marker đoạn văn bản phân biệt câu SQL trong Service
     * @param rows các dòng Map (tên cột như trong SELECT) trả về theo thứ tự
     * @return chính đối tượng này để nối lệnh đăng ký
     */
    public FakeJdbc returning(String marker, List<Map<String, Object>> rows) {
        Route replacement = new Route(marker, List.copyOf(rows));
        for (int index = 0; index < routes.size(); index++) {
            if (routes.get(index).marker().equals(marker)) {
                // Thay tại chỗ để giữ thứ tự ưu tiên đã đăng ký (bản FOR UPDATE trước bản đọc).
                routes.set(index, replacement);
                return this;
            }
        }
        routes.add(replacement);
        return this;
    }

    /** Đăng ký một dòng duy nhất cho {@code marker}, dùng cho hầu hết các test một bản ghi. */
    public FakeJdbc returningOne(String marker, Map<String, Object> row) {
        return returning(marker, List.of(row));
    }

    /** Số dòng bị ảnh hưởng trả về cho mọi lệnh {@code update}; 0 dùng để mô phỏng xung đột khóa. */
    public FakeJdbc affecting(int rows) {
        this.affectedRows = rows;
        return this;
    }

    /** Toàn bộ câu SQL đã thực thi kèm tham số, theo đúng thứ tự Service gọi. */
    public List<String> statements() {
        return List.copyOf(statements);
    }

    /** Chỉ các câu lệnh ghi (UPDATE/INSERT/DELETE) để kiểm tra "không ghi gì ngoài dự kiến". */
    public List<String> writes() {
        return statements.stream()
                .filter(
                        statement ->
                                statement.startsWith("UPDATE ")
                                        || statement.startsWith("INSERT ")
                                        || statement.startsWith("DELETE "))
                .toList();
    }

    /** Câu lệnh ghi đầu tiên chứa {@code marker}, hoặc null khi không có; hữu ích cho assert gọn. */
    public String writeContaining(String marker) {
        return writes().stream().filter(statement -> statement.contains(marker)).findFirst().orElse(null);
    }

    /** Số bản ghi đọc được đăng ký cho một câu SQL; kiểm tra xem Service có thực sự đọc lại. */
    public List<Map<String, Object>> rowsFor(String sql) {
        String normalized = sql.toUpperCase(Locale.ROOT);
        for (Route route : routes) {
            if (normalized.contains(route.marker().toUpperCase(Locale.ROOT))) {
                return route.rows();
            }
        }
        throw new IllegalStateException(
                "Chưa chuẩn bị dữ liệu giả cho câu SQL: "
                        + sql
                        + " (đã đăng ký: "
                        + routes.stream().map(Route::marker).toList()
                        + ")");
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
        record(sql, args);
        List<T> results = new ArrayList<>();
        int index = 0;
        for (Map<String, Object> row : rowsFor(sql)) {
            try {
                results.add(rowMapper.mapRow(resultSet(row), index++));
            } catch (java.sql.SQLException exception) {
                throw new IllegalStateException(
                        "RowMapper thật lỗi khi đọc dòng giả " + row, exception);
            }
        }
        return results;
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Object... args) {
        record(sql, args);
        return rowsFor(sql).stream()
                .map(row -> (Map<String, Object>) new LinkedHashMap<String, Object>(row))
                .toList();
    }

    @Override
    public <T> List<T> queryForList(String sql, Class<T> elementType, Object... args) {
        record(sql, args);
        return rowsFor(sql).stream().map(row -> convert(firstValue(row), elementType)).toList();
    }

    @Override
    public <T> T queryForObject(String sql, Class<T> elementType, Object... args) {
        record(sql, args);
        List<Map<String, Object>> rows = rowsFor(sql);
        if (rows.isEmpty()) {
            throw new IllegalStateException("Câu SQL giả không trả dòng nào: " + sql);
        }
        return convert(firstValue(rows.get(0)), elementType);
    }

    @Override
    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) {
        record(sql, args);
        List<Map<String, Object>> rows = rowsFor(sql);
        if (rows.isEmpty()) {
            throw new IllegalStateException("Câu SQL giả không trả dòng nào: " + sql);
        }
        try {
            return rowMapper.mapRow(resultSet(rows.get(0)), 0);
        } catch (java.sql.SQLException exception) {
            throw new IllegalStateException("RowMapper thật lỗi khi đọc dòng giả", exception);
        }
    }

    @Override
    public int update(String sql, Object... args) {
        record(sql, args);
        return affectedRows;
    }

    private void record(String sql, Object... args) {
        StringBuilder line = new StringBuilder(sql);
        for (Object arg : args) {
            line.append(" | ").append(arg);
        }
        statements.add(line.toString());
    }

    private static Object firstValue(Map<String, Object> row) {
        return row.values().iterator().next();
    }

    /** Dựng ResultSet đọc theo tên hoặc theo thứ tự cột (1-based) từ một Map đã chuẩn bị. */
    public static ResultSet resultSet(Map<String, Object> row) {
        return (ResultSet)
                Proxy.newProxyInstance(
                        ResultSet.class.getClassLoader(),
                        new Class<?>[] {ResultSet.class},
                        new RowHandler(row));
    }

    @SuppressWarnings("unchecked")
    private static <T> T convert(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        if (type == String.class) {
            return (T) String.valueOf(value);
        }
        if (type == Long.class && value instanceof Number number) {
            return (T) (Long) number.longValue();
        }
        if (type == Integer.class && value instanceof Number number) {
            return (T) (Integer) number.intValue();
        }
        if (type == Boolean.class) {
            return (T) (Boolean) truthy(value);
        }
        throw new IllegalStateException(
                "Không ép được giá trị giả " + value + " sang " + type.getName());
    }

    private static boolean truthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean flag) {
            return flag;
        }
        if (value instanceof Number number) {
            return number.longValue() != 0L;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private record Route(String marker, List<Map<String, Object>> rows) {}

    /** Chỉn vài getter JDBC mà RowMapper của project dùng; gọi ngoài dự kiến sẽ báo lỗi rõ ràng. */
    private static final class RowHandler implements InvocationHandler {

        private final Map<String, Object> row;

        private RowHandler(Map<String, Object> row) {
            this.row = row;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            Object[] arguments = args == null ? new Object[0] : args;
            return switch (method.getName()) {
                case "toString" -> "FakeResultSet" + row;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == arguments[0];
                case "wasNull" -> false;
                case "getLong" -> longValue(read(arguments));
                case "getInt" -> Math.toIntExact(longValue(read(arguments)));
                case "getShort" -> (short) longValue(read(arguments));
                case "getBoolean" -> truthy(read(arguments));
                case "getString" -> text(read(arguments));
                case "getObject" -> arguments.length == 1
                        ? read(arguments)
                        : convert(read(arguments), (Class<?>) arguments[1]);
                default ->
                        throw new UnsupportedOperationException(
                                "ResultSet giả chưa hỗ trợ "
                                        + method.getName()
                                        + " trên cột "
                                        + (arguments.length == 0 ? "?" : String.valueOf(arguments[0])));
            };
        }

        private Object read(Object[] arguments) {
            Object key = arguments[0];
            if (key instanceof String name) {
                if (!row.containsKey(name)) {
                    throw new IllegalStateException(
                            "Cột '"
                                    + name
                                    + "' chưa chuẩn bị. Các cột có sẵn: "
                                    + row.keySet());
                }
                return row.get(name);
            }
            int index = ((Integer) key) - 1;
            List<Object> values = new ArrayList<>(row.values());
            if (index < 0 || index >= values.size()) {
                throw new IllegalStateException(
                        "Cột thứ tự " + (index + 1) + " vượt quá số cột đã chuẩn bị " + values.size());
            }
            return values.get(index);
        }

        private static long longValue(Object value) {
            if (value == null) {
                return 0L;
            }
            if (value instanceof Number number) {
                return number.longValue();
            }
            return Long.parseLong(String.valueOf(value));
        }

        private static String text(Object value) {
            return value == null ? null : String.valueOf(value);
        }
    }

    /** Dựng một dòng theo cặp "tên cột, giá trị" liên tiếp, giữ thứ tự cột như khai báo. */
    public static Map<String, Object> row(Object... columnNameAndValue) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < columnNameAndValue.length; index += 2) {
            values.put(String.valueOf(columnNameAndValue[index]), columnNameAndValue[index + 1]);
        }
        return values;
    }
}
