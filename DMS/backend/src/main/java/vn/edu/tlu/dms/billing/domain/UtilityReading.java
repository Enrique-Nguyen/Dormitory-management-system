package vn.edu.tlu.dms.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Entity ánh xạ bảng đã có trong Flyway. Tham chiếu chéo module lưu bằng ID, tránh phụ thuộc vòng
 * và tải quan hệ ngoài ý muốn.
 */
@Entity
@Table(name = "utility_reading")
public class UtilityReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "meter_type", nullable = false, length = 10)
    private String meterType;

    @Column(name = "billing_month", nullable = false)
    private LocalDate billingMonth;

    @Column(name = "start_reading", nullable = false)
    private Integer startReading;

    @Column(name = "end_reading", nullable = false)
    private Integer endReading;

    @Column(name = "old_meter_final")
    private Integer oldMeterFinal;

    @Column(name = "new_meter_start")
    private Integer newMeterStart;

    @Column(name = "replacement_reason", length = 300)
    private String replacementReason;

    @Column(name = "consumption", insertable = false, updatable = false)
    private Integer consumption;

    @Column(name = "recorded_by", nullable = false)
    private Long recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long value) {
        this.roomId = value;
    }

    public String getMeterType() {
        return meterType;
    }

    public void setMeterType(String value) {
        this.meterType = value;
    }

    public LocalDate getBillingMonth() {
        return billingMonth;
    }

    public void setBillingMonth(LocalDate value) {
        this.billingMonth = value;
    }

    public Integer getStartReading() {
        return startReading;
    }

    public void setStartReading(Integer value) {
        this.startReading = value;
    }

    public Integer getEndReading() {
        return endReading;
    }

    public void setEndReading(Integer value) {
        this.endReading = value;
    }

    public Integer getOldMeterFinal() {
        return oldMeterFinal;
    }

    public void setOldMeterFinal(Integer value) {
        this.oldMeterFinal = value;
    }

    public Integer getNewMeterStart() {
        return newMeterStart;
    }

    public void setNewMeterStart(Integer value) {
        this.newMeterStart = value;
    }

    public String getReplacementReason() {
        return replacementReason;
    }

    public void setReplacementReason(String value) {
        this.replacementReason = value;
    }

    public Integer getConsumption() {
        return consumption;
    }

    public Long getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(Long value) {
        this.recordedBy = value;
    }

    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(OffsetDateTime value) {
        this.recordedAt = value;
    }
}
