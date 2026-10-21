package vn.edu.tlu.dms.billing.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.service.DepositReminderLookup;
import vn.edu.tlu.dms.common.constants.DmsConstants.DepositType;

import java.time.LocalDate;
import java.util.List;

@Service
public class DepositReminderLookupImpl implements DepositReminderLookup {

    private final JdbcTemplate jdbc;

    public DepositReminderLookupImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findOverdueContractIds() {
        return jdbc.queryForList(
                "SELECT contract_id FROM v_deposit_balance WHERE topup_overdue ORDER BY"
                    + " contract_id",
                Long.class);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public List<OverdueDeduction> findUnpaidDeductions(Long contractId) {
        return jdbc.query(
                """
                WITH deductions AS (
                    SELECT id,amount,due_date,
                           SUM(-amount) OVER (ORDER BY created_at,id) AS cumulative
                    FROM deposit_transaction WHERE contract_id=? AND type=?
                ), topups AS (
                    SELECT COALESCE(SUM(amount),0) AS total FROM deposit_transaction
                    WHERE contract_id=? AND type=?
                )
                SELECT id,LEAST(-amount,cumulative-total) AS remaining,due_date
                FROM deductions CROSS JOIN topups
                WHERE cumulative>total AND due_date<CURRENT_DATE ORDER BY id
                """,
                (row, i) ->
                        new OverdueDeduction(
                                row.getLong("id"),
                                row.getBigDecimal("remaining"),
                                row.getObject("due_date", LocalDate.class)),
                contractId,
                DepositType.DEDUCTION,
                contractId,
                DepositType.TOPUP);
    }
}
