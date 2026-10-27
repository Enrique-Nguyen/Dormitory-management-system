package vn.edu.tlu.dms.registration.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.SummerChoice;
import vn.edu.tlu.dms.common.constants.DmsConstants.SummerStatus;
import vn.edu.tlu.dms.contract.service.SummerLeaveLookup;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Cài đặt cổng đọc lựa chọn tạm rời hè đã duyệt cho module contract. Chỉ đọc bảng summer_choice và
 * semester của module registration, không trả entity.
 */
@Service
@Transactional(readOnly = true)
public class SummerLeaveLookupImpl implements SummerLeaveLookup {

    private static final String SELECT_APPROVED_TEMP_LEAVE =
            """
            SELECT sc.id, sc.semester_id, s.end_date
            FROM summer_choice sc
            JOIN semester s ON s.id = sc.semester_id
            WHERE sc.contract_id = ? AND sc.choice = ? AND sc.status = ?
            ORDER BY s.end_date DESC, sc.id DESC
            LIMIT 1
            """;

    private final JdbcTemplate jdbc;

    public SummerLeaveLookupImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<SummerLeave> findApprovedTempLeave(Long contractId) {
        if (contractId == null) {
            return Optional.empty();
        }
        return jdbc
                .query(
                        SELECT_APPROVED_TEMP_LEAVE,
                        (rs, i) ->
                                new SummerLeave(
                                        rs.getLong("id"),
                                        rs.getLong("semester_id"),
                                        rs.getObject("end_date", LocalDate.class)),
                        contractId,
                        SummerChoice.TEMP_LEAVE,
                        SummerStatus.APPROVED)
                .stream()
                .findFirst();
    }
}
