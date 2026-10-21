package vn.edu.tlu.dms.contract.job;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.constants.DmsConstants.SystemJob;
import vn.edu.tlu.dms.common.service.SystemJobService;
import vn.edu.tlu.dms.contract.service.ContractLifecycleService;

@Component
@ConditionalOnProperty(name = "dms.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class ContractJobs {

    private final SystemJobService jobs;
    private final ContractLifecycleService contracts;

    public ContractJobs(SystemJobService jobs, ContractLifecycleService contracts) {
        this.jobs = jobs;
        this.contracts = contracts;
    }

    @Scheduled(
            cron = "${dms.jobs.cancel-pending-cron:0 0 * * * *}",
            zone = DmsConstants.APPLICATION_TIME_ZONE)
    public void cancelPending() {
        jobs.run(
                SystemJob.CANCEL_PENDING,
                contracts::findExpiredPendingIds,
                contracts::cancelExpiredPending);
    }
}
