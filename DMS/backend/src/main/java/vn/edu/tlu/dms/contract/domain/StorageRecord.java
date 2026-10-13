package vn.edu.tlu.dms.contract.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Entity ánh xạ bảng đã có trong Flyway. Tham chiếu chéo module lưu bằng ID, tránh phụ thuộc vòng
 * và tải quan hệ ngoài ý muốn.
 */
@Entity
@Table(name = "storage_record")
public class StorageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "summer_choice_id", nullable = false)
    private Long summerChoiceId;

    @Column(name = "previous_bed_id", nullable = false)
    private Long previousBedId;

    @Column(name = "package_count", nullable = false)
    private Short packageCount;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "location", nullable = false, length = 100)
    private String location;

    @Column(name = "deposited_at", nullable = false)
    private LocalDate depositedAt;

    @Column(name = "received_by", nullable = false)
    private Long receivedBy;

    @Column(name = "returned_at")
    private LocalDate returnedAt;

    @Column(name = "returned_by")
    private Long returnedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long value) {
        this.contractId = value;
    }

    public Long getSummerChoiceId() {
        return summerChoiceId;
    }

    public void setSummerChoiceId(Long value) {
        this.summerChoiceId = value;
    }

    public Long getPreviousBedId() {
        return previousBedId;
    }

    public void setPreviousBedId(Long value) {
        this.previousBedId = value;
    }

    public Short getPackageCount() {
        return packageCount;
    }

    public void setPackageCount(Short value) {
        this.packageCount = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String value) {
        this.location = value;
    }

    public LocalDate getDepositedAt() {
        return depositedAt;
    }

    public void setDepositedAt(LocalDate value) {
        this.depositedAt = value;
    }

    public Long getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(Long value) {
        this.receivedBy = value;
    }

    public LocalDate getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(LocalDate value) {
        this.returnedAt = value;
    }

    public Long getReturnedBy() {
        return returnedBy;
    }

    public void setReturnedBy(Long value) {
        this.returnedBy = value;
    }
}
