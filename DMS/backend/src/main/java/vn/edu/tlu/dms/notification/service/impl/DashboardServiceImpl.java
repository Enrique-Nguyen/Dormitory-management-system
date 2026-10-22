package vn.edu.tlu.dms.notification.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.notification.api.DashboardDtos;
import vn.edu.tlu.dms.notification.service.DashboardService;

import java.util.HashMap;
import java.util.Map;

/**
 * Cài đặt DashboardService truy vấn và tính toán các chỉ số thống kê cho Staff Dashboard.
 * Dữ liệu chỉ đọc theo phạm vi quyền của cán bộ (FR-NOT-05, FR-NOT-06).
 */
@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final JdbcTemplate jdbc;
    private final ScopeGuard scopeGuard;
    private final CurrentUser currentUser;

    public DashboardServiceImpl(
            JdbcTemplate jdbc,
            ScopeGuard scopeGuard,
            CurrentUser currentUser) {
        this.jdbc = jdbc;
        this.scopeGuard = scopeGuard;
        this.currentUser = currentUser;
    }

    @Override
    public DashboardDtos.StaffDashboardResponse getSummary(Long managementUnitId) {
        scopeGuard.requirePermission(Permission.DASHBOARD_READ);
        scopeGuard.requireRole(Role.STAFF, Role.ADMIN);

        var actor = currentUser.get();
        Long effectiveUnitId;
        if (actor.isAdmin() || actor.canReadAllUnits()) {
            effectiveUnitId = managementUnitId;
        } else {
            effectiveUnitId = actor.unitId();
        }

        String unitName = null;
        if (effectiveUnitId != null) {
            var names =
                    jdbc.query(
                            "SELECT name FROM management_unit WHERE id = ?",
                            (rs, rowNum) -> rs.getString("name"),
                            effectiveUnitId);
            if (!names.isEmpty()) {
                unitName = names.get(0);
            }
        }

        // 1. Thống kê sức chứa & lấp đầy
        Long opBeds =
                jdbc.queryForObject(
                        """
                        SELECT COUNT(b.id)
                        FROM bed b
                        JOIN room r ON r.id = b.room_id
                        JOIN building bd ON bd.id = r.building_id
                        WHERE b.status = 'ACTIVE' AND r.status = 'ACTIVE'
                          AND (? IS NULL OR bd.management_unit_id = ?)
                        """,
                        Long.class,
                        effectiveUnitId,
                        effectiveUnitId);
        long operationalBeds = opBeds != null ? opBeds : 0L;

        Long occBeds =
                jdbc.queryForObject(
                        """
                        SELECT COUNT(DISTINCT h.bed_id)
                        FROM room_assignment_history h
                        JOIN bed b ON b.id = h.bed_id
                        JOIN room r ON r.id = b.room_id
                        JOIN building bd ON bd.id = r.building_id
                        WHERE h.ended_at IS NULL
                          AND (? IS NULL OR bd.management_unit_id = ?)
                        """,
                        Long.class,
                        effectiveUnitId,
                        effectiveUnitId);
        long occupiedBeds = occBeds != null ? occBeds : 0L;

        Long maintBeds =
                jdbc.queryForObject(
                        """
                        SELECT COUNT(b.id)
                        FROM bed b
                        JOIN room r ON r.id = b.room_id
                        JOIN building bd ON bd.id = r.building_id
                        WHERE r.status = 'MAINTENANCE'
                          AND (? IS NULL OR bd.management_unit_id = ?)
                        """,
                        Long.class,
                        effectiveUnitId,
                        effectiveUnitId);
        long maintenanceBeds = maintBeds != null ? maintBeds : 0L;

        long availableBeds = Math.max(operationalBeds - occupiedBeds, 0L);
        double occupancyRate =
                operationalBeds > 0
                        ? Math.round((occupiedBeds * 1000.0) / operationalBeds) / 10.0
                        : 0.0;

        var capacity =
                new DashboardDtos.CapacityStats(
                        operationalBeds,
                        occupiedBeds,
                        availableBeds,
                        maintenanceBeds,
                        occupancyRate);

        // 2. Thống kê tài chính & công nợ
        Long unpaidRoom =
                jdbc.queryForObject(
                        """
                        SELECT COALESCE(SUM(i.total_amount), 0)
                        FROM invoice i
                        LEFT JOIN room_assignment_history h ON h.student_id = i.student_id AND h.ended_at IS NULL
                        LEFT JOIN bed b ON b.id = h.bed_id
                        LEFT JOIN room r ON r.id = b.room_id
                        LEFT JOIN building bd ON bd.id = r.building_id
                        WHERE i.type = 'ROOM_FEE' AND i.status = 'UNPAID'
                          AND (? IS NULL OR bd.management_unit_id = ?)
                        """,
                        Long.class,
                        effectiveUnitId,
                        effectiveUnitId);
        long unpaidRoomFee = unpaidRoom != null ? unpaidRoom : 0L;

        Long unpaidUtil =
                jdbc.queryForObject(
                        """
                        SELECT COALESCE(SUM(i.total_amount), 0)
                        FROM invoice i
                        JOIN room r ON r.id = i.room_id
                        JOIN building bd ON bd.id = r.building_id
                        WHERE i.type = 'UTILITY' AND i.status = 'UNPAID'
                          AND (? IS NULL OR bd.management_unit_id = ?)
                        """,
                        Long.class,
                        effectiveUnitId,
                        effectiveUnitId);
        long unpaidUtility = unpaidUtil != null ? unpaidUtil : 0L;

        Long topupNeeded =
                jdbc.queryForObject(
                        """
                        SELECT COALESCE(SUM(v.topup_needed), 0)
                        FROM v_deposit_balance v
                        JOIN dormitory_contract c ON c.id = v.contract_id
                        JOIN room_application ap ON ap.id = c.application_id
                        WHERE (? IS NULL OR ap.preferred_unit_id = ?)
                        """,
                        Long.class,
                        effectiveUnitId,
                        effectiveUnitId);
        long totalTopup = topupNeeded != null ? topupNeeded : 0L;

        var finance =
                new DashboardDtos.FinancialStats(
                        unpaidRoomFee + unpaidUtility,
                        unpaidRoomFee,
                        unpaidUtility,
                        totalTopup);

        // 3. Thống kê vận hành phòng ốc, hợp đồng và ticket
        Long missingLeaders =
                jdbc.queryForObject(
                        """
                        SELECT COUNT(r.id)
                        FROM room r
                        JOIN building bd ON bd.id = r.building_id
                        WHERE r.status = 'ACTIVE' AND r.room_leader_id IS NULL
                          AND (? IS NULL OR bd.management_unit_id = ?)
                        """,
                        Long.class,
                        effectiveUnitId,
                        effectiveUnitId);
        long roomsMissingLeader = missingLeaders != null ? missingLeaders : 0L;

        Long expiringSoon =
                jdbc.queryForObject(
                        """
                        SELECT COUNT(c.id)
                        FROM dormitory_contract c
                        JOIN room_application ap ON ap.id = c.application_id
                        WHERE c.status = 'ACTIVE'
                          AND c.end_date >= CURRENT_DATE AND c.end_date <= CURRENT_DATE + 15
                          AND (? IS NULL OR ap.preferred_unit_id = ?)
                        """,
                        Long.class,
                        effectiveUnitId,
                        effectiveUnitId);
        long contractsExpiringSoon = expiringSoon != null ? expiringSoon : 0L;

        Long overdueMoveout =
                jdbc.queryForObject(
                        """
                        SELECT COUNT(c.id)
                        FROM dormitory_contract c
                        JOIN room_application ap ON ap.id = c.application_id
                        WHERE c.status = 'ACTIVE'
                          AND c.end_date < CURRENT_DATE
                          AND (? IS NULL OR ap.preferred_unit_id = ?)
                        """,
                        Long.class,
                        effectiveUnitId,
                        effectiveUnitId);
        long contractsOverdueMoveout = overdueMoveout != null ? overdueMoveout : 0L;

        Map<String, Long> ticketsByStatus = new HashMap<>();
        jdbc.query(
                """
                SELECT m.status, COUNT(m.id) AS cnt
                FROM maintenance_request m
                LEFT JOIN room r ON r.id = m.room_id
                LEFT JOIN building bd ON bd.id = COALESCE(r.building_id, m.building_id)
                WHERE (? IS NULL OR bd.management_unit_id = ?)
                GROUP BY m.status
                """,
                rs -> {
                    ticketsByStatus.put(rs.getString("status"), rs.getLong("cnt"));
                },
                effectiveUnitId,
                effectiveUnitId);

        var operations =
                new DashboardDtos.OperationsStats(
                        roomsMissingLeader,
                        contractsExpiringSoon,
                        contractsOverdueMoveout,
                        ticketsByStatus);

        return new DashboardDtos.StaffDashboardResponse(
                effectiveUnitId,
                unitName,
                capacity,
                finance,
                operations);
    }
}
