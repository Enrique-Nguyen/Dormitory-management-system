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

    @Column(name = "faculty", length = 100)
    private String faculty;

    @Column(name = "cohort", length = 10)
    private String cohort;

    @Column(name = "hometown", length = 200)
    private String hometown;

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

    public String getFaculty() {
        return faculty;
    }

    public void setFaculty(String value) {
        this.faculty = value;
    }

    public String getCohort() {
        return cohort;
    }

    public void setCohort(String value) {
        this.cohort = value;
    }

    public String getHometown() {
        return hometown;
    }

    public void setHometown(String value) {
        this.hometown = value;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String value) {
        this.emergencyContact = value;
    }
}
