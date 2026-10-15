package vn.edu.tlu.dms.maintenance.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import vn.edu.tlu.dms.maintenance.domain.MaintenanceRequest;

import java.util.List;

public interface MaintenanceRequestRepository
        extends JpaRepository<MaintenanceRequest, Long>,
                JpaSpecificationExecutor<MaintenanceRequest> {

    /** Phạm vi nằm trong SQL, được lọc trước phân trang và đếm tổng. */
    @Query(
            value =
                    """
                    SELECT m.* FROM maintenance_request m LEFT JOIN room r ON r.id=m.room_id
                    LEFT JOIN building rb ON rb.id=r.building_id LEFT JOIN building cb ON cb.id=m.building_id
                    WHERE (:role='ADMIN' OR (:role='STUDENT' AND (m.requester_id=:userId OR m.room_id=:stayingRoomId))
                    OR (:role='TECHNICIAN' AND m.assigned_to=:userId)
                    OR (:role='STAFF' AND (:crossUnit=true OR coalesce(rb.management_unit_id,cb.management_unit_id)=:unitId)))
                    AND (:allStatuses = true OR m.status IN (:statuses)) AND (:category IS NULL OR m.category=:category)
                    AND (:severity IS NULL OR m.severity=:severity)
                    AND (m.code ILIKE :q OR m.description ILIKE :q)
                    """,
            countQuery =
                    """
                    SELECT count(*) FROM maintenance_request m LEFT JOIN room r ON r.id=m.room_id
                    LEFT JOIN building rb ON rb.id=r.building_id LEFT JOIN building cb ON cb.id=m.building_id
                    WHERE (:role='ADMIN' OR (:role='STUDENT' AND (m.requester_id=:userId OR m.room_id=:stayingRoomId))
                    OR (:role='TECHNICIAN' AND m.assigned_to=:userId)
                    OR (:role='STAFF' AND (:crossUnit=true OR coalesce(rb.management_unit_id,cb.management_unit_id)=:unitId)))
                    AND (:allStatuses = true OR m.status IN (:statuses)) AND (:category IS NULL OR m.category=:category)
                    AND (:severity IS NULL OR m.severity=:severity)
                    AND (m.code ILIKE :q OR m.description ILIKE :q)
                    """,
            nativeQuery = true)
    Page<MaintenanceRequest> searchVisibleRequests(
            String role,
            Long userId,
            Long unitId,
            boolean crossUnit,
            Long stayingRoomId,
            boolean allStatuses,
            List<String> statuses,
            String category,
            String severity,
            String q,
            Pageable pageable);
}
