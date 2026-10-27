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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.NotificationSource;
import vn.edu.tlu.dms.contract.domain.ContractMovedOutEvent;
import vn.edu.tlu.dms.contract.domain.StorageRecordChangedEvent;
import vn.edu.tlu.dms.notification.service.NotificationService;

import java.lang.reflect.Method;
import java.util.List;

/** Kiểm tra người nhận, mã nguồn và nội dung thông báo contract; không cần cơ sở dữ liệu. */
class ContractNotificationListenerTest {

    private NotificationService service;

    private ContractNotificationListener listener;

    @BeforeEach
    void setUp() {
        service = mock(NotificationService.class);
        listener = new ContractNotificationListener(service);
    }

    @Test
    void movedOutNotifiesStudentWithContractLinkSource() {
        listener.onContractMovedOut(new ContractMovedOutEvent(21L, 7L, "MOVED_OUT"));

        verify(service)
                .notifyUsers(
                        eq(List.of(7L)),
                        eq("Hợp đồng #21 đã trả phòng"),
                        contains("đã được ghi nhận trả phòng"),
                        eq(NotificationSource.CONTRACT_MOVED_OUT),
                        eq(21L));
    }

    @Test
    void movedOutWithoutStudentCreatesNoNotification() {
        listener.onContractMovedOut(new ContractMovedOutEvent(21L, null, "MOVED_OUT"));

        verifyNoInteractions(service);
    }

    @Test
    void storageCreatedNotifiesStudent() {
        listener.onStorageRecordChanged(
                new StorageRecordChangedEvent(
                        5L, 21L, 7L, BusinessAction.CREATE_STORAGE_RECORD));

        verify(service)
                .notifyUsers(
                        eq(List.of(7L)),
                        eq("Đã tạo phiếu gửi đồ #5"),
                        contains("đã được tạo cho hợp đồng #21"),
                        eq(NotificationSource.STORAGE_RECORD_CHANGED),
                        eq(5L));
    }

    @Test
    void storageReturnedNotifiesStudent() {
        listener.onStorageRecordChanged(
                new StorageRecordChangedEvent(5L, 21L, 7L, BusinessAction.RETURN_STORAGE));

        verify(service)
                .notifyUsers(
                        eq(List.of(7L)),
                        eq("Đã nhận lại đồ gửi #5"),
                        contains("nhận lại đồ"),
                        eq(NotificationSource.STORAGE_RECORD_CHANGED),
                        eq(5L));
    }

    @Test
    void storageWithoutStudentCreatesNoNotification() {
        listener.onStorageRecordChanged(
                new StorageRecordChangedEvent(
                        5L, 21L, null, BusinessAction.CREATE_STORAGE_RECORD));

        verifyNoInteractions(service);
    }

    @Test
    void notificationFailureIsLoggedAndDoesNotEscape() {
        doThrow(new IllegalStateException("lỗi gửi thông báo"))
                .when(service)
                .notifyUsers(anyList(), anyString(), anyString(), anyString(), anyLong());

        assertDoesNotThrow(
                () -> listener.onContractMovedOut(new ContractMovedOutEvent(21L, 7L, "MOVED_OUT")));
        assertDoesNotThrow(
                () ->
                        listener.onStorageRecordChanged(
                                new StorageRecordChangedEvent(
                                        5L, 21L, 7L, BusinessAction.RETURN_STORAGE)));
    }

    @Test
    void handlersRunOnlyAfterBusinessTransactionCommits() throws Exception {
        assertAfterCommit("onContractMovedOut", ContractMovedOutEvent.class);
        assertAfterCommit("onStorageRecordChanged", StorageRecordChangedEvent.class);
    }

    private static void assertAfterCommit(String methodName, Class<?> eventType) throws Exception {
        Method method = ContractNotificationListener.class.getMethod(methodName, eventType);
        TransactionalEventListener annotation =
                method.getAnnotation(TransactionalEventListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
