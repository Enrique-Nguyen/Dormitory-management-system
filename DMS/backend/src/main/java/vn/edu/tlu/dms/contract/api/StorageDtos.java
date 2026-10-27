package vn.edu.tlu.dms.contract.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;

import java.time.LocalDate;

/** DTO tạo phiếu gửi đồ và nhận lại đồ của kỳ hè (API 4.9). */
public final class StorageDtos {

    private StorageDtos() {}

    /** Thân tạo phiếu gửi đồ; giường cũ do server đọc từ phân phòng đang mở. */
    public record CreateRequest(
            @NotNull(message = ValidationMessage.PACKAGE_COUNT_POSITIVE)
                    @Positive(message = ValidationMessage.PACKAGE_COUNT_POSITIVE)
                    Short packageCount,
            @NotBlank(message = ValidationMessage.STORAGE_DESCRIPTION_REQUIRED)
                    @Size(max = 500)
                    String description,
            @NotBlank(message = ValidationMessage.STORAGE_LOCATION_REQUIRED)
                    @Size(max = 100)
                    String location,
            @NotNull(message = ValidationMessage.DEPOSITED_DATE_REQUIRED) LocalDate depositedAt) {}

    /** Thân nhận lại đồ: giường nhận và ngày trả đồ bắt buộc. */
    public record ReturnRequest(
            @NotNull(message = ValidationMessage.BED_REQUIRED)
                    @Positive(message = ValidationMessage.BED_REQUIRED)
                    Long bedId,
            @NotNull(message = ValidationMessage.RETURNED_DATE_REQUIRED) LocalDate returnedAt) {}

    /** Phiếu gửi đồ sau khi tạo hoặc nhận lại; chỉ chứa ID và dữ liệu cần hiển thị. */
    public record Record(
            Long id,
            Long contractId,
            Long studentId,
            Long summerChoiceId,
            Long previousBedId,
            Short packageCount,
            String description,
            String location,
            LocalDate depositedAt,
            Long receivedBy,
            LocalDate returnedAt,
            Long returnedBy) {}
}
