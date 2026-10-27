package vn.edu.tlu.dms.support;

import org.springframework.jdbc.core.JdbcTemplate;

import vn.edu.tlu.dms.common.support.SqlPage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SqlPage giả dùng chung đăng ký dữ liệu với {@link FakeJdbc}, phục vụ các Service kế thừa
 * AbstractSqlCrudServiceImpl (vừa gọi {@code queries.one} vừa gọi {@code jdbc.update}).
 *
 * <p>Mỗi lần đọc trả về một Map bản sao vì nghiệp vụ có thể bổ sung khóa dẫn xuất vào chính Map vừa
 * đọc (xem {@code SummerCrudServiceImpl.attachContract}).
 */
public class FakeSqlPage extends SqlPage {

    private final FakeJdbc database;

    private final List<String> reads = new ArrayList<>();

    public FakeSqlPage(FakeJdbc database) {
        super(new JdbcTemplate());
        this.database = database;
    }

    /** Câu SQL đọc qua SqlPage kèm tham số, theo thứ tự Service gọi. */
    public List<String> statements() {
        return List.copyOf(reads);
    }

    @Override
    public Map<String, Object> one(String sql, Object... args) {
        reads.add(join(sql, args));
        List<Map<String, Object>> rows = database.rowsFor(sql);
        if (rows.isEmpty()) {
            throw new IllegalStateException("Câu SQL giả không trả dòng nào: " + sql);
        }
        return new LinkedHashMap<>(rows.get(0));
    }

    @Override
    public List<Map<String, Object>> list(String sql, Object... args) {
        reads.add(join(sql, args));
        return database.rowsFor(sql).stream()
                .map(row -> (Map<String, Object>) new LinkedHashMap<String, Object>(row))
                .toList();
    }

    private static String join(String sql, Object... args) {
        StringBuilder line = new StringBuilder(sql);
        for (Object arg : args) {
            line.append(" | ").append(arg);
        }
        return line.toString();
    }
}
