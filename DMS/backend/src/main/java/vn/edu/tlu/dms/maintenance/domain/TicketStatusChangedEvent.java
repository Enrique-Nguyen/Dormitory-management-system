package vn.edu.tlu.dms.maintenance.domain;

/** Sự kiện sau khi ticket đổi trạng thái; notification lắng nghe sau commit. */
public record TicketStatusChangedEvent(
        Long ticketId, Long requesterId, Long assignedTo, String status) {}
