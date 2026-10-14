package vn.edu.tlu.dms.common.security;

/** account cung cấp implementation; common không phụ thuộc module tầng cao hơn. */
public interface CurrentUser {

    /** Actor của request hiện tại; được nạp một lần mỗi request. */
    Actor get();

    /** Bỏ Actor đã nạp để lần {@link #get()} sau đọc lại cờ tài khoản (sau khi đổi cờ của chính mình). */
    void invalidate();
}
