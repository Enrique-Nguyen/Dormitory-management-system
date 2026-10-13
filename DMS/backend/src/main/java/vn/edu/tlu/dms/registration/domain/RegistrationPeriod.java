package vn.edu.tlu.dms.registration.domain;

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
@Table(name = "registration_period")
public class RegistrationPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "semester_id", nullable = false)
    private Long semesterId;

    @Column(name = "kind", nullable = false, length = 10)
    private String kind;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "open_at", nullable = false)
    private OffsetDateTime openAt;

    @Column(name = "close_at", nullable = false)
    private OffsetDateTime closeAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Long value) {
        this.semesterId = value;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String value) {
        this.kind = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public OffsetDateTime getOpenAt() {
        return openAt;
    }

    public void setOpenAt(OffsetDateTime value) {
        this.openAt = value;
    }

    public OffsetDateTime getCloseAt() {
        return closeAt;
    }

    public void setCloseAt(OffsetDateTime value) {
        this.closeAt = value;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long value) {
        this.createdBy = value;
    }
}
