package vn.edu.tlu.dms.notification.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import vn.edu.tlu.dms.notification.service.NotificationService;
import vn.edu.tlu.dms.registration.domain.RegistrationPeriodCreatedEvent;

/** Chỉ xử lý sự kiện sau khi nghiệp vụ đã commit; không tạo phụ thuộc ngược từ registration. */
@Component
public class RegistrationNotificationListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RegistrationNotificationListener.class);

    private final NotificationService service;

    public RegistrationNotificationListener(NotificationService service) {
        this.service = service;
    }

    /**
     * Nhận sự kiện tạo đợt đăng ký đã commit và gửi thông báo nếu đây là đợt hè. Lỗi tạo thông báo
     * được ghi log để không ảnh hưởng đến dữ liệu đợt đăng ký đã lưu.
     *
     * @param event thông tin đợt đăng ký vừa được tạo
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegistrationPeriodCreated(RegistrationPeriodCreatedEvent event) {
        if (!event.kind().equals("SUMMER")) {
            return;
        }
        try {
            service.notifyActiveStudentsOfSummerPeriod(
                    event.periodId(), event.name(), event.unitIds(), event.actorId());
        } catch (RuntimeException exception) {
            LOGGER.error("Không tạo được thông báo đợt hè {}", event.periodId(), exception);
        }
    }
}
