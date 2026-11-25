package vn.edu.tlu.dms.room.domain;

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
@Table(name = "semester")
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "academic_year", nullable = false, length = 9)
    private String academicYear;

    @Column(name = "type", nullable = false, length = 10)
    private String type;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Mã sinh bởi CSDL từ loại + năm đầu (cột GENERATED, V18); chỉ đọc. */
    @Column(name = "code", insertable = false, updatable = false)
    private String code;

    @Column(name = "room_fee_due_date")
    private LocalDate roomFeeDueDate;

    @Column(name = "note", length = 1000)
    private String note;

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String value) {
        this.academicYear = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String value) {
        this.type = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate value) {
        this.startDate = value;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate value) {
        this.endDate = value;
    }

    public String getCode() {
        return code;
    }


    public LocalDate getRoomFeeDueDate() {
        return roomFeeDueDate;
    }

    public void setRoomFeeDueDate(LocalDate value) {
        this.roomFeeDueDate = value;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String value) {
        this.note = value;
    }
}
