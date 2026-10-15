package vn.edu.tlu.dms.common.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.dto.ApiError;
import vn.edu.tlu.dms.common.exception.BusinessException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Map<String, String> CONSTRAINT_CODES =
            Map.ofEntries(
                    Map.entry("ux_assignment_open_bed", ErrorCode.BED_TAKEN),
                    Map.entry("ux_assignment_open_student", ErrorCode.STUDENT_ALREADY_ASSIGNED),
                    Map.entry("ux_contract_live_student", ErrorCode.CONTRACT_EXISTS),
                    Map.entry("ux_contract_live_application", ErrorCode.CONTRACT_EXISTS),
                    Map.entry(
                            "room_application_student_id_period_id_key",
                            ErrorCode.APPLICATION_EXISTS),
                    Map.entry("app_user_username_key", ErrorCode.USERNAME_EXISTS),
                    Map.entry("room_building_id_code_key", ErrorCode.ROOM_CODE_EXISTS),
                    Map.entry("semester_daterange_excl", ErrorCode.SEMESTER_OVERLAP),
                    Map.entry("utility_rate_meter_type_effective_from_key", ErrorCode.RATE_EXISTS),
                    Map.entry("ux_deposit_once", ErrorCode.DEPOSIT_INVALID),
                    Map.entry("ux_refund_once", ErrorCode.DEPOSIT_INVALID),
                    Map.entry("ux_deduction_ticket", ErrorCode.DEPOSIT_INVALID),
                    Map.entry("ux_invoice_room_fee", ErrorCode.DUPLICATE_INVOICE),
                    Map.entry("ux_invoice_utility", ErrorCode.DUPLICATE_INVOICE),
                    Map.entry("ux_extension_pending", ErrorCode.REQUEST_PENDING_EXISTS),
                    Map.entry("ux_transfer_pending", ErrorCode.REQUEST_PENDING_EXISTS),
                    Map.entry("ux_storage_open", ErrorCode.STORAGE_RECORD_EXISTS),
                    Map.entry(
                            "summer_choice_contract_id_semester_id_key",
                            ErrorCode.SUMMER_CHOICE_EXISTS));

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
                                                ApiField.FIELD,
                                                f.getField(),
                                                ApiField.MESSAGE,
                                                String.valueOf(f.getDefaultMessage())))
                        .toList();
        return response(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.VALIDATION_FAILED,
                Map.of(ApiField.FIELD_ERRORS, fields),
                request);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        ConstraintViolationException.class
    })
    ResponseEntity<ApiError> malformed(Exception e, HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST.value(), ErrorCode.VALIDATION_FAILED, Map.of(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> denied(AccessDeniedException e, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN.value(), ErrorCode.FORBIDDEN, Map.of(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> tooLarge(Exception e, HttpServletRequest request) {
        return response(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                ErrorCode.FILE_TOO_LARGE,
                Map.of("maxMb", 5),
                request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(
            DataIntegrityViolationException e, HttpServletRequest request) {
        String name = null;
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            // Hibernate không luôn nhận diện exclusion constraint; lấy metadata từ driver.
            if (cause instanceof PSQLException postgres
                    && postgres.getServerErrorMessage() != null) {
                name = postgres.getServerErrorMessage().getConstraint();
            }
        }
        String code =
                CONSTRAINT_CODES.getOrDefault(String.valueOf(name), ErrorCode.CONCURRENT_UPDATE);
        if (name != null && name.startsWith("deposit_transaction")) {
            code = ErrorCode.DEPOSIT_INVALID;
        }
        if (name != null && name.startsWith("utility_reading")) {
            code = ErrorCode.READING_INVALID;
        }
        log.warn("Constraint rejected request {}: {}", request.getAttribute("requestId"), name);
        return response(HttpStatus.CONFLICT.value(), code, Map.of(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest request) {
        log.error("Unexpected request error {}", request.getAttribute("requestId"), e);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorCode.INTERNAL_ERROR,
                Map.of(),
                request);
    }

    private ResponseEntity<ApiError> response(
            int status, String code, Map<String, Object> details, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(
                        new ApiError(
                                code, code, details, (String) request.getAttribute("requestId")));
    }
}
