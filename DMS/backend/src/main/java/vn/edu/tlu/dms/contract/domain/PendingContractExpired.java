package vn.edu.tlu.dms.contract.domain;

/** Sự kiện trạng thái đồng bộ; listener lỗi phải rollback cả hủy hợp đồng và trả giường. */
public record PendingContractExpired(Long contractId, Long applicationId, String reason) {
    public record PreLockApplication(Long applicationId) {}
}
