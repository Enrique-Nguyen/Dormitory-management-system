package vn.edu.tlu.dms.contract.domain;

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
@Table(name = "move_out_record")
public class MoveOutRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "moved_out_at", nullable = false)
    private OffsetDateTime movedOutAt;

    @Column(name = "asset_condition", nullable = false, length = 10)
    private String assetCondition;

    @Column(name = "asset_note", length = 500)
    private String assetNote;

    @Column(name = "unpaid_room_fee", nullable = false)
    private Long unpaidRoomFee;

    @Column(name = "deposit_balance", nullable = false)
    private Long depositBalance;

    @Column(name = "refund_amount", nullable = false)
    private Long refundAmount;

    @Column(name = "confirmed_by", nullable = false)
    private Long confirmedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long value) {
        this.contractId = value;
    }

    public OffsetDateTime getMovedOutAt() {
        return movedOutAt;
    }

    public void setMovedOutAt(OffsetDateTime value) {
        this.movedOutAt = value;
    }

    public String getAssetCondition() {
        return assetCondition;
    }

    public void setAssetCondition(String value) {
        this.assetCondition = value;
    }

    public String getAssetNote() {
        return assetNote;
    }

    public void setAssetNote(String value) {
        this.assetNote = value;
    }

    public Long getUnpaidRoomFee() {
        return unpaidRoomFee;
    }

    public void setUnpaidRoomFee(Long value) {
        this.unpaidRoomFee = value;
    }

    public Long getDepositBalance() {
        return depositBalance;
    }

    public void setDepositBalance(Long value) {
        this.depositBalance = value;
    }

    public Long getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(Long value) {
        this.refundAmount = value;
    }

    public Long getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(Long value) {
        this.confirmedBy = value;
    }
}
