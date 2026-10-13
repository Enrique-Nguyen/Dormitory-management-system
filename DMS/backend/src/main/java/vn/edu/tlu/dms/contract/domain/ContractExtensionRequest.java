package vn.edu.tlu.dms.contract.domain;

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
@Table(name = "contract_extension_request")
public class ContractExtensionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Column(name = "old_end_date", nullable = false)
    private LocalDate oldEndDate;

    @Column(name = "new_end_date", nullable = false)
    private LocalDate newEndDate;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "decided_by")
    private Long decidedBy;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "decision_reason", length = 500)
    private String decisionReason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

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

    public String getSource() {
        return source;
    }

    public void setSource(String value) {
        this.source = value;
    }

    public LocalDate getOldEndDate() {
        return oldEndDate;
    }

    public void setOldEndDate(LocalDate value) {
        this.oldEndDate = value;
    }

    public LocalDate getNewEndDate() {
        return newEndDate;
    }

    public void setNewEndDate(LocalDate value) {
        this.newEndDate = value;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String value) {
        this.reason = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        this.status = value;
    }

    public Long getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(Long value) {
        this.decidedBy = value;
    }

    public OffsetDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(OffsetDateTime value) {
        this.decidedAt = value;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String value) {
        this.decisionReason = value;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime value) {
        this.createdAt = value;
    }
}
