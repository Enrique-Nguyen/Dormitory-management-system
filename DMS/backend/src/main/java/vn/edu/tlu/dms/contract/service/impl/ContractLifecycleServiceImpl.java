package vn.edu.tlu.dms.contract.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentEndReason;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ConfigKey;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.SystemJob;
import vn.edu.tlu.dms.common.service.ConfigService;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.common.service.SystemJobService.Change;
import vn.edu.tlu.dms.contract.domain.PendingContractExpired;
import vn.edu.tlu.dms.contract.service.ContractLifecycleService;
import vn.edu.tlu.dms.room.service.BedOccupancyService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class ContractLifecycleServiceImpl implements ContractLifecycleService {

    private final JdbcTemplate jdbc;
    private final BedOccupancyService occupancy;
    private final StatusHistoryService history;
    private final ConfigService config;
    private final ApplicationEventPublisher events;

    public ContractLifecycleServiceImpl(
            JdbcTemplate jdbc,
            BedOccupancyService occupancy,
            StatusHistoryService history,
            ConfigService config,
            ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.occupancy = occupancy;
        this.history = history;
        this.config = config;
        this.events = events;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findExpiredPendingIds() {
        return jdbc.queryForList(
                "SELECT id FROM dormitory_contract WHERE status=?"
                        + " AND activation_deadline<CURRENT_TIMESTAMP ORDER BY id",
                Long.class,
                ContractStatus.PENDING);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Change cancelExpiredPending(Long id) {
        var studentIds =
                jdbc.queryForList(
                        "SELECT student_id FROM dormitory_contract WHERE id=? AND status=?"
                                + " AND activation_deadline<CURRENT_TIMESTAMP",
                        Long.class,
                        id,
                        ContractStatus.PENDING);
        if (studentIds.isEmpty()) {
            return null;
        }
        occupancy.lockOpenAssignment(id, studentIds.get(0));
        var rows =
                jdbc.queryForList(
                        "SELECT application_id FROM dormitory_contract WHERE id=? AND status=? AND"
                                + " activation_deadline<CURRENT_TIMESTAMP FOR UPDATE",
                        id,
                        ContractStatus.PENDING);
        if (rows.isEmpty()) {
            return null;
        }
        // SELECT FOR UPDATE re-evaluate điều kiện sau khi chờ khóa ở READ COMMITTED.
        occupancy.release(id, AssignmentEndReason.HOLD_EXPIRED, null, SystemJob.EXPIRED_REASON);
        jdbc.update(
                "UPDATE dormitory_contract SET status=?,closed_at=CURRENT_TIMESTAMP,status_reason=?"
                        + " WHERE id=?",
                ContractStatus.CANCELLED,
                SystemJob.EXPIRED_REASON,
                id);
        history.record(
                EntityType.CONTRACT,
                id,
                ContractStatus.PENDING,
                ContractStatus.CANCELLED,
                null,
                SystemJob.EXPIRED_REASON);
        events.publishEvent(
                new PendingContractExpired(
                        id,
                        ((Number) rows.get(0).get("application_id")).longValue(),
                        SystemJob.EXPIRED_REASON));
        return new Change(
                AuditAction.CONTRACT_AUTO_CANCEL,
                EntityType.CONTRACT,
                id,
                ContractStatus.PENDING,
                ContractStatus.CANCELLED,
                SystemJob.EXPIRED_REASON);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findExpiringIds() {
        return jdbc.queryForList(
                "SELECT id FROM dormitory_contract WHERE status=?"
                        + " AND end_date>=CURRENT_DATE AND end_date<=CURRENT_DATE+? ORDER BY id",
                Long.class,
                ContractStatus.ACTIVE,
                config.positiveInt(ConfigKey.CONTRACT_EXPIRING_DAYS));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<ActiveContract> lockActiveContract(Long id) {
        return jdbc
                .query(
                        "SELECT id,student_id,code,end_date FROM dormitory_contract"
                                + " WHERE id=? AND status=? FOR UPDATE",
                        (row, i) ->
                                new ActiveContract(
                                        row.getLong("id"),
                                        row.getLong("student_id"),
                                        row.getString("code"),
                                        row.getObject("end_date", LocalDate.class)),
                        id,
                        ContractStatus.ACTIVE)
                .stream()
                .findFirst();
    }

    @Override
    public boolean isExpiring(ActiveContract contract) {
        var today = LocalDate.now(ZoneId.of(DmsConstants.APPLICATION_TIME_ZONE));
        return !contract.endDate().isBefore(today)
                && !contract.endDate()
                        .isAfter(
                                today.plusDays(
                                        config.positiveInt(ConfigKey.CONTRACT_EXPIRING_DAYS)));
    }
}
