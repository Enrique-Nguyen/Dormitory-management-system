package vn.edu.tlu.dms.room.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import vn.edu.tlu.dms.common.service.ResidencyLookup;

import java.util.Optional;

@Service
public class ResidencyLookupImpl implements ResidencyLookup {

    private final JdbcTemplate jdbc;

    public ResidencyLookupImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Residency> current(Long studentId) {
        return jdbc
                .query(
                        """
                        SELECT s.room_id, s.building_id, s.management_unit_id, s.contract_status = 'ACTIVE' AS active,
                               (r.leader_student_id = s.student_id AND s.assignment_kind = 'STAY') AS leader
                        FROM v_current_stay s JOIN room r ON r.id = s.room_id WHERE s.student_id = ?
                        """,
                        (r, i) ->
                                new Residency(
                                        r.getLong("room_id"),
                                        r.getLong("building_id"),
                                        r.getLong("management_unit_id"),
                                        r.getBoolean("active"),
                                        r.getBoolean("leader")),
                        studentId)
                .stream()
                .findFirst();
    }
}
