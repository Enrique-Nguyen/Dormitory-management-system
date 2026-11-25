package vn.edu.tlu.dms.registration.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants.CatalogValue;

/** ID trống để tạo mới; có ID để sửa từng phần. Trường không gửi giữ nguyên khi sửa. */
public record PriorityCategoryRequest(
        @Positive Long id,
        @Size(max = 30) String code,
        @Size(max = 100) String name,
        @PositiveOrZero Integer weight,
        Boolean active,
        @Size(max = 1000) String description,
        @Size(max = 500) String evidenceRequired,
        Boolean evidenceMandatory,
        @Size(max = 300) String legalBasis,
        @PositiveOrZero Short sortOrder,
        @Pattern(regexp = CatalogValue.AUTO_RULE_PATTERN) String autoRule,
        @Size(max = 1000) String note) {

    /** Tương thích lời gọi chỉ có mã, tên, trọng số và trạng thái. */
    public PriorityCategoryRequest(Long id, String code, String name, Integer weight, Boolean active) {
        this(id, code, name, weight, active, null, null, null, null, null, null, null);
    }

    /** Tương thích lời gọi tạo mới trong mã Java; API dùng body có trường ID tùy chọn. */
    public PriorityCategoryRequest(String code, String name, Integer weight, Boolean active) {
        this(null, code, name, weight, active);
    }
}
