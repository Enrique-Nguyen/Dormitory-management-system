package vn.edu.tlu.dms.common.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Điều kiện tìm kiếm dùng chung cho POST search-by-page. Trang bắt đầu từ 0 như DMS hiện tại; chỉ
 * các điều kiện do Service của từng màn hỗ trợ mới được dùng. Không nhận owner/mine/actor hiện tại
 * từ client: phạm vi truy cập luôn lấy từ danh tính đã xác thực.
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public final class SearchRequest {
    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(100)
    private int size = 20;

    @Size(max = 500)
    private String q;

    @Size(max = 10)
    private List<@NotBlank String> sort;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<@NotBlank String> status;

    private String role;
    @Positive private Long managementUnitId;
    private Boolean locked;
    @Positive private Long buildingId;
    private Short floor;
    @Positive private Long roomTypeId;
    @Min(0) private Short bedCount;
    @Min(0) private Long priceMain;
    @Min(0) private Long priceSummer;
    private String gender;
    private Boolean hasFreeBed;
    private Boolean noLeader;
    @Positive private Long semesterId;
    private String kind;
    private Boolean open;
    @Positive private Long periodId;
    private String type;
    private String category;
    private String severity;
    private String meterType;
    private LocalDate billingMonth;
    private Boolean unread;
    @Positive private Long schoolId;
    @Positive private Long facultyId;
    @Positive private Long cohortId;
    @Positive private Long hometownId;
    @Size(max = 30) private String code;
    private Boolean active;
    @Size(max = 9) private String academicYear;
    @Size(max = 30) private String scope;
    @Size(max = 100) private String key;
    private Boolean returned;
    @Positive private Long contractId;
    @Positive private Long studentId;
    private LocalDate startDateFrom;
    private LocalDate startDateTo;
    private LocalDate endDateFrom;
    private LocalDate endDateTo;
    @Positive private Long id;
    @Positive private Integer packageCount;
    @Size(max = 100) private String location;
    @Size(max = 500) private String description;
    private LocalDate depositedFrom;
    private LocalDate depositedTo;
    private LocalDate returnedFrom;
    private LocalDate returnedTo;
    private Boolean hasTopup;
    @Positive private Long actorId;
    private String action;
    private String entityType;
    private OffsetDateTime from;
    private OffsetDateTime to;
    @Size(max = 30) private String choice;
    @Size(max = 200) private String name;
    @Min(0) private Integer weight;
    @Min(0) private Long unitPrice;
    private LocalDate effectiveFromFrom;
    private LocalDate effectiveFromTo;
    @Min(0) private Long totalAmountMin;
    @Min(0) private Long totalAmountMax;
    private LocalDate dueDateFrom;
    private LocalDate dueDateTo;
    @Min(0) private Long depositAmount;
    private OffsetDateTime openAtFrom;
    private OffsetDateTime openAtTo;
    private OffsetDateTime closeAtFrom;
    private OffsetDateTime closeAtTo;

    public Long depositAmount() { return depositAmount; }
    public OffsetDateTime openAtFrom() { return openAtFrom; }
    public OffsetDateTime openAtTo() { return openAtTo; }
    public OffsetDateTime closeAtFrom() { return closeAtFrom; }
    public OffsetDateTime closeAtTo() { return closeAtTo; }

    @AssertTrue
    public boolean isOpenAtRangeValid() {
        return openAtFrom == null || openAtTo == null || !openAtFrom.isAfter(openAtTo);
    }

    @AssertTrue
    public boolean isCloseAtRangeValid() {
        return closeAtFrom == null || closeAtTo == null || !closeAtFrom.isAfter(closeAtTo);
    }

    public String choice() { return choice; }
    public String name() { return name; }
    public Integer weight() { return weight; }
    public Long unitPrice() { return unitPrice; }
    public LocalDate effectiveFromFrom() { return effectiveFromFrom; }
    public LocalDate effectiveFromTo() { return effectiveFromTo; }
    public Long totalAmountMin() { return totalAmountMin; }
    public Long totalAmountMax() { return totalAmountMax; }
    public LocalDate dueDateFrom() { return dueDateFrom; }
    public LocalDate dueDateTo() { return dueDateTo; }

    @AssertTrue
    public boolean isEffectiveFromRangeValid() {
        return effectiveFromFrom == null || effectiveFromTo == null || !effectiveFromFrom.isAfter(effectiveFromTo);
    }

    @AssertTrue
    public boolean isTotalAmountRangeValid() {
        return totalAmountMin == null || totalAmountMax == null || totalAmountMin <= totalAmountMax;
    }

    @AssertTrue
    public boolean isDueDateRangeValid() {
        return dueDateFrom == null || dueDateTo == null || !dueDateFrom.isAfter(dueDateTo);
    }

    public int page() {
        return page;
    }

    public int size() {
        return size;
    }

    public String q() {
        return q == null ? "" : q;
    }

    public List<String> sort() {
        return sort;
    }

    public String status() {
        return status == null || status.isEmpty() ? null : status.get(0);
    }

    public List<String> statuses() {
        return status;
    }

    public String role() {
        return role;
    }

    public Long managementUnitId() {
        return managementUnitId;
    }

    public Boolean locked() {
        return locked;
    }

    public Long buildingId() {
        return buildingId;
    }

    public Short floor() {
        return floor;
    }

    public Long roomTypeId() {
        return roomTypeId;
    }

    public Short bedCount() {
        return bedCount;
    }

    public Long priceMain() {
        return priceMain;
    }

    public Long priceSummer() {
        return priceSummer;
    }

    public String gender() {
        return gender;
    }

    public Boolean hasFreeBed() {
        return hasFreeBed;
    }

    public Boolean noLeader() {
        return noLeader;
    }

    public Long semesterId() {
        return semesterId;
    }

    public String kind() {
        return kind;
    }

    public Boolean open() {
        return open;
    }

    public Long periodId() {
        return periodId;
    }

    public String type() {
        return type;
    }

    public String category() {
        return category;
    }

    public String severity() {
        return severity;
    }

    public String meterType() {
        return meterType;
    }

    public LocalDate billingMonth() {
        return billingMonth;
    }

    public Boolean unread() {
        return unread;
    }

    public Long schoolId() { return schoolId; }

    public Long facultyId() { return facultyId; }

    public Long cohortId() { return cohortId; }

    public Long hometownId() { return hometownId; }

    public String code() { return code; }

    public Boolean active() { return active; }

    public String academicYear() { return academicYear; }

    public String scope() { return scope; }

    public String key() { return key; }

    public Boolean returned() { return returned; }

    public Long contractId() { return contractId; }

    public Long studentId() { return studentId; }

    public LocalDate startDateFrom() { return startDateFrom; }

    public LocalDate startDateTo() { return startDateTo; }

    public LocalDate endDateFrom() { return endDateFrom; }

    public LocalDate endDateTo() { return endDateTo; }

    @AssertTrue
    public boolean isStartDateRangeValid() {
        return startDateFrom == null || startDateTo == null || !startDateFrom.isAfter(startDateTo);
    }

    @AssertTrue
    public boolean isEndDateRangeValid() {
        return endDateFrom == null || endDateTo == null || !endDateFrom.isAfter(endDateTo);
    }

    public Long id() { return id; }

    public Integer packageCount() { return packageCount; }

    public String location() { return location; }

    public String description() { return description; }

    public LocalDate depositedFrom() { return depositedFrom; }

    public LocalDate depositedTo() { return depositedTo; }

    public LocalDate returnedFrom() { return returnedFrom; }

    public LocalDate returnedTo() { return returnedTo; }

    @AssertTrue
    public boolean isDepositedRangeValid() {
        return depositedFrom == null || depositedTo == null || !depositedFrom.isAfter(depositedTo);
    }

    @AssertTrue
    public boolean isReturnedRangeValid() {
        return returnedFrom == null || returnedTo == null || !returnedFrom.isAfter(returnedTo);
    }

    public Boolean hasTopup() { return hasTopup; }

    public Long actorId() {
        return actorId;
    }

    public String action() {
        return action;
    }

    public String entityType() {
        return entityType;
    }

    public OffsetDateTime from() {
        return from;
    }

    public OffsetDateTime to() {
        return to;
    }
}
