package vn.edu.tlu.dms.billing.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import vn.edu.tlu.dms.billing.domain.Invoice;

import java.util.Optional;

public interface InvoiceRepository
        extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    /** Hóa đơn điện nước chỉ hiện cho sinh viên có khoảng ở giao với tháng lập hóa đơn. */
    String STUDENT_SCOPE =
            """
            (i.type = 'ROOM_FEE' AND i.student_id = :userId)
            OR (i.type = 'UTILITY' AND EXISTS (
                SELECT 1 FROM room_assignment_history a JOIN bed b ON b.id = a.bed_id
                WHERE a.student_id = :userId AND b.room_id = i.room_id AND a.kind = 'STAY'
                    AND a.stay_started_at < i.billing_month + INTERVAL '1 month'
                    AND (a.ended_at IS NULL OR a.ended_at > i.billing_month)))
            """;

    /**
     * Phạm vi đơn vị của Staff. Điện nước theo đơn vị của phòng. Phí phòng theo đơn vị mà sinh viên
     * ở trong học kỳ của hóa đơn (hoặc đã đăng ký nguyện vọng trong đợt thuộc học kỳ đó), không
     * lấy mọi lịch sử ở từng đơn vị để sinh viên chuyển đơn vị không làm lộ hóa đơn kỳ khác.
     */
    String STAFF_UNIT_SCOPE =
            """
            (i.type = 'UTILITY' AND EXISTS (
                SELECT 1 FROM room r JOIN building bd ON bd.id = r.building_id
                WHERE r.id = i.room_id AND bd.management_unit_id = :unitId))
            OR (i.type = 'ROOM_FEE' AND (
                EXISTS (SELECT 1 FROM room_assignment_history a
                    JOIN bed b ON b.id = a.bed_id JOIN room r ON r.id = b.room_id
                    JOIN building bd ON bd.id = r.building_id
                    JOIN semester s ON s.id = i.semester_id
                    WHERE a.student_id = i.student_id AND bd.management_unit_id = :unitId
                        AND a.started_at < s.end_date + 1
                        AND (a.ended_at IS NULL OR a.ended_at >= s.start_date))
                OR EXISTS (SELECT 1 FROM room_application ap
                    JOIN registration_period rp ON rp.id = ap.period_id
                    WHERE ap.student_id = i.student_id AND ap.preferred_unit_id = :unitId
                        AND rp.semester_id = i.semester_id)))
            """;

    String ACCESS_SCOPE =
            "((:role = 'STUDENT' AND ("
                    + STUDENT_SCOPE
                    + ")) OR (:role = 'STAFF' AND (:crossUnit = true OR "
                    + STAFF_UNIT_SCOPE
                    + ")))";

    String SEARCH_FILTER =
            """
            AND (:type IS NULL OR i.type = :type)
            AND (:status IS NULL OR i.status = :status)
            AND (i.code ILIKE :q OR EXISTS(SELECT 1 FROM app_user u WHERE u.id=i.student_id AND u.full_name ILIKE :q))
            """;

    @Query(
            value =
                    "SELECT i.* FROM invoice i WHERE "
                            + ACCESS_SCOPE
                            + SEARCH_FILTER
                            + " ORDER BY i.issued_at DESC, i.id DESC",
            countQuery = "SELECT count(*) FROM invoice i WHERE " + ACCESS_SCOPE + SEARCH_FILTER,
            nativeQuery = true)
    Page<Invoice> searchAccessibleInvoices(
            String role,
            Long userId,
            Long unitId,
            boolean crossUnit,
            String type,
            String status,
            String q,
            Pageable pageable);

    @Query(
            value = "SELECT i.* FROM invoice i WHERE " + ACCESS_SCOPE + " AND i.id = :id",
            nativeQuery = true)
    Optional<Invoice> findAccessibleInvoice(
            Long id, String role, Long userId, Long unitId, boolean crossUnit);

    /** Quyền ghi (xác nhận thanh toán) của Staff đúng đơn vị, không tính quyền tổng hợp. */
    @Query(
            value =
                    "SELECT EXISTS (SELECT 1 FROM invoice i WHERE i.id = :id AND ("
                            + STAFF_UNIT_SCOPE
                            + "))",
            nativeQuery = true)
    boolean isInStaffUnit(Long id, Long unitId);
}
