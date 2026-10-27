package vn.edu.tlu.dms.room.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentKind;
import vn.edu.tlu.dms.room.service.OccupancyQuery;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class OccupancyQueryImpl implements OccupancyQuery {

    private final JdbcTemplate jdbc;

    public OccupancyQueryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<CurrentBed> findOpenAssignmentByContract(Long contractId) {
        return jdbc
                .query(
                        """
                        SELECT a.id, a.student_id, a.contract_id, a.kind, b.id AS bed_id,
                               b.code AS bed_code, r.id AS room_id, r.code AS room_code,
                               bd.id AS building_id, bd.code AS building_code,
                               bd.management_unit_id
                        FROM room_assignment_history a
                        JOIN bed b ON b.id = a.bed_id
                        JOIN room r ON r.id = b.room_id
                        JOIN building bd ON bd.id = r.building_id
                        WHERE a.contract_id = ? AND a.ended_at IS NULL
                        """,
                        (rs, i) ->
                                new CurrentBed(
                                        rs.getLong("id"),
                                        rs.getLong("student_id"),
                                        rs.getLong("contract_id"),
                                        rs.getString("kind"),
                                        rs.getLong("bed_id"),
                                        rs.getString("bed_code"),
                                        rs.getLong("room_id"),
                                        rs.getString("room_code"),
                                        rs.getLong("building_id"),
                                        rs.getString("building_code"),
                                        rs.getLong("management_unit_id")),
                        contractId)
                .stream()
                .findFirst();
    }

    @Override
    public List<Occupant> findOccupantsAt(Long roomId, OffsetDateTime at) {
        return jdbc.query(
                """
                SELECT DISTINCT a.student_id, a.contract_id
                FROM room_assignment_history a
                JOIN bed b ON b.id = a.bed_id
                WHERE b.room_id = ? AND a.kind = ?
                  AND a.stay_started_at <= ?
                  AND (a.ended_at IS NULL OR a.ended_at > ?)
                ORDER BY a.contract_id
                """,
                (rs, i) -> new Occupant(rs.getLong("student_id"), rs.getLong("contract_id")),
                roomId,
                AssignmentKind.STAY,
                at,
                at);
    }

    @Override
    public boolean isRoomLeader(Long roomId, Long studentId) {
        Boolean leader =
                jdbc.queryForObject(
                        "SELECT EXISTS (SELECT 1 FROM room WHERE id=? AND leader_student_id=?)",
                        Boolean.class,
                        roomId,
                        studentId);
        return Boolean.TRUE.equals(leader);
    }

    @Override
    public List<AssignmentRow> findAssignmentsByContract(Long contractId) {
        if (contractId == null) {
            return List.of();
        }
        return jdbc.query(
                """
                SELECT a.id, a.kind, b.id AS bed_id, b.code AS bed_code,
                       r.id AS room_id, r.code AS room_code, r.floor,
                       bd.id AS building_id, bd.code AS building_code,
                       bd.management_unit_id,
                       a.started_at, a.stay_started_at, a.ended_at, a.end_reason,
                       a.price_main, a.price_summer
                FROM room_assignment_history a
                JOIN bed b ON b.id = a.bed_id
                JOIN room r ON r.id = b.room_id
                JOIN building bd ON bd.id = r.building_id
                WHERE a.contract_id = ?
                ORDER BY a.started_at DESC, a.id DESC
                """,
                (rs, i) ->
                        new AssignmentRow(
                                rs.getLong("id"),
                                rs.getString("kind"),
                                rs.getLong("bed_id"),
                                rs.getString("bed_code"),
                                rs.getLong("room_id"),
                                rs.getString("room_code"),
                                rs.getLong("building_id"),
                                rs.getString("building_code"),
                                rs.getObject("floor", Integer.class),
                                rs.getObject("management_unit_id", Long.class),
                                rs.getObject("started_at", OffsetDateTime.class),
                                rs.getObject("stay_started_at", OffsetDateTime.class),
                                rs.getObject("ended_at", OffsetDateTime.class),
                                rs.getString("end_reason"),
                                rs.getObject("price_main", Long.class),
                                rs.getObject("price_summer", Long.class)),
                contractId);
    }

    @Override
    public Optional<BedLocation> findBedLocation(Long bedId) {
        return jdbc
                .query(
                        """
                        SELECT b.id AS bed_id, b.status AS bed_status,
                               r.id AS room_id, r.room_type_id, r.gender, r.status AS room_status,
                               bd.management_unit_id
                        FROM bed b
                        JOIN room r ON r.id = b.room_id
                        JOIN building bd ON bd.id = r.building_id
                        WHERE b.id = ?
                        """,
                        (rs, i) ->
                                new BedLocation(
                                        rs.getLong("bed_id"),
                                        rs.getLong("room_id"),
                                        rs.getLong("room_type_id"),
                                        rs.getLong("management_unit_id"),
                                        rs.getString("gender"),
                                        rs.getString("room_status"),
                                        rs.getString("bed_status")),
                        bedId)
                .stream()
                .findFirst();
    }
}
