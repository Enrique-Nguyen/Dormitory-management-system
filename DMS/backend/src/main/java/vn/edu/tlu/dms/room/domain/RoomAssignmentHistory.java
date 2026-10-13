package vn.edu.tlu.dms.room.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Entity ánh xạ bảng đã có trong Flyway. Tham chiếu chéo module lưu bằng ID, tránh phụ thuộc vòng
 * và tải quan hệ ngoài ý muốn.
 */
@Entity
@Table(name = "room_assignment_history")
public class RoomAssignmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "bed_id", nullable = false)
    private Long bedId;

    @Column(name = "kind", nullable = false, length = 10)
    private String kind;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "stay_started_at")
    private OffsetDateTime stayStartedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "end_reason", length = 20)
    private String endReason;

    @Column(name = "price_main", nullable = false)
    private Long priceMain;

    @Column(name = "price_summer", nullable = false)
    private Long priceSummer;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long value) {
        this.studentId = value;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long value) {
        this.contractId = value;
    }

    public Long getBedId() {
        return bedId;
    }

    public void setBedId(Long value) {
        this.bedId = value;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String value) {
        this.kind = value;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime value) {
        this.startedAt = value;
    }

    public OffsetDateTime getStayStartedAt() {
        return stayStartedAt;
    }

    public void setStayStartedAt(OffsetDateTime value) {
        this.stayStartedAt = value;
    }

    public OffsetDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(OffsetDateTime value) {
        this.endedAt = value;
    }

    public String getEndReason() {
        return endReason;
    }

    public void setEndReason(String value) {
        this.endReason = value;
    }

    public Long getPriceMain() {
        return priceMain;
    }

    public void setPriceMain(Long value) {
        this.priceMain = value;
    }

    public Long getPriceSummer() {
        return priceSummer;
    }

    public void setPriceSummer(Long value) {
        this.priceSummer = value;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String value) {
        this.note = value;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long value) {
        this.createdBy = value;
    }
}
