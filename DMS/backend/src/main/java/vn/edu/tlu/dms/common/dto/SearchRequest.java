package vn.edu.tlu.dms.common.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;

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
    @Positive private Long actorId;
    private String action;
    private String entityType;
    private OffsetDateTime from;
    private OffsetDateTime to;

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
