package vn.edu.tlu.dms.maintenance.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ConfigKey;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.MaintenanceStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.SystemJob;
import vn.edu.tlu.dms.common.service.ConfigService;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.common.service.SystemJobService.Change;
import vn.edu.tlu.dms.maintenance.service.MaintenanceLifecycleService;

import java.util.List;

@Service
public class MaintenanceLifecycleServiceImpl implements MaintenanceLifecycleService {

    private final JdbcTemplate jdbc;
    private final ConfigService config;
    private final StatusHistoryService history;

    public MaintenanceLifecycleServiceImpl(
            JdbcTemplate jdbc, ConfigService config, StatusHistoryService history) {
        this.jdbc = jdbc;
        this.config = config;
        this.history = history;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findAutoCloseIds() {
        return jdbc.queryForList(
                "SELECT id FROM maintenance_request WHERE status=?"
                        + " AND resolved_at<CURRENT_TIMESTAMP-(? * INTERVAL '1 day') ORDER BY id",
                Long.class,
                MaintenanceStatus.RESOLVED,
                config.positiveInt(ConfigKey.MAINTENANCE_AUTOCLOSE_DAYS));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Change closeResolved(Long id) {
        var rows =
                jdbc.queryForList(
                        "SELECT id FROM maintenance_request WHERE id=? AND status=? AND"
                            + " resolved_at<CURRENT_TIMESTAMP-(? * INTERVAL '1 day') FOR UPDATE",
                        id,
                        MaintenanceStatus.RESOLVED,
                        config.positiveInt(ConfigKey.MAINTENANCE_AUTOCLOSE_DAYS));
        if (rows.isEmpty()) {
            return null;
        }
        jdbc.update(
                "UPDATE maintenance_request SET status=?,closed_at=CURRENT_TIMESTAMP WHERE id=?",
                MaintenanceStatus.CLOSED,
                id);
        history.record(
                EntityType.MAINTENANCE,
                id,
                MaintenanceStatus.RESOLVED,
                MaintenanceStatus.CLOSED,
                null,
                SystemJob.AUTOCLOSE_REASON);
        return new Change(
                AuditAction.MAINTENANCE_AUTO_CLOSE,
                EntityType.MAINTENANCE,
                id,
                MaintenanceStatus.RESOLVED,
                MaintenanceStatus.CLOSED,
                SystemJob.AUTOCLOSE_REASON);
    }
}
