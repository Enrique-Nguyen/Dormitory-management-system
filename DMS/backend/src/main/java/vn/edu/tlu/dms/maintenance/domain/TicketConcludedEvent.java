package vn.edu.tlu.dms.maintenance.domain;

import java.util.List;

/** Sự kiện sau khi Staff kết luận ticket và (nếu có) trừ cọc. */
public record TicketConcludedEvent(
        Long ticketId, Long requesterId, String cause, List<Long> chargedStudentIds) {}
