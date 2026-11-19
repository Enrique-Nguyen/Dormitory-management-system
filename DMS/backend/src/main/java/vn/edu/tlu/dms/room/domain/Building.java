package vn.edu.tlu.dms.room.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Entity ánh xạ bảng đã có trong Flyway. Tham chiếu chéo module lưu bằng ID, tránh phụ thuộc vòng
 * và tải quan hệ ngoài ý muốn.
 */
@Entity
@Table(name = "building")
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "management_unit_id", nullable = false)
    private Long managementUnitId;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "deposit_amount", nullable = false)
    private Long depositAmount;

    @Column(name = "floor_count", nullable = false)
    private Short floorCount;

    @Column(name = "has_ground_floor", nullable = false)
    private Boolean hasGroundFloor = Boolean.FALSE;

    @Column(name = "gender_policy", length = 10)
    private String genderPolicy;

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "commissioned_year")
    private Short commissionedYear;

    @Column(name = "floor_area_m2", precision = 10, scale = 2)
    private BigDecimal floorAreaM2;

    @Column(name = "manager_user_id")
    private Long managerUserId;

    @Column(name = "hotline", length = 15)
    private String hotline;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "note", length = 1000)
    private String note;

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getManagementUnitId() {
        return managementUnitId;
    }

    public void setManagementUnitId(Long value) {
        this.managementUnitId = value;
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

    public Long getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(Long value) {
        this.depositAmount = value;
    }

    public Short getFloorCount() {
        return floorCount;
    }

    public void setFloorCount(Short value) {
        this.floorCount = value;
    }

    public Boolean getHasGroundFloor() {
        return hasGroundFloor;
    }

    public void setHasGroundFloor(Boolean value) {
        this.hasGroundFloor = value;
    }

    public String getGenderPolicy() {
        return genderPolicy;
    }

    public void setGenderPolicy(String value) {
        this.genderPolicy = value;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String value) {
        this.address = value;
    }

    public Short getCommissionedYear() {
        return commissionedYear;
    }

    public void setCommissionedYear(Short value) {
        this.commissionedYear = value;
    }

    public BigDecimal getFloorAreaM2() {
        return floorAreaM2;
    }

    public void setFloorAreaM2(BigDecimal value) {
        this.floorAreaM2 = value;
    }

    public Long getManagerUserId() {
        return managerUserId;
    }

    public void setManagerUserId(Long value) {
        this.managerUserId = value;
    }

    public String getHotline() {
        return hotline;
    }

    public void setHotline(String value) {
        this.hotline = value;
    }

    public LocalTime getOpenTime() {
        return openTime;
    }

    public void setOpenTime(LocalTime value) {
        this.openTime = value;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalTime value) {
        this.closeTime = value;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String value) {
        this.note = value;
    }
}
