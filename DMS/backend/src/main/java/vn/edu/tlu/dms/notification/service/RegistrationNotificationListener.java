package vn.edu.tlu.dms.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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
