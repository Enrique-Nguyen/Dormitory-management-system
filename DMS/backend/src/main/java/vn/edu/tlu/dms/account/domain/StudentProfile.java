package vn.edu.tlu.dms.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/**
 * Entity ánh xạ bảng đã có trong Flyway. Tham chiếu chéo module lưu bằng ID, tránh phụ thuộc vòng
 * và tải quan hệ ngoài ý muốn.
 */
@Entity
@Table(name = "student_profile")
public class StudentProfile {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "student_code", nullable = false, length = 20)
    private String studentCode;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "gender", nullable = false, length = 1, columnDefinition = "char(1)")
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "faculty_id")
    private Long facultyId;

    @Column(name = "cohort_id")
    private Long cohortId;

    @Column(name = "hometown_id")
    private Long hometownId;

    @Column(name = "emergency_contact", length = 200)
    private String emergencyContact;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long value) {
        this.userId = value;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String value) {
        this.studentCode = value;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String value) {
        this.gender = value;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate value) {
        this.dateOfBirth = value;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Long value) {
        this.schoolId = value;
    }

    public Long getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(Long value) {
        this.facultyId = value;
    }

    public Long getCohortId() {
        return cohortId;
    }

    public void setCohortId(Long value) {
        this.cohortId = value;
    }

    public Long getHometownId() {
        return hometownId;
    }

    public void setHometownId(Long value) {
        this.hometownId = value;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String value) {
        this.emergencyContact = value;
    }
}
