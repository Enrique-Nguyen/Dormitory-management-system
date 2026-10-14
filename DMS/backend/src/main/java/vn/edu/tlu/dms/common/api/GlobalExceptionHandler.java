package vn.edu.tlu.dms.common.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import vn.edu.tlu.dms.common.dto.ApiError;
import vn.edu.tlu.dms.common.exception.BusinessException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Map<String, String> CONSTRAINT_CODES =
            Map.ofEntries(
                    Map.entry("ux_assignment_open_bed", "BED_TAKEN"),
                    Map.entry("ux_assignment_open_student", "STUDENT_ALREADY_ASSIGNED"),
                    Map.entry("ux_contract_live_student", "CONTRACT_EXISTS"),
                    Map.entry("ux_contract_live_application", "CONTRACT_EXISTS"),
                    Map.entry("room_application_student_id_period_id_key", "APPLICATION_EXISTS"),
                    Map.entry("app_user_username_key", "USERNAME_EXISTS"),
                    Map.entry("room_building_id_code_key", "ROOM_CODE_EXISTS"),
                    Map.entry("semester_daterange_excl", "SEMESTER_OVERLAP"),
                    Map.entry("utility_rate_meter_type_effective_from_key", "RATE_EXISTS"),
                    Map.entry("ux_deposit_once", "DEPOSIT_INVALID"),
                    Map.entry("ux_refund_once", "DEPOSIT_INVALID"),
                    Map.entry("ux_deduction_ticket", "DEPOSIT_INVALID"),
                    Map.entry("ux_invoice_room_fee", "DUPLICATE_INVOICE"),
                    Map.entry("ux_invoice_utility", "DUPLICATE_INVOICE"),
                    Map.entry("ux_extension_pending", "REQUEST_PENDING_EXISTS"),
                    Map.entry("ux_transfer_pending", "REQUEST_PENDING_EXISTS"),
                    Map.entry("ux_storage_open", "STORAGE_RECORD_EXISTS"),
                    Map.entry("summer_choice_contract_id_semester_id_key", "SUMMER_CHOICE_EXISTS"),
                    Map.entry("ux_room_leader", "LEADER_ALREADY_ASSIGNED"),
                    Map.entry("student_profile_student_code_key", "STUDENT_CODE_EXISTS"),
                    Map.entry("bed_room_id_code_key", "BED_CODE_EXISTS"));

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> business(BusinessException e, HttpServletRequest request) {
        return response(e.getStatus(), e.getErrorCode(), e.getDetails(), request);
    }

    @ExceptionHandler(BindException.class)
    ResponseEntity<ApiError> validation(BindException e, HttpServletRequest request) {
        var fields =
                e.getBindingResult().getFieldErrors().stream()
                        .map(
                                f ->
                                        Map.of(
                                                "field",
                                                f.getField(),
                                                "message",
                                                String.valueOf(f.getDefaultMessage())))
                        .toList();
        return response(400, "VALIDATION_FAILED", Map.of("fieldErrors", fields), request);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        ConstraintViolationException.class
    })
    ResponseEntity<ApiError> malformed(Exception e, HttpServletRequest request) {
        return response(400, "VALIDATION_FAILED", Map.of(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> denied(AccessDeniedException e, HttpServletRequest request) {
        return response(403, "FORBIDDEN", Map.of(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> tooLarge(Exception e, HttpServletRequest request) {
        return response(413, "FILE_TOO_LARGE", Map.of("maxMb", 5), request);
    }

    /** Thiếu tham số/phần multipart hoặc sai kiểu validate tham số: lỗi của client, không phải 500. */
    @ExceptionHandler({
        ServletRequestBindingException.class,
        MissingServletRequestPartException.class,
        HandlerMethodValidationException.class
    })
    ResponseEntity<ApiError> badRequest(Exception e, HttpServletRequest request) {
        return response(400, "VALIDATION_FAILED", Map.of(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> methodNotAllowed(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return response(405, "METHOD_NOT_ALLOWED", Map.of(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> unsupportedMediaType(
            HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        return response(415, "UNSUPPORTED_MEDIA_TYPE", Map.of(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> noResource(NoResourceFoundException e, HttpServletRequest request) {
        return response(404, "NOT_FOUND", Map.of(), request);
    }

    /**
     * Ưu tiên tên ràng buộc đã khai báo; còn lại theo SQLState để lỗi xác định (trùng, khóa ngoại,
     * CHECK) không bị báo thành "dữ liệu vừa bị người khác thay đổi" khiến người dùng thử lại vô ích.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(
            DataIntegrityViolationException e, HttpServletRequest request) {
        String name = null;
        String sqlState = null;
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            // Hibernate không luôn nhận diện exclusion constraint; lấy metadata từ driver.
            if (cause instanceof PSQLException postgres) {
                sqlState = postgres.getSQLState();
                if (postgres.getServerErrorMessage() != null) {
                    name = postgres.getServerErrorMessage().getConstraint();
                }
            }
        }
        int status = 409;
        String code = name == null ? null : CONSTRAINT_CODES.get(name);
        if (code == null && name != null && name.startsWith("deposit_transaction")) {
            code = "DEPOSIT_INVALID";
        }
        if (code == null && name != null && name.startsWith("utility_reading")) {
            code = "READING_INVALID";
        }
        if (code == null) {
            code = "CONCURRENT_UPDATE";
            if (sqlState != null) {
                switch (sqlState) {
                    case "23505", "23P01" -> code = "DUPLICATE_VALUE";
                    case "23503" -> code = "RESOURCE_IN_USE";
                    case "23502", "23514" -> {
                        code = "VALIDATION_FAILED";
                        status = 400;
                    }
                    default -> {}
                }
            }
        }
        log.warn(
                "Constraint rejected request {}: {} (SQLState {})",
                request.getAttribute("requestId"),
                name,
                sqlState);
        return response(status, code, Map.of(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest request) {
        log.error("Unexpected request error {}", request.getAttribute("requestId"), e);
        return response(500, "INTERNAL_ERROR", Map.of(), request);
    }

    private ResponseEntity<ApiError> response(
            int status, String code, Map<String, Object> details, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(
                        new ApiError(
                                code, code, details, (String) request.getAttribute("requestId")));
    }
}
