package vn.edu.tlu.dms.common.exception;

import org.springframework.http.HttpStatus;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;

import java.util.List;
import java.util.Map;

public class BusinessException extends RuntimeException {

    private final String errorCode;

    private final int status;

    private final Map<String, Object> details;

    /** Dùng HttpStatus có tên rõ nghĩa; mã trạng thái trả về API giữ nguyên kiểu số. */
    public BusinessException(String code, HttpStatus status, Map<String, Object> details) {
        this(code, status.value(), details);
    }

    public BusinessException(String code, HttpStatus status) {
        this(code, status, Map.of());
    }

    public BusinessException(String code, int status, Map<String, Object> details) {
        super(code);
        this.errorCode = code;
        this.status = status;
        this.details = Map.copyOf(details);
    }

    public BusinessException(String code, int status) {
        this(code, status, Map.of());
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getStatus() {
        return status;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    /** Từ chối thao tác khi tài khoản không có quyền được yêu cầu. */
    public static BusinessException forbidden() {
        return new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
    }

    public static BusinessException notFound() {
        return new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    public static BusinessException invalid(String field, String message) {
        return new BusinessException(
                ErrorCode.VALIDATION_FAILED,
                HttpStatus.BAD_REQUEST,
                Map.of(
                        ApiField.FIELD_ERRORS,
                        List.of(Map.of(ApiField.FIELD, field, ApiField.MESSAGE, message))));
    }

    public static BusinessException conflict(String code) {
        return new BusinessException(code, HttpStatus.CONFLICT);
    }
}
