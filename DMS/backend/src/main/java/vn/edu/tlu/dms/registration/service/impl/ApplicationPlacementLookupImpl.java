package vn.edu.tlu.dms.registration.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.ApplicationStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.PlacementSource;
import vn.edu.tlu.dms.common.service.PlacementContextLookup;

import java.util.Optional;

/** Ngữ cảnh chọn giường của đơn đăng ký: đơn vị và loại phòng nguyện vọng; chỉ đơn APPROVED. */
@Service
@Transactional(readOnly = true)
public class ApplicationPlacementLookupImpl implements PlacementContextLookup {

    private final JdbcTemplate jdbc;

    public ApplicationPlacementLookupImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String source() {
        return PlacementSource.APPLICATION;
    }

    @Override
    public Optional<PlacementContext> find(Long id) {
        return jdbc
                .query(
                        "SELECT student_id, preferred_unit_id, preferred_room_type_id, status"
                                + " FROM room_application WHERE id=?",
                        (rs, i) ->
                                new PlacementContext(
                                        rs.getLong("student_id"),
                                        null,
                                        rs.getLong("preferred_unit_id"),
                                        rs.getObject("preferred_room_type_id", Long.class),
                                        ApplicationStatus.APPROVED.equals(rs.getString("status"))),
                        id)
                .stream()
                .findFirst();
    }
}
