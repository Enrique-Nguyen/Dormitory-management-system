package vn.edu.tlu.dms.room.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants.SemesterType;

import java.time.LocalDate;

/** PATCH giữ nguyên trường không gửi; khi tạo mới, Service kiểm tra các trường bắt buộc. */
public record SemesterRequest(
        @Size(max = 50) String name,
        @Pattern(regexp = "[0-9]{4}-[0-9]{4}") String academicYear,
        @Pattern(regexp = SemesterType.PATTERN) String type,
        LocalDate startDate,
        LocalDate endDate) {}
