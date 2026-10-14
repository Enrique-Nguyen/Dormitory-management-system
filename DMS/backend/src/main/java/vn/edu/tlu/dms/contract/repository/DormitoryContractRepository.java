package vn.edu.tlu.dms.contract.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import vn.edu.tlu.dms.contract.domain.DormitoryContract;

import java.util.Optional;

public interface DormitoryContractRepository
        extends JpaRepository<DormitoryContract, Long>,
                JpaSpecificationExecutor<DormitoryContract> {

    /** Lọc đơn vị theo lịch sử phân phòng để giữ khả năng tra cứu hợp đồng đã kết thúc. */
    @Query(
            value =
                    """
                    SELECT c.* FROM dormitory_contract c
                    WHERE (:role = 'ADMIN' OR (:role = 'STUDENT' AND c.student_id = :userId)
                        OR (:role = 'STAFF' AND (:crossUnit = true OR EXISTS (
                            SELECT 1 FROM room_assignment_history a
                            JOIN bed b ON b.id = a.bed_id
                            JOIN room r ON r.id = b.room_id
                            JOIN building bd ON bd.id = r.building_id
                            WHERE a.contract_id = c.id AND bd.management_unit_id = :unitId) OR (NOT EXISTS(SELECT 1 FROM room_assignment_history h WHERE h.contract_id=c.id) AND EXISTS(SELECT 1 FROM room_application ap WHERE ap.id=c.application_id AND ap.preferred_unit_id=:unitId)))))
                    AND (:mine = false OR c.student_id = :userId)
                    AND (:status IS NULL OR c.status = :status)
                    AND (c.code ILIKE :q OR EXISTS(SELECT 1 FROM app_user u WHERE u.id=c.student_id AND u.full_name ILIKE :q))
                    ORDER BY c.created_at DESC, c.id DESC
                    """,
            countQuery =
                    """
                    SELECT count(*) FROM dormitory_contract c
                    WHERE (:role = 'ADMIN' OR (:role = 'STUDENT' AND c.student_id = :userId)
                        OR (:role = 'STAFF' AND (:crossUnit = true OR EXISTS (
                            SELECT 1 FROM room_assignment_history a
                            JOIN bed b ON b.id = a.bed_id
                            JOIN room r ON r.id = b.room_id
                            JOIN building bd ON bd.id = r.building_id
                            WHERE a.contract_id = c.id AND bd.management_unit_id = :unitId) OR (NOT EXISTS(SELECT 1 FROM room_assignment_history h WHERE h.contract_id=c.id) AND EXISTS(SELECT 1 FROM room_application ap WHERE ap.id=c.application_id AND ap.preferred_unit_id=:unitId)))))
                    AND (:mine = false OR c.student_id = :userId)
                    AND (:status IS NULL OR c.status = :status)
                    AND (c.code ILIKE :q OR EXISTS(SELECT 1 FROM app_user u WHERE u.id=c.student_id AND u.full_name ILIKE :q))
                    """,
            nativeQuery = true)
    Page<DormitoryContract> searchAccessibleContracts(
            String role,
            Long userId,
            Long unitId,
            boolean crossUnit,
            boolean mine,
            String status,
            String q,
            Pageable pageable);

    @Query(
            value =
                    """
                    SELECT c.* FROM dormitory_contract c
                    WHERE (:role = 'ADMIN' OR (:role = 'STUDENT' AND c.student_id = :userId)
                        OR (:role = 'STAFF' AND (:crossUnit = true OR EXISTS (
                            SELECT 1 FROM room_assignment_history a
                            JOIN bed b ON b.id = a.bed_id
                            JOIN room r ON r.id = b.room_id
                            JOIN building bd ON bd.id = r.building_id
                            WHERE a.contract_id = c.id AND bd.management_unit_id = :unitId) OR (NOT EXISTS(SELECT 1 FROM room_assignment_history h WHERE h.contract_id=c.id) AND EXISTS(SELECT 1 FROM room_application ap WHERE ap.id=c.application_id AND ap.preferred_unit_id=:unitId)))))
                    AND c.id = :id
                    """,
            nativeQuery = true)
    Optional<DormitoryContract> findAccessibleContract(
            Long id, String role, Long userId, Long unitId, boolean crossUnit);
}
