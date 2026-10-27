package vn.edu.tlu.dms.support;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

/**
 * Tiện ích dựng học kỳ cho tích hợp test trên PostgreSQL. Học kỳ được cấp phát theo hai ràng buộc
 * của bảng {@code semester}: khóa {@code UNIQUE(academic_year, type)} và ràng buộc loại trừ
 * {@code EXCLUDE USING gist (daterange(start_date, end_date))}. Cách cấp phát này đúng cả trên CSDL
 * rỗng (Testcontainers) lẫn CSDL local đã có dữ liệu mẫu, nhờ đó các lớp test không cần giả định
 * "bảng semester trống".
 *
 * <p>Ngày bắt đầu luôn lấy sau {@code MAX(end_date)} hiện có (nên không chồng với bất kỳ học kỳ nào),
 * còn nhãn năm học được dò tăng dần cho tới khi chưa dùng cho loại học kỳ đang yêu cầu. Mọi bước đều
 * dùng SELECT kiểm tra trước rồi mới INSERT một lần, tránh làm hỏng transaction của test đang bọc.
 */
public final class FutureSemesterFixture {

    /** Số ngày của một học kỳ dựng sẵn cho test khi lời gọi không yêu cầu độ dài riêng. */
    private static final int DEFAULT_LENGTH_DAYS = 30;

    /** Bước nhảy khi phải dò nhãn năm học khác, đủ lớn để luôn vượt các học kỳ đã có. */
    private static final int PROBE_SHIFT_DAYS = 400;

    /** Giới hạn vòng dò để báo lỗi rõ ràng thay vì lặp vô hạn. */
    private static final int MAX_PROBE = 200;

    private FutureSemesterFixture() {}

    /** Học kỳ đã tạo kèm cửa sổ ngày để test dùng lại (ví dụ so sánh ngày gia hạn). */
    public record Created(
            Long id,
            String academicYear,
            String type,
            String name,
            LocalDate startDate,
            LocalDate endDate) {}

    /** Tạo học kỳ tương lai không trùng ràng buộc và trả về id. */
    public static Long insert(JdbcTemplate jdbc, String type, String name) {
        return create(jdbc, type, name, DEFAULT_LENGTH_DAYS).id();
    }

    /** Tạo học kỳ tương lai với độ dài cho trước và trả về id. */
    public static Long insert(JdbcTemplate jdbc, String type, String name, int lengthDays) {
        return create(jdbc, type, name, lengthDays).id();
    }

    /** Tạo học kỳ tương lai không trùng ràng buộc và trả về toàn bộ dữ liệu đã ghi. */
    public static Created create(JdbcTemplate jdbc, String type, String name, int lengthDays) {
        LocalDate start = jdbc.queryForObject(
                "SELECT COALESCE(MAX(end_date), CURRENT_DATE + 365) + 1 FROM semester",
                LocalDate.class);
        for (int probe = 0; probe < MAX_PROBE; probe++) {
            LocalDate end = start.plusDays(lengthDays);
            String academicYear = yearLabel(start, end);
            if (!academicYearUsed(jdbc, academicYear, type)) {
                Long id = jdbc.queryForObject(
                        "INSERT INTO semester(academic_year, type, name, start_date, end_date)"
                                + " VALUES(?,?,?,?,?) RETURNING id",
                        Long.class,
                        academicYear,
                        type,
                        truncate(name),
                        start,
                        end);
                return new Created(id, academicYear, type, truncate(name), start, end);
            }
            start = start.plusDays(PROBE_SHIFT_DAYS);
        }
        throw new IllegalStateException("Không cấp phát được học kỳ tương lai cho loại " + type);
    }

    /** Nhãn năm học đúng mẫu {@code ^\d{4}-\d{4}$} và dài vừa đủ cho cột VARCHAR(9). */
    private static String yearLabel(LocalDate start, LocalDate end) {
        return String.format(
                "%04d-%04d", start.getYear(), Math.max(start.getYear(), end.getYear()) + 1);
    }

    private static boolean academicYearUsed(JdbcTemplate jdbc, String academicYear, String type) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM semester WHERE academic_year = ? AND type = ?)",
                        Boolean.class,
                        academicYear,
                        type));
    }

    /** Cột {@code semester.name} chỉ 50 ký tự; tên test thừa ký tự thì cắt an toàn. */
    private static String truncate(String name) {
        return name.length() <= 50 ? name : name.substring(0, 50);
    }
}
