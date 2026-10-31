package vn.edu.tlu.dms.account.service;

import vn.edu.tlu.dms.account.api.AccountDtos.Preference;
import vn.edu.tlu.dms.account.api.AccountDtos.PreferenceInput;

/** Sở thích giao diện của chính tài khoản đang đăng nhập; không nhận chủ sở hữu từ client. */
public interface UserPreferenceService {

    /**
     * Đọc sở thích giao diện của người đăng nhập. Chưa có bản ghi thì trả mặc định light / blue /
     * gọn / version 0 và không tạo dòng mới.
     */
    Preference preferences();

    /**
     * Lưu sở thích của người đăng nhập theo kiểu tạo hoặc sửa: version null là lưu lần đầu, có
     * version là cập nhật và phải khớp bản đã đọc. Dữ liệu chỉ được ghi sau khi kiểm tra danh mục
     * mode/preset và khóa dòng hiện có.
     */
    Preference saveOrUpdate(PreferenceInput input);
}
