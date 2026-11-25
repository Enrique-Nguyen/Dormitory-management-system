package vn.edu.tlu.dms.registration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity ánh xạ bảng đã có trong Flyway. Tham chiếu chéo module lưu bằng ID, tránh phụ thuộc vòng
 * và tải quan hệ ngoài ý muốn.
 */
@Entity
@Table(name = "priority_category")
public class PriorityCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "weight", nullable = false)
    private Integer weight;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "evidence_required", length = 500)
    private String evidenceRequired;

    @Column(name = "evidence_mandatory", nullable=false)
    private Boolean evidenceMandatory = Boolean.FALSE;

    @Column(name = "legal_basis", length = 300)
    private String legalBasis;

    @Column(name = "sort_order")
    private Short sortOrder;

    @Column(name = "auto_rule", length = 30)
    private String autoRule;

    @Column(name = "note", length = 1000)
    private String note;

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String value) {
        this.code = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer value) {
        this.weight = value;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean value) {
        this.active = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    public String getEvidenceRequired() {
        return evidenceRequired;
    }

    public void setEvidenceRequired(String value) {
        this.evidenceRequired = value;
    }

    public Boolean getEvidenceMandatory() {
        return evidenceMandatory;
    }

    public void setEvidenceMandatory(Boolean value) {
        this.evidenceMandatory = value;
    }

    public String getLegalBasis() {
        return legalBasis;
    }

    public void setLegalBasis(String value) {
        this.legalBasis = value;
    }

    public Short getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Short value) {
        this.sortOrder = value;
    }

    public String getAutoRule() {
        return autoRule;
    }

    public void setAutoRule(String value) {
        this.autoRule = value;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String value) {
        this.note = value;
    }
}
