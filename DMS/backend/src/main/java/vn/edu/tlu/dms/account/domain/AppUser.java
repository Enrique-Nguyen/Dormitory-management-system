package vn.edu.tlu.dms.account.domain;

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
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "management_unit_id")
    private Long managementUnitId;

    @Column(name = "cross_unit_access", nullable = false)
    private Boolean crossUnitAccess;

    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword;

    @Column(name = "locked", nullable = false)
    private Boolean locked;

    @Column(name = "failed_login_count", nullable = false)
    private Integer failedLoginCount;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "security_version", nullable = false)
    private Long securityVersion = 0L;

    @Column(name = "keycloak_user_id", length = 255)
    private String keycloakUserId;

    public String getKeycloakUserId() {
        return keycloakUserId;
    }

    public void setKeycloakUserId(String value) {
        this.keycloakUserId = value;
    }

    public Long getSecurityVersion() {
        return securityVersion;
    }

    public void setSecurityVersion(Long value) {
        this.securityVersion = value;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String value) {
        this.username = value;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String value) {
        this.passwordHash = value;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String value) {
        this.role = value;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String value) {
        this.fullName = value;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String value) {
        this.phone = value;
    }

    public Long getManagementUnitId() {
        return managementUnitId;
    }

    public void setManagementUnitId(Long value) {
        this.managementUnitId = value;
    }

    public Boolean getCrossUnitAccess() {
        return crossUnitAccess;
    }

    public void setCrossUnitAccess(Boolean value) {
        this.crossUnitAccess = value;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean value) {
        this.mustChangePassword = value;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean value) {
        this.locked = value;
    }

    public Integer getFailedLoginCount() {
        return failedLoginCount;
    }

    public void setFailedLoginCount(Integer value) {
        this.failedLoginCount = value;
    }

    public OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(OffsetDateTime value) {
        this.lockedUntil = value;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime value) {
        this.createdAt = value;
    }
}
