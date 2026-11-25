package vn.edu.tlu.dms.room.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity ánh xạ bảng đã có trong Flyway. Tham chiếu chéo module lưu bằng ID, tránh phụ thuộc vòng
 * và tải quan hệ ngoài ý muốn.
 */
@Entity
@Table(name = "room")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "building_id", nullable = false)
    private Long buildingId;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(name = "floor", nullable = false)
    private Short floor;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "gender", nullable = false, length = 1, columnDefinition = "char(1)")
    private String gender;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "leader_student_id")
    private Long leaderStudentId;

    @Column(name = "display_name", length = 50)
    private String displayName;

    @Column(name = "area_m2", precision = 6, scale = 2)
    private BigDecimal areaM2;

    @Column(name = "orientation", length = 2)
    private String orientation;

    @Column(name = "electric_meter_no", length = 30)
    private String electricMeterNo;

    @Column(name = "water_meter_no", length = 30)
    private String waterMeterNo;

    @Column(name = "commissioned_on")
    private LocalDate commissionedOn;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "note", length = 1000)
    private String note;

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long value) {
        this.buildingId = value;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long value) {
        this.roomTypeId = value;
    }

    public Short getFloor() {
        return floor;
    }

    public void setFloor(Short value) {
        this.floor = value;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String value) {
        this.code = value;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String value) {
        this.gender = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        this.status = value;
    }

    public Long getLeaderStudentId() {
        return leaderStudentId;
    }

    public void setLeaderStudentId(Long value) {
        this.leaderStudentId = value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String value) {
        this.displayName = value;
    }

    public BigDecimal getAreaM2() {
        return areaM2;
    }

    public void setAreaM2(BigDecimal value) {
        this.areaM2 = value;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String value) {
        this.orientation = value;
    }

    public String getElectricMeterNo() {
        return electricMeterNo;
    }

    public void setElectricMeterNo(String value) {
        this.electricMeterNo = value;
    }

    public String getWaterMeterNo() {
        return waterMeterNo;
    }

    public void setWaterMeterNo(String value) {
        this.waterMeterNo = value;
    }

    public LocalDate getCommissionedOn() {
        return commissionedOn;
    }

    public void setCommissionedOn(LocalDate value) {
        this.commissionedOn = value;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String value) {
        this.statusReason = value;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String value) {
        this.note = value;
    }
}
