package vn.edu.tlu.dms.common.support;

/** Dựng mẫu LIKE/ILIKE từ từ khóa của người dùng: ký tự đặc biệt được hiểu như chữ thường. */
public final class LikePattern {

    private LikePattern() {}

    /**
     * Mẫu "chứa từ khóa". Escape {@code \}, {@code %} và {@code _} bằng dấu {@code \} (ký tự
     * escape mặc định của PostgreSQL) để tìm "100%" không khớp mọi chuỗi.
     */
    public static String contains(String keyword) {
        String text = keyword == null ? "" : keyword.trim();
        return "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
