package vn.edu.tlu.dms.notification.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import vn.edu.tlu.dms.common.constants.DmsConstants.MaintenanceStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.NotificationSource;
import vn.edu.tlu.dms.maintenance.domain.TicketConcludedEvent;
import vn.edu.tlu.dms.maintenance.domain.TicketStatusChangedEvent;
import vn.edu.tlu.dms.notification.service.NotificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Gửi thông báo hộp thư sau khi maintenance đã commit thay đổi ticket. Listener chỉ gọi Service
 * công khai của notification và nuốt lỗi gửi tin để không ảnh hưởng nghiệp vụ đã lưu.
 */
@Component
public class MaintenanceNotificationListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MaintenanceNotificationListener.class);

    private final NotificationService service;

    public MaintenanceNotificationListener(NotificationService service) {
        this.service = service;
    }

    /**
     * Thông báo cho người tạo phiếu và kỹ thuật viên được phân công (nếu khác người tạo) khi ticket
     * đổi trạng thái. Mỗi người chỉ nhận một bản ghi cho một trạng thái.
     *
     * @param event thông tin ticket và trạng thái mới sau khi nghiệp vụ đã commit
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketStatusChanged(TicketStatusChangedEvent event) {
        if (event == null || event.ticketId() == null) {
            return;
        }
        try {
            List<Long> recipients = new ArrayList<>(2);
            addRecipient(recipients, event.requesterId());
            addRecipient(recipients, event.assignedTo());
            if (recipients.isEmpty()) {
                return;
            }
            String statusText = statusText(event.status());
            String title = "Phiếu sửa chữa #" + event.ticketId() + " " + statusText;
            String body =
                    "Phiếu sửa chữa #"
                            + event.ticketId()
                            + " "
                            + statusText
                            + ". Vui lòng mở mục Sửa chữa để xem chi tiết.";
            service.notifyUsers(
                    recipients,
                    title,
                    body,
                    NotificationSource.TICKET_STATUS_CHANGED,
                    event.ticketId());
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Không tạo được thông báo đổi trạng thái ticket {}",
                    event.ticketId(),
                    exception);
        }
    }

    /**
     * Thông báo kết luận ticket cho người tạo phiếu; nếu có sinh viên bị trừ cọc thì nêu rõ chi phí
     * sẽ trừ vào cọc để người nhận biết trước khi xem chi tiết. Sinh viên bị trừ cọc nhưng không
     * phải người tạo phiếu nhận thêm một thông báo trừ cọc riêng.
     *
     * @param event kết luận ticket sau khi nghiệp vụ và trừ cọc đã commit
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketConcluded(TicketConcludedEvent event) {
        if (event == null || event.ticketId() == null || event.requesterId() == null) {
            return;
        }
        try {
            StringBuilder body =
                    new StringBuilder("Phiếu sửa chữa #")
                            .append(event.ticketId())
                            .append(" đã được kết luận. Vui lòng mở mục Sửa chữa để xem chi tiết.");
            if (event.chargedStudentIds() != null && !event.chargedStudentIds().isEmpty()) {
                body.append(
                        event.chargedStudentIds().contains(event.requesterId())
                                ? " Chi phí sửa chữa sẽ được trừ vào cọc của bạn."
                                : " Chi phí sửa chữa sẽ được trừ vào cọc của sinh viên chịu trách"
                                        + " nhiệm.");
            }
            service.notifyUsers(
                    List.of(event.requesterId()),
                    "Kết luận phiếu sửa chữa #" + event.ticketId(),
                    body.toString(),
                    NotificationSource.TICKET_STATUS_CHANGED,
                    event.ticketId());
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Không tạo được thông báo kết luận ticket {}", event.ticketId(), exception);
        }
        notifyChargedStudents(event);
    }

    /**
     * Sinh viên bị trừ cọc nhận thông báo riêng để biết khoản trừ và mở sổ cọc. Người tạo phiếu đã
     * nhận thông báo kết luận nêu việc trừ cọc nên được loại khỏi danh sách để không trùng nội
     * dung. Lỗi ở bước này chỉ ghi log, không ảnh hưởng thông báo kết luận đã gửi trước đó.
     *
     * @param event kết luận ticket kèm danh sách sinh viên bị trừ cọc
     */
    private void notifyChargedStudents(TicketConcludedEvent event) {
        if (event.chargedStudentIds() == null || event.chargedStudentIds().isEmpty()) {
            return;
        }
        List<Long> chargedStudents =
                event.chargedStudentIds().stream()
                        .filter(Objects::nonNull)
                        .filter(userId -> !userId.equals(event.requesterId()))
                        .distinct()
                        .toList();
        if (chargedStudents.isEmpty()) {
            return;
        }
        try {
            service.notifyUsers(
                    chargedStudents,
                    "Trừ cọc do sửa chữa phiếu #" + event.ticketId(),
                    "Chi phí sửa chữa của phiếu #"
                            + event.ticketId()
                            + " được trừ vào cọc của bạn. Vui lòng mở mục Sổ cọc để xem chi tiết.",
                    NotificationSource.DEPOSIT_DEDUCTED,
                    event.ticketId());
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Không tạo được thông báo trừ cọc cho ticket {}",
                    event.ticketId(),
                    exception);
        }
    }

    private static void addRecipient(List<Long> recipients, Long userId) {
        if (userId != null && !recipients.contains(userId)) {
            recipients.add(userId);
        }
    }

    /** Nhãn tiếng Việt theo mã trạng thái ticket; mã lạ vẫn được hiển thị để không mất tin. */
    private static String statusText(String status) {
        if (status == null) {
            return "vừa được cập nhật";
        }
        return switch (status) {
            case MaintenanceStatus.OPEN -> "đã được tạo";
            case MaintenanceStatus.ASSIGNED -> "đã được phân công cho kỹ thuật viên";
            case MaintenanceStatus.IN_PROGRESS -> "đang được kỹ thuật viên xử lý";
            case MaintenanceStatus.RESOLVED -> "đã sửa xong và đang chờ nghiệm thu";
            case MaintenanceStatus.CLOSED -> "đã được đóng";
            case MaintenanceStatus.REJECTED -> "đã bị từ chối";
            case MaintenanceStatus.CANCELLED -> "đã bị hủy";
            default -> "đã được cập nhật trạng thái " + status;
        };
    }
}
