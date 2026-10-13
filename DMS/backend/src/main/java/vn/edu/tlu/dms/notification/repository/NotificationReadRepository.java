package vn.edu.tlu.dms.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.notification.domain.NotificationRead;
import vn.edu.tlu.dms.notification.domain.NotificationReadId;

public interface NotificationReadRepository
        extends JpaRepository<NotificationRead, NotificationReadId>,
                JpaSpecificationExecutor<NotificationRead> {}
