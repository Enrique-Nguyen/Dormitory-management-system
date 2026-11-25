package vn.edu.tlu.dms.notification.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApplicationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.NotificationSource;
import vn.edu.tlu.dms.common.constants.DmsConstants.RegistrationKind;
import vn.edu.tlu.dms.notification.service.NotificationService;
import vn.edu.tlu.dms.registration.domain.ApplicationStatusChangedEvent;
import vn.edu.tlu.dms.registration.domain.RegistrationPeriodCreatedEvent;

import java.util.List;

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
        if (!event.kind().equals(RegistrationKind.SUMMER)) {
            return;
        }
        try {
            service.notifyActiveStudentsOfSummerPeriod(
                    event.periodId(), event.name(), event.unitIds(), event.actorId());
        } catch (RuntimeException exception) {
            LOGGER.error("Không tạo được thông báo đợt hè {}", event.periodId(), exception);
        }
    }

    /**
     * Thông báo cho sinh viên khi đơn đăng ký được duyệt, bị từ chối hoặc được giữ chỗ (FR-NOT-04).
     * Tiêu đề giữ chỗ nêu mã giường để lần phân lại sau khi hủy giữ chỗ vẫn tạo thông báo mới, không
     * bị chống trùng theo nguồn. Lỗi chỉ ghi log vì nghiệp vụ đơn đã commit.
     *
     * @param event đơn vừa đổi trạng thái sau khi nghiệp vụ đã commit
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationStatusChanged(ApplicationStatusChangedEvent event) {
        if (event == null || event.applicationId() == null || event.studentId() == null) {
            return;
        }
        String source;
        String title;
        String body;
        switch (event.status()) {
            case ApplicationStatus.APPROVED -> {
                source = NotificationSource.APPLICATION_APPROVED;
                title = "Đơn đăng ký #" + event.applicationId() + " đã được duyệt";
                body =
                        "Đơn đăng ký ở KTX #"
                                + event.applicationId()
                                + " đã được duyệt. Cán bộ KTX sẽ phân giường giữ chỗ cho bạn.";
            }
            case ApplicationStatus.REJECTED -> {
                source = NotificationSource.APPLICATION_REJECTED;
                title = "Đơn đăng ký #" + event.applicationId() + " bị từ chối";
                body =
                        "Đơn đăng ký ở KTX #"
                                + event.applicationId()
                                + " bị từ chối. Lý do: "
                                + (event.reason() == null ? "" : event.reason().trim());
            }
            case ApplicationStatus.ASSIGNED -> {
                source = NotificationSource.BED_HELD;
                String bed = event.bedLabel() == null ? "" : " " + event.bedLabel();
                title = "Đơn đăng ký #" + event.applicationId() + " được giữ chỗ giường" + bed;
                body =
                        "Bạn được giữ chỗ giường"
                                + bed
                                + ". Vui lòng mở chi tiết đơn để xem hạn kích hoạt hợp đồng và"
                                + " nộp tiền cọc đúng hạn.";
            }
            default -> {
                return;
            }
        }
        try {
            service.notifyUsers(
                    List.of(event.studentId()), title, body, source, event.applicationId());
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Không tạo được thông báo đơn đăng ký {}", event.applicationId(), exception);
        }
    }
}
