package vn.edu.tlu.dms.room.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
}
