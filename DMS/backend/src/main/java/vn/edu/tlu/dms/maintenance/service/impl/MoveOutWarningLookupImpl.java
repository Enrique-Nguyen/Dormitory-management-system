package vn.edu.tlu.dms.maintenance.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.MaintenanceStatus;
import vn.edu.tlu.dms.contract.service.MoveOutWarningLookup;
import vn.edu.tlu.dms.room.service.OccupancyQuery;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cài đặt cổng cảnh báo trả phòng cho module contract. Chỉ đọc bảng ticket của maintenance và dùng
 * OccupancyQuery của room để xác định sinh viên thực sự ở phòng tại thời điểm ticket được tạo.
 */
@Service
@Transactional(readOnly = true)
public class MoveOutWarningLookupImpl implements MoveOutWarningLookup {

    private static final String SELECT_WARNINGS =
            """
            SELECT id, code, status, created_at, (cause IS NOT NULL) AS concluded
            FROM maintenance_request
            WHERE room_id = ? AND created_at <= ?
              AND (status IN (?, ?, ?, ?) OR (status = ? AND cause IS NULL))
            ORDER BY created_at, id
            """;

    private final JdbcTemplate jdbc;

    private final OccupancyQuery occupancy;

    public MoveOutWarningLookupImpl(JdbcTemplate jdbc, OccupancyQuery occupancy) {
        this.jdbc = jdbc;
        this.occupancy = occupancy;
    }

    @Override
    public List<UnsettledTicket> findUnsettledTickets(
            Long studentId, Long roomId, OffsetDateTime at) {
        if (studentId == null || roomId == null) {
            return List.of();
        }
        OffsetDateTime moment = at == null ? OffsetDateTime.now() : at;
        List<WarningRow> rows =
                jdbc.query(
                        SELECT_WARNINGS,
                        (rs, i) ->
                                new WarningRow(
                                        rs.getLong("id"),
                                        rs.getString("code"),
                                        rs.getString("status"),
                                        rs.getBoolean("concluded"),
                                        rs.getObject("created_at", OffsetDateTime.class)),
                        roomId,
                        moment,
                        MaintenanceStatus.OPEN,
                        MaintenanceStatus.ASSIGNED,
                        MaintenanceStatus.IN_PROGRESS,
                        MaintenanceStatus.RESOLVED,
                        MaintenanceStatus.CLOSED);
        List<UnsettledTicket> tickets = new ArrayList<>();
        for (WarningRow row : rows) {
            boolean occupiedAtCreation =
                    occupancy.findOccupantsAt(roomId, row.createdAt()).stream()
                            .anyMatch(occupant -> studentId.equals(occupant.studentId()));
            if (occupiedAtCreation) {
                tickets.add(
                        new UnsettledTicket(
                                row.id(), row.code(), row.status(), row.concluded()));
            }
        }
        return tickets;
    }

    /** Dòng ticket tối thiểu để kiểm tra người ở tại thời điểm tạo ticket. */
    private record WarningRow(
            Long id, String code, String status, boolean concluded, OffsetDateTime createdAt) {}
}
