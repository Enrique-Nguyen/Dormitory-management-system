package vn.edu.tlu.dms.contract.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.PlacementSource;
import vn.edu.tlu.dms.common.constants.DmsConstants.StayRequestStatus;
import vn.edu.tlu.dms.common.service.PlacementContextLookup;

import java.util.Optional;

/**
 * Ngữ cảnh chọn giường của yêu cầu chuyển phòng: hợp đồng và sinh viên; đơn vị lấy theo giường
 * hiện tại (BR-05). Chỉ yêu cầu PENDING được chọn giường.
 */
@Service
@Transactional(readOnly = true)
public class TransferPlacementLookupImpl implements PlacementContextLookup {

    private final JdbcTemplate jdbc;

    public TransferPlacementLookupImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String source() {
        return PlacementSource.TRANSFER_REQUEST;
    }

    @Override
    public Optional<PlacementContext> find(Long id) {
        return jdbc
                .query(
                        "SELECT t.contract_id, c.student_id, t.status FROM room_transfer_request t"
                                + " JOIN dormitory_contract c ON c.id = t.contract_id WHERE t.id=?",
                        (rs, i) ->
                                new PlacementContext(
                                        rs.getLong("student_id"),
                                        rs.getLong("contract_id"),
                                        null,
                                        null,
                                        StayRequestStatus.PENDING.equals(rs.getString("status"))),
                        id)
                .stream()
                .findFirst();
    }
}
