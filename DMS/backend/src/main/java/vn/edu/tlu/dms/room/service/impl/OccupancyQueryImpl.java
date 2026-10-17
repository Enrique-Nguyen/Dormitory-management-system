package vn.edu.tlu.dms.room.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.room.service.OccupancyQuery;

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
}
