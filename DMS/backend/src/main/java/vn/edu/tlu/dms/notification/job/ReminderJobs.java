package vn.edu.tlu.dms.notification.job;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import vn.edu.tlu.dms.billing.service.DepositReminderLookup;
import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.constants.DmsConstants.SystemJob;
import vn.edu.tlu.dms.common.service.SystemJobService;
import vn.edu.tlu.dms.contract.service.ContractLifecycleService;
import vn.edu.tlu.dms.notification.service.SystemReminderService;

@Component
@ConditionalOnProperty(name = "dms.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class ReminderJobs {

    private final SystemJobService jobs;
    private final ContractLifecycleService contracts;
    private final DepositReminderLookup deposits;
    private final SystemReminderService reminders;

    public ReminderJobs(
            SystemJobService jobs,
            ContractLifecycleService contracts,
            DepositReminderLookup deposits,
            SystemReminderService reminders) {
        this.jobs = jobs;
        this.contracts = contracts;
        this.deposits = deposits;
        this.reminders = reminders;
    }

    @Scheduled(
            cron = "${dms.jobs.deposit-reminder-cron:0 0 * * * *}",
            zone = DmsConstants.APPLICATION_TIME_ZONE)
    public void remindDeposits() {
        jobs.run(
                SystemJob.REMIND_DEPOSIT,
                deposits::findOverdueContractIds,
                reminders::remindOverdueDeposit);
    }

    @Scheduled(
            cron = "${dms.jobs.contract-reminder-cron:0 0 8 * * *}",
            zone = DmsConstants.APPLICATION_TIME_ZONE)
    public void remindContracts() {
        jobs.run(
                SystemJob.REMIND_CONTRACT,
                contracts::findExpiringIds,
                reminders::remindExpiringContract);
    }
}
