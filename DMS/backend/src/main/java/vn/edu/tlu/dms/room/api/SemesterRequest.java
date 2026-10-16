package vn.edu.tlu.dms.room.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants.SemesterType;

import java.time.LocalDate;

/** ID trống để tạo mới; có ID để sửa từng phần. Trường không gửi giữ nguyên khi sửa. */
public record SemesterRequest(
        @Positive Long id,
        @Size(max = 50) String name,
        @Pattern(regexp = "[0-9]{4}-[0-9]{4}") String academicYear,
        @Pattern(regexp = SemesterType.PATTERN) String type,
        LocalDate startDate,
        LocalDate endDate) {

    /** Tương thích lời gọi tạo mới trong mã Java; API dùng body có trường ID tùy chọn. */
    public SemesterRequest(
            String name, String academicYear, String type, LocalDate startDate, LocalDate endDate) {
        this(null, name, academicYear, type, startDate, endDate);
    }
}
