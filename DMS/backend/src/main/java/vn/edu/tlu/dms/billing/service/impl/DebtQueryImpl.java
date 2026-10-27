package vn.edu.tlu.dms.billing.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.repository.InvoiceRepository;
import vn.edu.tlu.dms.billing.service.DebtQuery;
import vn.edu.tlu.dms.common.constants.DmsConstants.InvoiceStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.InvoiceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Cài đặt truy vấn chỉ đọc về công nợ của sinh viên và phòng (SAD 4.1, 4.2). Không khóa bản ghi và
 * không làm thay đổi trạng thái giao dịch.
 */
@Service
@Transactional(readOnly = true)
public class DebtQueryImpl implements DebtQuery {

    private final JdbcTemplate jdbc;

    public DebtQueryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Tổng hợp nợ phí phòng và nợ cọc của sinh viên cho các nghiệp vụ nội bộ. */
    @Override
    public StudentDebt getStudentDebt(Long studentId) {
        if (studentId == null) {
            return new StudentDebt(null, 0L, 0L, 0L, false);
        }

        Long unpaidRoomFee =
                jdbc.queryForObject(
                        """
                        SELECT COALESCE(SUM(total_amount), 0)
                        FROM invoice
                        WHERE student_id = ? AND type = 'ROOM_FEE' AND status = 'UNPAID'
                        """,
                        Long.class,
                        studentId);

        Long depositDebt =
                jdbc.queryForObject(
                        """
                        SELECT COALESCE(SUM(deposit_debt), 0)
                        FROM v_deposit_balance
                        WHERE student_id = ?
                        """,
                        Long.class,
                        studentId);

        long roomFee = unpaidRoomFee != null ? unpaidRoomFee : 0L;
        long depDebt = depositDebt != null ? depositDebt : 0L;
        long total = roomFee + depDebt;

        return new StudentDebt(studentId, roomFee, depDebt, total, total > 0);
    }

    /** Tổng hợp nợ hóa đơn điện nước của phòng cho các nghiệp vụ nội bộ. */
    @Override
    public RoomDebt getRoomDebt(Long roomId) {
        if (roomId == null) {
            return new RoomDebt(null, 0L, false);
        }

        Long unpaidUtility =
                jdbc.queryForObject(
                        """
                        SELECT COALESCE(SUM(total_amount), 0)
                        FROM invoice
                        WHERE room_id = ? AND type = 'UTILITY' AND status = 'UNPAID'
                        """,
                        Long.class,
                        roomId);

        long utility = unpaidUtility != null ? unpaidUtility : 0L;
        return new RoomDebt(roomId, utility, utility > 0);
    }

    @Override
    public List<UnpaidInvoice> findUnpaidRoomFeeInvoices(Long studentId) {
        return jdbc.query(
                """
                SELECT id, code, total_amount, due_date
                FROM invoice
                WHERE student_id = ? AND type = ? AND status = ?
                ORDER BY due_date, id
                """,
                (rs, i) ->
                        new UnpaidInvoice(
                                rs.getLong("id"),
                                rs.getString("code"),
                                rs.getLong("total_amount"),
                                rs.getObject("due_date", java.time.LocalDate.class)),
                studentId,
                InvoiceType.ROOM_FEE,
                InvoiceStatus.UNPAID);
    }

    @Override
    public List<UnpaidInvoice> findUnpaidUtilityInvoices(Long roomId) {
        return jdbc.query(
                """
                SELECT id, code, total_amount, due_date
                FROM invoice
                WHERE room_id = ? AND type = ? AND status = ?
                ORDER BY due_date, id
                """,
                (rs, i) ->
                        new UnpaidInvoice(
                                rs.getLong("id"),
                                rs.getString("code"),
                                rs.getLong("total_amount"),
                                rs.getObject("due_date", java.time.LocalDate.class)),
                roomId,
                InvoiceType.UTILITY,
                InvoiceStatus.UNPAID);
    }

    /** Dùng cùng phạm vi với danh sách hóa đơn, theo đơn vị và học kỳ của từng hóa đơn. */
    @Override
    public FinanceSummary getFinanceSummary(Long managementUnitId) {
        String scope =
                managementUnitId == null
                        ? "TRUE"
                        : "(" + InvoiceRepository.STAFF_UNIT_SCOPE.replace(":unitId", "?") + ")";
        var args = new ArrayList<Object>();
        args.add(InvoiceType.ROOM_FEE);
        args.add(InvoiceType.UTILITY);
        args.add(InvoiceStatus.UNPAID);
        if (managementUnitId != null) {
            long parameters =
                    InvoiceRepository.STAFF_UNIT_SCOPE.chars().filter(c -> c == ':').count();
            for (int i = 0; i < parameters; i++) args.add(managementUnitId);
        }
        var totals =
                jdbc.queryForMap(
                        """
                        SELECT COALESCE(SUM(i.total_amount) FILTER (WHERE i.type=?),0) AS room_fee,
                               COALESCE(SUM(i.total_amount) FILTER (WHERE i.type=?),0) AS utility
                        FROM invoice i WHERE i.status=? AND
                        """
                                + scope,
                        args.toArray());
        Long topup =
                jdbc.queryForObject(
                        """
                        SELECT COALESCE(SUM(v.topup_needed),0) FROM v_deposit_balance v
                        JOIN dormitory_contract c ON c.id=v.contract_id
                        JOIN room_application ap ON ap.id=c.application_id
                        WHERE (CAST(? AS bigint) IS NULL OR ap.preferred_unit_id=?)
                        """,
                        Long.class,
                        managementUnitId,
                        managementUnitId);
        return new FinanceSummary(
                ((Number) totals.get("room_fee")).longValue(),
                ((Number) totals.get("utility")).longValue(),
                topup == null ? 0 : topup);
    }
}
