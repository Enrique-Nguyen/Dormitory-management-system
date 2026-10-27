package vn.edu.tlu.dms.contract.domain;

/** Sự kiện sau khi hợp đồng được trả phòng; notification lắng nghe sau commit. */
public record ContractMovedOutEvent(Long contractId, Long studentId, String endStatus) {}
