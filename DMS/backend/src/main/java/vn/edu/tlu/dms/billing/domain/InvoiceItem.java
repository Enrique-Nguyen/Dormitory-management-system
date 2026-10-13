package vn.edu.tlu.dms.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Entity ánh xạ bảng đã có trong Flyway. Tham chiếu chéo module lưu bằng ID, tránh phụ thuộc vòng
 * và tải quan hệ ngoài ý muốn.
 */
@Entity
@Table(name = "invoice_item")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "item_type", nullable = false, length = 15)
    private String itemType;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "assignment_id")
    private Long assignmentId;

    @Column(name = "utility_reading_id")
    private Long utilityReadingId;

    @Column(name = "adjusts_invoice_id")
    private Long adjustsInvoiceId;

    @Column(name = "note", length = 500)
    private String note;

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long value) {
        this.invoiceId = value;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String value) {
        this.itemType = value;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal value) {
        this.quantity = value;
    }

    public Long getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Long value) {
        this.unitPrice = value;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long value) {
        this.amount = value;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long value) {
        this.assignmentId = value;
    }

    public Long getUtilityReadingId() {
        return utilityReadingId;
    }

    public void setUtilityReadingId(Long value) {
        this.utilityReadingId = value;
    }

    public Long getAdjustsInvoiceId() {
        return adjustsInvoiceId;
    }

    public void setAdjustsInvoiceId(Long value) {
        this.adjustsInvoiceId = value;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String value) {
        this.note = value;
    }
}
