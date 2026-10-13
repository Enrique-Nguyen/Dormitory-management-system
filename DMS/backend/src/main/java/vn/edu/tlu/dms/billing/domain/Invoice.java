package vn.edu.tlu.dms.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Entity ánh xạ bảng đã có trong Flyway. Tham chiếu chéo module lưu bằng ID, tránh phụ thuộc vòng
 * và tải quan hệ ngoài ý muốn.
 */
@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "type", nullable = false, length = 10)
    private String type;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "semester_id")
    private Long semesterId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "billing_month")
    private LocalDate billingMonth;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "payer_user_id")
    private Long payerUserId;

    @Column(name = "payer_name", length = 100)
    private String payerName;

    @Column(name = "payment_method", length = 10)
    private String paymentMethod;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @Column(name = "issued_by", nullable = false)
    private Long issuedBy;

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

    public String getType() {
        return type;
    }

    public void setType(String value) {
        this.type = value;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long value) {
        this.studentId = value;
    }

    public Long getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Long value) {
        this.semesterId = value;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long value) {
        this.roomId = value;
    }

    public LocalDate getBillingMonth() {
        return billingMonth;
    }

    public void setBillingMonth(LocalDate value) {
        this.billingMonth = value;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long value) {
        this.totalAmount = value;
    }

    public OffsetDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(OffsetDateTime value) {
        this.issuedAt = value;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate value) {
        this.dueDate = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        this.status = value;
    }

    public Long getPayerUserId() {
        return payerUserId;
    }

    public void setPayerUserId(Long value) {
        this.payerUserId = value;
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String value) {
        this.payerName = value;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String value) {
        this.paymentMethod = value;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(OffsetDateTime value) {
        this.paidAt = value;
    }

    public Long getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(Long value) {
        this.confirmedBy = value;
    }

    public Long getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(Long value) {
        this.issuedBy = value;
    }
}
