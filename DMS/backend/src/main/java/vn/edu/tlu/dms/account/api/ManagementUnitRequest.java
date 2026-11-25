package vn.edu.tlu.dms.account.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants.Facility;

/**
 * ID trống để tạo mới; có ID để sửa từng phần. Trường không gửi hoặc null giữ nguyên khi sửa; chuỗi
 * rỗng xóa trường văn bản tùy chọn.
 */
public record ManagementUnitRequest(
        @Positive Long id,
        @Size(max = 20) String code,
        @Size(max = 100) String name,
        @Size(max = 30) String shortName,
        @Size(max = 300) String address,
        @Size(max = 15) @Pattern(regexp = Facility.PHONE_PATTERN) String hotline,
        @Size(max = 200) @Pattern(regexp = Facility.EMAIL_PATTERN) String email,
        @Positive Long headUserId,
        @Size(max = 100) String workingHours,
        Boolean active,
        @Size(max = 1000) String note) {

    /** Tương thích lời gọi tạo mới trong mã Java; API dùng body có trường ID tùy chọn. */
    public ManagementUnitRequest(String code, String name) {
        this(null, code, name, null, null, null, null, null, null, null, null);
    }
}
