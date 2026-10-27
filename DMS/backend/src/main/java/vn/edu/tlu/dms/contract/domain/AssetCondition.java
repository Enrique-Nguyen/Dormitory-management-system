package vn.edu.tlu.dms.contract.domain;

/** Tình trạng tài sản ghi trong biên bản trả phòng; giá trị khớp CHECK asset_condition của CSDL. */
public enum AssetCondition {
    OK,
    DAMAGED;

    /** Trả enum tương ứng hoặc null khi mã không hợp lệ; nơi gọi tự phát lỗi validation. */
    public static AssetCondition from(String value) {
        for (AssetCondition condition : values()) {
            if (condition.name().equals(value)) {
                return condition;
            }
        }
        return null;
    }
}
