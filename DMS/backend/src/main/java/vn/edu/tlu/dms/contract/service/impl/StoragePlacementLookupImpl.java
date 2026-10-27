package vn.edu.tlu.dms.contract.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.PlacementSource;
import vn.edu.tlu.dms.common.service.PlacementContextLookup;
import vn.edu.tlu.dms.room.service.OccupancyQuery;
import vn.edu.tlu.dms.room.service.OccupancyQuery.BedLocation;

import java.util.Optional;

/**
 * Ngữ cảnh chọn giường khi sinh viên tạm rời hè nhận lại đồ: lấy sinh viên/hợp đồng từ phiếu gửi
 * đồ và đơn vị/loại phòng từ giường cũ. Chỉ phiếu chưa nhận lại của hợp đồng ACTIVE mới sẵn sàng.
 */
@Service
@Transactional(readOnly = true)
public class StoragePlacementLookupImpl implements PlacementContextLookup {

    private final JdbcTemplate jdbc;

    private final OccupancyQuery occupancyQuery;

    public StoragePlacementLookupImpl(JdbcTemplate jdbc, OccupancyQuery occupancyQuery) {
        this.jdbc = jdbc;
        this.occupancyQuery = occupancyQuery;
    }

    @Override
    public String source() {
        return PlacementSource.STORAGE_RECORD;
    }

    @Override
    public Optional<PlacementContext> find(Long id) {
        return jdbc.query(
                        """
                        SELECT s.contract_id, c.student_id, c.status AS contract_status,
                               s.previous_bed_id, s.returned_at
                        FROM storage_record s
                        JOIN dormitory_contract c ON c.id = s.contract_id
                        WHERE s.id = ?
                        """,
                        (rs, i) -> {
                            Long bedId = rs.getObject("previous_bed_id", Long.class);
                            Optional<BedLocation> location =
                                    bedId == null
                                            ? Optional.empty()
                                            : occupancyQuery.findBedLocation(bedId);
                            if (location.isEmpty()) {
                                return null;
                            }
                            BedLocation bed = location.get();
                            boolean ready =
                                    rs.getObject("returned_at") == null
                                            && ContractStatus.ACTIVE.equals(
                                                    rs.getString("contract_status"));
                            return new PlacementContext(
                                    rs.getLong("student_id"),
                                    rs.getLong("contract_id"),
                                    bed.managementUnitId(),
                                    bed.roomTypeId(),
                                    ready);
                        },
                        id)
                .stream()
                .filter(java.util.Objects::nonNull)
                .findFirst();
    }
}
