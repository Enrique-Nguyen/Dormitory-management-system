package vn.edu.tlu.dms.account.api;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** ID trống để tạo mới; có ID để sửa từng phần. Trường không gửi giữ nguyên khi sửa. */
public record ManagementUnitRequest(
        @Positive Long id, @Size(max = 20) String code, @Size(max = 100) String name) {

    /** Tương thích lời gọi tạo mới trong mã Java; API dùng body có trường ID tùy chọn. */
    public ManagementUnitRequest(String code, String name) {
        this(null, code, name);
    }
}
