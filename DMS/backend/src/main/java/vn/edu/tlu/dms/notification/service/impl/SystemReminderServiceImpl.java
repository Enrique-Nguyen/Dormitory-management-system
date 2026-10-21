package vn.edu.tlu.dms.notification.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.service.DepositReminderLookup;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.NotificationScope;
import vn.edu.tlu.dms.common.constants.DmsConstants.SystemJob;
import vn.edu.tlu.dms.common.service.SystemJobService.Change;
import vn.edu.tlu.dms.contract.service.ContractLifecycleService;
import vn.edu.tlu.dms.notification.service.SystemReminderService;

import java.util.Map;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class SystemReminderServiceImpl implements SystemReminderService {

    private final JdbcTemplate jdbc;
    private final ContractLifecycleService contracts;
    private final DepositReminderLookup deposits;

    public SystemReminderServiceImpl(
            JdbcTemplate jdbc, ContractLifecycleService contracts, DepositReminderLookup deposits) {
        this.jdbc = jdbc;
        this.contracts = contracts;
        this.deposits = deposits;
    }

    @Override
    public Change remindExpiringContract(Long contractId) {
        var contract = contracts.lockActiveContract(contractId).orElse(null);
        if (contract == null || !contracts.isExpiring(contract)) {
            return null;
        }
        String title = "Hợp đồng sắp hết hạn: " + contract.code() + " (" + contract.endDate() + ")";
        int created =
                createOnce(
                        contract.studentId(),
                        SystemJob.CONTRACT_SOURCE,
                        contractId,
                        title,
                        "Hợp đồng "
                                + contract.code()
                                + " hết hạn ngày "
                                + contract.endDate()
                                + ". Vui lòng xem hợp đồng và gửi yêu cầu gia hạn nếu tiếp tục lưu"
                                + " trú.");
        return created == 0
                ? null
                : new Change(
                        AuditAction.CONTRACT_EXPIRING_REMINDER,
                        EntityType.CONTRACT,
                        contractId,
                        null,
                        Map.of("endDate", contract.endDate()),
                        null);
    }

    @Override
    public Change remindOverdueDeposit(Long contractId) {
        var contract = contracts.lockActiveContract(contractId).orElse(null);
        if (contract == null) {
            return null;
        }
        int created = 0;
        for (var deduction : deposits.findUnpaidDeductions(contractId)) {
            created +=
                    createOnce(
                            contract.studentId(),
                            SystemJob.DEPOSIT_SOURCE,
                            deduction.id(),
                            "Nhắc bù cọc quá hạn: " + contract.code(),
                            "Khoản trừ cọc "
                                    + deduction.id()
                                    + " của hợp đồng "
                                    + contract.code()
                                    + " còn thiếu "
                                    + deduction.remaining().toPlainString()
                                    + " đồng, hạn bù "
                                    + deduction.dueDate()
                                    + ". Vui lòng liên hệ Ban quản lý để bù cọc.");
        }
        return created == 0
                ? null
                : new Change(
                        AuditAction.DEPOSIT_REMINDER,
                        EntityType.CONTRACT,
                        contractId,
                        null,
                        Map.of("notifications", created),
                        null);
    }

    /**
     * Nơi gọi giữ khóa hợp đồng: kiểm tra và insert cùng transaction nên hai job không gửi trùng.
     */
    private int createOnce(Long userId, String source, Long sourceId, String title, String body) {
        return jdbc.update(
                """
INSERT INTO notification(scope,user_id,title,body,source_type,source_id,created_by)
SELECT ?,?,?,?,?,?,NULL WHERE NOT EXISTS (
    SELECT 1 FROM notification WHERE user_id=? AND source_type=? AND source_id=? AND title=?
)
""",
                NotificationScope.USER,
                userId,
                title,
                body,
                source,
                sourceId,
                userId,
                source,
                sourceId,
                title);
    }
}
