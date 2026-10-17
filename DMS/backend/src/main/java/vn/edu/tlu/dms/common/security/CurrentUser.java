package vn.edu.tlu.dms.common.security;

/** account cung cấp implementation; common không phụ thuộc module tầng cao hơn. */
public interface CurrentUser {

    /**
     * Trả Actor của request hiện tại. Role, trạng thái khóa và security_version được đối chiếu
     * với dữ liệu DMS mới nhất một lần khi gọi đầu tiên trong request, rồi dùng lại như ảnh chụp
     * xác thực cho các lần gọi sau trong cùng request. Nhờ vậy thao tác tự thu hồi phiên hay đổi
     * quyền của chính người gọi vẫn hoàn tất, còn request kế tiếp được kiểm tra lại từ đầu; quyền
     * nghiệp vụ luôn được đọc mới từ ma trận quyền, không lấy từ Actor.
     *
     * @return Actor đã xác thực của request hiện tại
     */
    Actor get();
}
