package vn.edu.tlu.dms.common.dto;

import java.util.Map;

public record ApiError(
        String errorCode, String message, Map<String, Object> details, String requestId) {}
