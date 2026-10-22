package vn.edu.tlu.dms.maintenance.job;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.constants.DmsConstants.SystemJob;
import vn.edu.tlu.dms.common.service.SystemJobService;
import vn.edu.tlu.dms.maintenance.service.MaintenanceLifecycleService;

@Component
@ConditionalOnProperty(name = "dms.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class MaintenanceJobs {

    private final SystemJobService jobs;
    private final MaintenanceLifecycleService maintenance;

    public MaintenanceJobs(SystemJobService jobs, MaintenanceLifecycleService maintenance) {
        this.jobs = jobs;
        this.maintenance = maintenance;
    }

    @Scheduled(
            cron = "${dms.jobs.close-resolved-cron:0 0 * * * *}",
            zone = DmsConstants.APPLICATION_TIME_ZONE)
    public void closeResolved() {
        jobs.run(
                SystemJob.CLOSE_RESOLVED,
                maintenance::findAutoCloseIds,
                maintenance::closeResolved);
    }
}
