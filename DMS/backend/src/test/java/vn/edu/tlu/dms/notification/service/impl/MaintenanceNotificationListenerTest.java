package vn.edu.tlu.dms.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import vn.edu.tlu.dms.common.constants.DmsConstants.MaintenanceStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.NotificationSource;
import vn.edu.tlu.dms.maintenance.domain.TicketConcludedEvent;
import vn.edu.tlu.dms.maintenance.domain.TicketStatusChangedEvent;
import vn.edu.tlu.dms.notification.service.NotificationService;

import java.lang.reflect.Method;
import java.util.List;

/** Kiểm tra nội dung và người nhận thông báo ticket; không cần cơ sở dữ liệu. */
class MaintenanceNotificationListenerTest {

    private NotificationService service;

    private MaintenanceNotificationListener listener;

    @BeforeEach
    void setUp() {
        service = mock(NotificationService.class);
        listener = new MaintenanceNotificationListener(service);
    }

    @Test
    void statusChangedNotifiesRequesterAndAssignedTechnician() {
        listener.onTicketStatusChanged(
                new TicketStatusChangedEvent(
                        12L, 7L, 9L, MaintenanceStatus.ASSIGNED));

        verify(service)
                .notifyUsers(
                        eq(List.of(7L, 9L)),
                        eq("Phiếu sửa chữa #12 đã được phân công cho kỹ thuật viên"),
                        contains("Phiếu sửa chữa #12"),
                        eq(NotificationSource.TICKET_STATUS_CHANGED),
                        eq(12L));
    }

    @Test
    void assigneeSameAsRequesterReceivesOneNotification() {
        listener.onTicketStatusChanged(
                new TicketStatusChangedEvent(
                        12L, 7L, 7L, MaintenanceStatus.IN_PROGRESS));

        verify(service)
                .notifyUsers(
                        eq(List.of(7L)),
                        contains("đang được kỹ thuật viên xử lý"),
                        contains("đang được kỹ thuật viên xử lý"),
                        eq(NotificationSource.TICKET_STATUS_CHANGED),
                        eq(12L));
    }

    @Test
    void statusWithoutRecipientsDoesNotCreateNotification() {
        listener.onTicketStatusChanged(
                new TicketStatusChangedEvent(12L, null, null, MaintenanceStatus.CLOSED));

        verifyNoInteractions(service);
    }

    @Test
    void conclusionNotifiesRequesterAboutDepositDeduction() {
        listener.onTicketConcluded(
                new TicketConcludedEvent(15L, 7L, "MISUSE", List.of(7L, 8L)));

        verify(service)
                .notifyUsers(
                        eq(List.of(7L)),
                        eq("Kết luận phiếu sửa chữa #15"),
                        contains("trừ vào cọc của bạn"),
                        eq(NotificationSource.TICKET_STATUS_CHANGED),
                        eq(15L));
    }

    @Test
    void chargedStudentOtherThanRequesterGetsDepositNotice() {
        listener.onTicketConcluded(
                new TicketConcludedEvent(15L, 7L, "MISUSE", List.of(7L, 8L)));

        verify(service)
                .notifyUsers(
                        eq(List.of(8L)),
                        eq("Trừ cọc do sửa chữa phiếu #15"),
                        contains("trừ vào cọc của bạn"),
                        eq(NotificationSource.DEPOSIT_DEDUCTED),
                        eq(15L));
    }

    @Test
    void requesterChargedAloneReceivesOnlyConclusionNotice() {
        listener.onTicketConcluded(
                new TicketConcludedEvent(15L, 7L, "MISUSE", List.of(7L)));

        verify(service, times(1))
                .notifyUsers(anyList(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void depositNoticeFailureDoesNotBreakConclusionNotice() {
        doThrow(new IllegalStateException("lỗi gửi thông báo trừ cọc"))
                .when(service)
                .notifyUsers(
                        eq(List.of(8L)),
                        anyString(),
                        anyString(),
                        eq(NotificationSource.DEPOSIT_DEDUCTED),
                        anyLong());

        assertDoesNotThrow(
                () ->
                        listener.onTicketConcluded(
                                new TicketConcludedEvent(15L, 7L, "MISUSE", List.of(7L, 8L))));
        verify(service)
                .notifyUsers(
                        eq(List.of(7L)),
                        eq("Kết luận phiếu sửa chữa #15"),
                        anyString(),
                        eq(NotificationSource.TICKET_STATUS_CHANGED),
                        eq(15L));
    }

    @Test
    void notificationFailureIsLoggedAndDoesNotEscape() {
        doThrow(new IllegalStateException("lỗi gửi thông báo"))
                .when(service)
                .notifyUsers(anyList(), anyString(), anyString(), anyString(), anyLong());

        assertDoesNotThrow(
                () ->
                        listener.onTicketStatusChanged(
                                new TicketStatusChangedEvent(
                                        12L, 7L, null, MaintenanceStatus.RESOLVED)));
    }

    @Test
    void handlersRunOnlyAfterBusinessTransactionCommits() throws Exception {
        assertAfterCommit("onTicketStatusChanged", TicketStatusChangedEvent.class);
        assertAfterCommit("onTicketConcluded", TicketConcludedEvent.class);
    }

    private static void assertAfterCommit(String methodName, Class<?> eventType) throws Exception {
        Method method = MaintenanceNotificationListener.class.getMethod(methodName, eventType);
        TransactionalEventListener annotation =
                method.getAnnotation(TransactionalEventListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
