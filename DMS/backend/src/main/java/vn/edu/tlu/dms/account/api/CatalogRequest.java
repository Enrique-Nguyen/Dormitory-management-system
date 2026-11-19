package vn.edu.tlu.dms.account.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants.CatalogValue;
import vn.edu.tlu.dms.common.constants.DmsConstants.Facility;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;

import java.time.LocalDate;

/**
 * Body lưu danh mục: không có ID là tạo, có ID là sửa từng phần. Trường null giữ nguyên, chuỗi rỗng
 * xóa trường văn bản tùy chọn. Mỗi loại danh mục chỉ nhận nhóm trường của mình; trường thuộc loại
 * khác bị từ chối ở Service.
 */
public record CatalogRequest(
        @Positive(message = ValidationMessage.ID_POSITIVE) Long id,
        @Size(max = 30) String code,
        @Size(max = 500) String name,
        Boolean active,
        @Positive(message = ValidationMessage.ID_POSITIVE) Long parentId,
        @Positive(message = ValidationMessage.ID_POSITIVE) Long schoolId,
        @Size(max = 30) String shortName,
        @Size(max = 10) String moetCode,
        @Size(max = 300) String address,
        Boolean homeSchool,
        @Size(max = 15) @Pattern(regexp = Facility.PHONE_PATTERN) String phone,
        @Size(max = 200) @Pattern(regexp = Facility.EMAIL_PATTERN) String email,
        @Min(CatalogValue.MIN_ENROLLMENT_YEAR) @Max(2100) Short enrollmentYear,
        @Min(CatalogValue.MIN_ENROLLMENT_YEAR) @Max(2110) Short expectedGraduationYear,
        @Pattern(regexp = CatalogValue.ADMIN_LEVEL_PATTERN) String adminLevel,
        @Size(max = 10) String gsoCode,
        @Pattern(regexp = CatalogValue.PRIORITY_AREA_PATTERN) String priorityArea,
        Boolean hardshipArea,
        LocalDate startDate,
        LocalDate endDate,
        @Pattern(regexp = CatalogValue.APPLIES_TO_PATTERN) String appliesTo,
        @Size(max = 50) String icon,
        @Size(max = 500) String description,
        @Pattern(regexp = CatalogValue.SEVERITY_PATTERN) String defaultSeverity,
        @Min(1) @Max(CatalogValue.MAX_SLA_HOURS) Short slaHours,
        @Pattern(regexp = CatalogValue.SKILL_PATTERN) String skill,
        @Size(max = 1000) String note) {

    /** Tương thích lời gọi chỉ có mã, tên, trạng thái và quan hệ cha. */
    public CatalogRequest(Long id, String code, String name, Boolean active, Long parentId, Long schoolId) {
        this(id, code, name, active, parentId, schoolId, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
