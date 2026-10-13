package vn.edu.tlu.dms.common.exception;

import java.util.List;
import java.util.Map;

public class BusinessException extends RuntimeException {

    private final String errorCode;

    private final int status;

    private final Map<String, Object> details;

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

    public static BusinessException notFound() {
        return new BusinessException("NOT_FOUND", 404);
    }

    public static BusinessException invalid(String field, String message) {
        return new BusinessException(
                "VALIDATION_FAILED",
                400,
                Map.of("fieldErrors", List.of(Map.of("field", field, "message", message))));
    }

    public static BusinessException conflict(String code) {
        return new BusinessException(code, 409);
    }
}
