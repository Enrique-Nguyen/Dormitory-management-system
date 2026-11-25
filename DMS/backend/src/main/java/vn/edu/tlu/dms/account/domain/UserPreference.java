package vn.edu.tlu.dms.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;

/**
 * Sở thích giao diện của một tài khoản. Khóa chính là khóa ngoại tới {@code app_user} nên quan hệ
 * 1-1 do CSDL bảo đảm; entity không ánh xạ quan hệ để tránh tải tài khoản ngoài ý muốn.
 *
 * <p>Khóa chính do client gán nên implement {@link Persistable} để Spring Data dùng
 * {@code persist} cho bản ghi mới; nhờ vậy hai tab cùng lưu lần đầu sẽ để CSDL chặn một bên bằng
 * khóa chính thay vì {@code merge} ghi đè âm thầm.
 */
@Entity
@Table(name = "user_preference")
public class UserPreference implements Persistable<Long> {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Transient private boolean newRecord = true;

    @Column(name = "theme_mode", nullable = false, length = 10)
    private String themeMode;

    @Column(name = "theme_preset", nullable = false, length = 20)
    private String themePreset;

    @Column(name = "table_dense", nullable = false)
    private Boolean tableDense;

    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getUserId() {
        return userId;
    }

    @Override
    public Long getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return newRecord;
    }

    @PostLoad
    @PostPersist
    void markPersisted() {
        newRecord = false;
    }

    public void setUserId(Long value) {
        this.userId = value;
    }

    public String getThemeMode() {
        return themeMode;
    }

    public void setThemeMode(String value) {
        this.themeMode = value;
    }

    public String getThemePreset() {
        return themePreset;
    }

    public void setThemePreset(String value) {
        this.themePreset = value;
    }

    public Boolean getTableDense() {
        return tableDense;
    }

    public void setTableDense(Boolean value) {
        this.tableDense = value;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long value) {
        this.version = value;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime value) {
        this.updatedAt = value;
    }
}
