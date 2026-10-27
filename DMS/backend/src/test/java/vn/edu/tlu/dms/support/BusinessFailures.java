package vn.edu.tlu.dms.support;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.exception.BusinessException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bộ trích thông điệp lỗi nghiệp vụ giúp assert trường nào bị từ chối và vì sao. */
public final class BusinessFailures {

    private BusinessFailures() {}

    /**
     * Bản đồ {@code field -> message} từ {@code details.fieldErrors} của lỗi validation. Lỗi không
     * phải validation thì trả map rỗng để test tự assert mã lỗi.
     */
    public static Map<String, String> fieldErrors(BusinessException error) {
        Map<String, String> byField = new LinkedHashMap<>();
        Object raw = error.getDetails().get(ApiField.FIELD_ERRORS);
        if (!(raw instanceof List<?> entries)) {
            return byField;
        }
        for (Object item : entries) {
            if (item instanceof Map<?, ?> entry) {
                byField.put(
                        String.valueOf(entry.get(ApiField.FIELD)),
                        String.valueOf(entry.get(ApiField.MESSAGE)));
            }
        }
        return byField;
    }
}
