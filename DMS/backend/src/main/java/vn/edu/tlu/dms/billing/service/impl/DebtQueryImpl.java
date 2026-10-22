package vn.edu.tlu.dms.billing.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.service.DebtQuery;

/**
 * Cài đặt truy vấn chỉ đọc về công nợ của sinh viên và phòng (SAD 4.1, 4.2).
 * Không khóa bản ghi và không làm thay đổi trạng thái giao dịch.
 */
@Service
@Transactional(readOnly = true)
public class DebtQueryImpl implements DebtQuery {

    private final JdbcTemplate jdbc;

    public DebtQueryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

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
}
