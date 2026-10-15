package vn.edu.tlu.dms.billing.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import vn.edu.tlu.dms.billing.domain.Invoice;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository
        extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    /** Hóa đơn điện nước chỉ hiện cho sinh viên có khoảng ở giao với tháng lập hóa đơn. */
    @Query(
            value =
                    """
                    SELECT i.* FROM invoice i
                    WHERE (:role = 'ADMIN' OR (:role = 'STUDENT' AND (
                        (i.type = 'ROOM_FEE' AND i.student_id = :userId)
                        OR (i.type = 'UTILITY' AND EXISTS (
                            SELECT 1 FROM room_assignment_history a JOIN bed b ON b.id = a.bed_id
                            WHERE a.student_id = :userId AND b.room_id = i.room_id AND a.kind = 'STAY'
                                AND a.stay_started_at < i.billing_month + INTERVAL '1 month'
                                AND (a.ended_at IS NULL OR a.ended_at > i.billing_month)))))
                        OR (:role = 'STAFF' AND (:crossUnit = true
                            OR EXISTS (SELECT 1 FROM room r JOIN building bd ON bd.id = r.building_id
                                WHERE r.id = i.room_id AND bd.management_unit_id = :unitId)
                            OR EXISTS (SELECT 1 FROM room_assignment_history a JOIN bed b ON b.id = a.bed_id
                                JOIN room r ON r.id = b.room_id JOIN building bd ON bd.id = r.building_id
                                WHERE a.student_id = i.student_id AND bd.management_unit_id = :unitId) OR EXISTS(SELECT 1 FROM room_application ap WHERE ap.student_id=i.student_id AND ap.preferred_unit_id=:unitId))))
                    AND (:mine = false OR (i.type = 'ROOM_FEE' AND i.student_id = :userId)
                        OR (i.type = 'UTILITY' AND EXISTS (
                            SELECT 1 FROM room_assignment_history own_stay JOIN bed own_bed ON own_bed.id = own_stay.bed_id
                            WHERE own_stay.student_id = :userId AND own_bed.room_id = i.room_id AND own_stay.kind = 'STAY'
                                AND own_stay.stay_started_at < i.billing_month + INTERVAL '1 month'
                                AND (own_stay.ended_at IS NULL OR own_stay.ended_at > i.billing_month))))
                    AND (:type IS NULL OR i.type = :type)
                    AND (:filterStatuses = false OR i.status IN (:statuses))
                    AND (i.code ILIKE :q OR EXISTS(SELECT 1 FROM app_user u WHERE u.id=i.student_id AND u.full_name ILIKE :q))
                    """,
            countQuery =
                    """
                    SELECT count(*) FROM invoice i
                    WHERE (:role = 'ADMIN' OR (:role = 'STUDENT' AND (
                        (i.type = 'ROOM_FEE' AND i.student_id = :userId)
                        OR (i.type = 'UTILITY' AND EXISTS (
                            SELECT 1 FROM room_assignment_history a JOIN bed b ON b.id = a.bed_id
                            WHERE a.student_id = :userId AND b.room_id = i.room_id AND a.kind = 'STAY'
                                AND a.stay_started_at < i.billing_month + INTERVAL '1 month'
                                AND (a.ended_at IS NULL OR a.ended_at > i.billing_month)))))
                        OR (:role = 'STAFF' AND (:crossUnit = true
                            OR EXISTS (SELECT 1 FROM room r JOIN building bd ON bd.id = r.building_id
                                WHERE r.id = i.room_id AND bd.management_unit_id = :unitId)
                            OR EXISTS (SELECT 1 FROM room_assignment_history a JOIN bed b ON b.id = a.bed_id
                                JOIN room r ON r.id = b.room_id JOIN building bd ON bd.id = r.building_id
                                WHERE a.student_id = i.student_id AND bd.management_unit_id = :unitId) OR EXISTS(SELECT 1 FROM room_application ap WHERE ap.student_id=i.student_id AND ap.preferred_unit_id=:unitId))))
                    AND (:mine = false OR (i.type = 'ROOM_FEE' AND i.student_id = :userId)
                        OR (i.type = 'UTILITY' AND EXISTS (
                            SELECT 1 FROM room_assignment_history own_stay JOIN bed own_bed ON own_bed.id = own_stay.bed_id
                            WHERE own_stay.student_id = :userId AND own_bed.room_id = i.room_id AND own_stay.kind = 'STAY'
                                AND own_stay.stay_started_at < i.billing_month + INTERVAL '1 month'
                                AND (own_stay.ended_at IS NULL OR own_stay.ended_at > i.billing_month))))
                    AND (:type IS NULL OR i.type = :type)
                    AND (:filterStatuses = false OR i.status IN (:statuses))
                    AND (i.code ILIKE :q OR EXISTS(SELECT 1 FROM app_user u WHERE u.id=i.student_id AND u.full_name ILIKE :q))
                    """,
            nativeQuery = true)
    Page<Invoice> searchAccessibleInvoices(
            String role,
            Long userId,
            Long unitId,
            boolean crossUnit,
            boolean mine,
            String type,
            boolean filterStatuses,
            List<String> statuses,
            String q,
            Pageable pageable);

    @Query(
            value =
                    """
                    SELECT i.* FROM invoice i
                    WHERE (:role = 'ADMIN' OR (:role = 'STUDENT' AND (
                        (i.type = 'ROOM_FEE' AND i.student_id = :userId)
                        OR (i.type = 'UTILITY' AND EXISTS (
                            SELECT 1 FROM room_assignment_history a JOIN bed b ON b.id = a.bed_id
                            WHERE a.student_id = :userId AND b.room_id = i.room_id AND a.kind = 'STAY'
                                AND a.stay_started_at < i.billing_month + INTERVAL '1 month'
                                AND (a.ended_at IS NULL OR a.ended_at > i.billing_month)))))
                        OR (:role = 'STAFF' AND (:crossUnit = true
                            OR EXISTS (SELECT 1 FROM room r JOIN building bd ON bd.id = r.building_id
                                WHERE r.id = i.room_id AND bd.management_unit_id = :unitId)
                            OR EXISTS (SELECT 1 FROM room_assignment_history a JOIN bed b ON b.id = a.bed_id
                                JOIN room r ON r.id = b.room_id JOIN building bd ON bd.id = r.building_id
                                WHERE a.student_id = i.student_id AND bd.management_unit_id = :unitId) OR EXISTS(SELECT 1 FROM room_application ap WHERE ap.student_id=i.student_id AND ap.preferred_unit_id=:unitId))))
                    AND i.id = :id
                    """,
            nativeQuery = true)
    Optional<Invoice> findAccessibleInvoice(
            Long id, String role, Long userId, Long unitId, boolean crossUnit);
}
