package vn.edu.tlu.dms.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.NotificationScope;
import vn.edu.tlu.dms.common.constants.DmsConstants.NotificationSource;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.notification.domain.Notification;
import vn.edu.tlu.dms.notification.repository.NotificationRepository;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/** Kiểm tra lọc người nhận và chống trùng của hàm tạo thông báo hệ thống. */
class NotificationServiceImplTest {

    private NotificationRepository repository;

    private JdbcTemplate jdbc;

    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        jdbc = mock(JdbcTemplate.class);
        service =
                new NotificationServiceImpl(
                        repository,
                        jdbc,
                        mock(CurrentUser.class),
                        mock(ScopeGuard.class));
    }

    @Test
    void notifyUsersSkipsNullAndDuplicateRecipients() {
        service.notifyUsers(
                Arrays.asList(3L, null, 3L, 4L),
                "Tiêu đề",
                "Nội dung",
                NotificationSource.TICKET_STATUS_CHANGED,
                9L);

        ArgumentCaptor<Notification> notifications =
                ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(notifications.capture());
        verify(repository).flush();
        assertThat(notifications.getAllValues())
                .extracting(Notification::getUserId)
                .containsExactly(3L, 4L);
        assertThat(notifications.getAllValues())
                .allSatisfy(
                        notification -> {
                            assertThat(notification.getScope()).isEqualTo(NotificationScope.USER);
                            assertThat(notification.getSourceType())
                                    .isEqualTo(NotificationSource.TICKET_STATUS_CHANGED);
                            assertThat(notification.getSourceId()).isEqualTo(9L);
                            assertThat(notification.getCreatedAt()).isNotNull();
                        });
    }

    @Test
    void notifyUsersDoesNothingWithoutRecipients() {
        service.notifyUsers(
                List.of(),
                "Tiêu đề",
                "Nội dung",
                NotificationSource.TICKET_STATUS_CHANGED,
                9L);

        verifyNoInteractions(jdbc, repository);
    }

    @Test
    void notifyUsersDoesNothingWithBlankContent() {
        service.notifyUsers(
                List.of(3L),
                " ",
                "Nội dung",
                NotificationSource.TICKET_STATUS_CHANGED,
                9L);

        verifyNoInteractions(jdbc, repository);
    }

    @Test
    void notifyUsersSkipsExistingSourceAndTitle() {
        when(jdbc.queryForObject(
                        anyString(), eq(Boolean.class), any(), any(), any(), any()))
                .thenReturn(Boolean.TRUE);

        service.notifyUsers(
                List.of(3L),
                "Tiêu đề",
                "Nội dung",
                NotificationSource.TICKET_STATUS_CHANGED,
                9L);

        verify(repository, never()).save(any());
    }

    @Test
    void notifyUsersRunsInNewTransactionSoListenerFailureCannotRollbackBusiness() throws Exception {
        Method method =
                NotificationServiceImpl.class.getMethod(
                        "notifyUsers",
                        List.class,
                        String.class,
                        String.class,
                        String.class,
                        Long.class);

        Transactional annotation = method.getAnnotation(Transactional.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    void ticketStatusChangedLinksToTicketDetailPerPortal() {
        assertThat(
                        service.notificationLink(
                                "/student", NotificationSource.TICKET_STATUS_CHANGED, 12L))
                .isEqualTo("/student/maintenance/12");
        assertThat(
                        service.notificationLink(
                                "/technician", NotificationSource.TICKET_STATUS_CHANGED, 12L))
                .isEqualTo("/technician/tasks/12");
        assertThat(
                        service.notificationLink(
                                "/staff", NotificationSource.TICKET_STATUS_CHANGED, 12L))
                .isEqualTo("/staff/maintenance/12");
        assertThat(
                        service.notificationLink(
                                "/student", NotificationSource.TICKET_STATUS_CHANGED, null))
                .isNull();
    }

    @Test
    void depositDeductedLinksToDepositScreen() {
        assertThat(service.notificationLink("/student", NotificationSource.DEPOSIT_DEDUCTED, 15L))
                .isEqualTo("/student/deposit");
    }

    @Test
    void contractExpiringLinksToContractDetailOrList() {
        assertThat(service.notificationLink("/student", NotificationSource.CONTRACT_EXPIRING, 21L))
                .isEqualTo("/student/contracts/21");
        assertThat(service.notificationLink("/student", NotificationSource.CONTRACT_EXPIRING, null))
                .isEqualTo("/student/contracts");
    }

    @Test
    void manualNotificationHasNoBusinessLink() {
        assertThat(service.notificationLink("/student", NotificationSource.MANUAL, 3L)).isNull();
        assertThat(service.notificationLink("/student", null, 3L)).isNull();
    }
}
