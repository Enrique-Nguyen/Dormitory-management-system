package vn.edu.tlu.dms.contract.domain;

/** Sự kiện sau khi tạo phiếu gửi đồ hoặc nhận lại đồ. */
public record StorageRecordChangedEvent(
        Long storageRecordId, Long contractId, Long studentId, String action) {}
