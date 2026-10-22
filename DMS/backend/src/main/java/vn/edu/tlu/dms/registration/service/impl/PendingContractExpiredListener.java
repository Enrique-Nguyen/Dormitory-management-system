package vn.edu.tlu.dms.registration.service.impl;

import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApplicationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.contract.domain.PendingContractExpired;

@Component
public class PendingContractExpiredListener {

    private final JdbcTemplate jdbc;
    private final StatusHistoryService history;
    private final AuditService audit;

    public PendingContractExpiredListener(
            JdbcTemplate jdbc, StatusHistoryService history, AuditService audit) {
        this.jdbc = jdbc;
        this.history = history;
        this.audit = audit;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onExpired(PendingContractExpired event) {
        String before =
                jdbc.queryForObject(
                        "SELECT status FROM room_application WHERE id=? FOR UPDATE",
                        String.class,
                        event.applicationId());
        if (ApplicationStatus.CANCELLED.equals(before)) {
            return;
        }
        if (!ApplicationStatus.ASSIGNED.equals(before)
                && !ApplicationStatus.APPROVED.equals(before)) {
            throw BusinessException.conflict(ErrorCode.INVALID_STATE_TRANSITION);
        }
        jdbc.update(
                "UPDATE room_application SET status=? WHERE id=?",
                ApplicationStatus.CANCELLED,
                event.applicationId());
        history.record(
                EntityType.APPLICATION,
                event.applicationId(),
                before,
                ApplicationStatus.CANCELLED,
                null,
                event.reason());
        audit.record(
                null,
                AuditAction.APPLICATION_AUTO_CANCEL,
                EntityType.APPLICATION,
                event.applicationId(),
                before,
                ApplicationStatus.CANCELLED,
                event.reason());
    }
}
