package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.SemesterType;
import vn.edu.tlu.dms.common.constants.DmsConstants.SummerChoice;
import vn.edu.tlu.dms.common.constants.DmsConstants.SummerStatus;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.service.RolePermissionService;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.registration.api.PendingItemController.Item;
import vn.edu.tlu.dms.registration.service.PendingItemService;
import vn.edu.tlu.dms.registration.service.SummerCrudService;
import vn.edu.tlu.dms.support.FutureSemesterFixture;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Kiểm thử tích hợp lựa chọn hè trên PostgreSQL thật: STAY gia hạn hợp đồng, TEMP_LEAVE/LEAVE chỉ
 * duyệt, từ chối lưu lý do, chống xử lý lặp và phạm vi đơn vị. Cần Docker đang chạy; nếu môi trường
 * không có Docker thì lớp này không chạy được.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class SummerChoiceIntegrationTest {

    private static final LocalDate CONTRACT_START = LocalDate.of(2098, 9, 1);

    private static final LocalDate CONTRACT_END = LocalDate.of(2098, 12, 31);

    private static final LocalDate SUMMER_END = LocalDate.of(2099, 8, 31);

    @Autowired SummerCrudService summers;

    @Autowired PendingItemService pendingItems;

    @Autowired JdbcTemplate jdbc;

    @MockitoBean CurrentUser currentUser;

    @MockitoBean RolePermissionService rolePermissions;

    private String suffix;

    private Long unitId;

    private Long staffId;

    private Long studentId;

    private Long otherStudentId;

    private Long summerSemesterId;

    private Long contractId;

    private Long choiceId;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        unitId = jdbc.queryForObject("SELECT id FROM management_unit WHERE code = 'K1'", Long.class);
        staffId = insertUser("staff_" + suffix, Role.STAFF, unitId, "Staff Test");
        studentId = createStudent(suffix + "01", "Sinh viên một");
        otherStudentId = createStudent(suffix + "02", "Sinh viên hai");

        Long regularSemesterId =
                jdbc.queryForObject(
                        """
                        INSERT INTO semester(academic_year, type, name, start_date, end_date)
                        VALUES('2098-2099', 'HK1', 'Học kỳ 1', '2098-09-01', '2098-12-31')
                        RETURNING id
                        """,
                        Long.class);
        summerSemesterId =
                jdbc.queryForObject(
                        """
                        INSERT INTO semester(academic_year, type, name, start_date, end_date)
                        VALUES('2098-2099', 'SUMMER', 'Học kỳ hè', '2099-06-01', '2099-08-31')
                        RETURNING id
                        """,
                        Long.class);
        Long periodId =
                jdbc.queryForObject(
                        """
                        INSERT INTO registration_period(semester_id, kind, name, open_at, close_at,
                            created_by)
                        VALUES(?, 'REGULAR', ?, now(), now() + interval '30 days', ?)
                        RETURNING id
                        """,
                        Long.class,
                        regularSemesterId,
                        "Period " + suffix,
                        staffId);
        contractId = createContract(studentId, periodId);
        choiceId = createChoice(contractId, summerSemesterId, SummerChoice.STAY);

        // Quyền đang cấp được giả lập đầy đủ để kiểm tra nghiệp vụ; ma trận thật do module account
        // quản lý. Cấu hình mặc định hiện chưa cấp SUMMER_REGISTRATION_UPDATE cho STAFF.
        when(rolePermissions.getGrantedPermissions(anyString()))
                .thenReturn(List.copyOf(Permission.ALL));
        asStaff(unitId);
    }

    @Test
    void approveStayExtendsContractAndRecordsSummerExtension() {
        Map<String, Object> detail = detail(summers.approve(choiceId));

        assertThat(detail.get("status")).isEqualTo(SummerStatus.APPROVED);
        assertThat(detail.get("allowedActions")).isEqualTo(List.of());
        assertThat((List<?>) detail.get("timeline")).isNotEmpty();

        Map<String, Object> choice = choiceRow(choiceId);
        assertThat(choice.get("status")).isEqualTo(SummerStatus.APPROVED);
        assertThat(choice.get("decided_by")).isEqualTo(staffId);
        assertThat(choice.get("decided_at")).isNotNull();

        assertThat(contractEnd(contractId)).isEqualTo(SUMMER_END);

        Map<String, Object> extension = extensionRow(contractId);
        assertThat(extension.get("source")).isEqualTo("SUMMER_STAY");
        assertThat(extension.get("status")).isEqualTo(SummerStatus.APPROVED);
        assertThat(extension.get("old_end_date").toString()).isEqualTo(CONTRACT_END.toString());
        assertThat(extension.get("new_end_date").toString()).isEqualTo(SUMMER_END.toString());
        assertThat(extension.get("decided_by")).isEqualTo(staffId);

        assertThat(
                        count(
                                """
                                SELECT COUNT(*) FROM status_history
                                WHERE entity_type = ? AND entity_id = ? AND from_status = ?
                                  AND to_status = ?
                                """,
                                EntityType.SUMMER_CHOICE,
                                choiceId,
                                SummerStatus.PENDING,
                                SummerStatus.APPROVED))
                .isEqualTo(1L);
        assertThat(
                        count(
                                """
                                SELECT COUNT(*) FROM audit_log
                                WHERE action = ? AND entity_type = ? AND entity_id = ?
                                """,
                                AuditAction.SUMMER_CHOICE_APPROVE,
                                EntityType.SUMMER_CHOICE,
                                choiceId))
                .isEqualTo(1L);
    }

    @Test
    void approveTempLeaveDoesNotChangeContractOrBed() {
        Long tempLeaveId = createChoice(contractId, newSummerSemester(), SummerChoice.TEMP_LEAVE);

        Map<String, Object> detail = detail(summers.approve(tempLeaveId));

        assertThat(detail.get("status")).isEqualTo(SummerStatus.APPROVED);
        assertThat(contractEnd(contractId)).isEqualTo(CONTRACT_END);
        assertThat(extensionCount(contractId)).isZero();
        assertThat(choiceRow(tempLeaveId).get("status")).isEqualTo(SummerStatus.APPROVED);
    }

    @Test
    void approveLeaveDoesNotChangeContractOrBed() {
        Long leaveId = createChoice(contractId, newSummerSemester(), SummerChoice.LEAVE);

        Map<String, Object> detail = detail(summers.approve(leaveId));

        assertThat(detail.get("status")).isEqualTo(SummerStatus.APPROVED);
        assertThat(contractEnd(contractId)).isEqualTo(CONTRACT_END);
        assertThat(extensionCount(contractId)).isZero();
        assertThat(choiceRow(leaveId).get("status")).isEqualTo(SummerStatus.APPROVED);
    }

    @Test
    void rejectKeepsContractAndStoresReasonInHistoryAndAudit() {
        String reason = "Chưa đủ điều kiện lưu trú hè";

        Map<String, Object> detail = detail(summers.reject(choiceId, reason));

        assertThat(detail.get("status")).isEqualTo(SummerStatus.REJECTED);
        assertThat(contractEnd(contractId)).isEqualTo(CONTRACT_END);
        assertThat(extensionCount(contractId)).isZero();
        assertThat(choiceRow(choiceId).get("status")).isEqualTo(SummerStatus.REJECTED);
        assertThat(
                        jdbc.queryForObject(
                                """
                                SELECT reason FROM status_history
                                WHERE entity_type = ? AND entity_id = ? AND to_status = ?
                                """,
                                String.class,
                                EntityType.SUMMER_CHOICE,
                                choiceId,
                                SummerStatus.REJECTED))
                .isEqualTo(reason);
        assertThat(
                        jdbc.queryForObject(
                                """
                                SELECT reason FROM audit_log
                                WHERE action = ? AND entity_type = ? AND entity_id = ?
                                """,
                                String.class,
                                AuditAction.SUMMER_CHOICE_REJECT,
                                EntityType.SUMMER_CHOICE,
                                choiceId))
                .isEqualTo(reason);
    }

    @Test
    void rejectRequiresReason() {
        assertThatThrownBy(() -> summers.reject(choiceId, "ngắn"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.VALIDATION_FAILED));
        assertThat(choiceRow(choiceId).get("status")).isEqualTo(SummerStatus.PENDING);
    }

    @Test
    void choiceCannotBeApprovedTwice() {
        summers.approve(choiceId);

        assertThatThrownBy(() -> summers.approve(choiceId))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
        assertThat(extensionCount(contractId)).isEqualTo(1);
    }

    @Test
    void staffOutsideUnitCannotApproveOrRead() {
        asStaff(unitId + 1_000L);

        assertThatThrownBy(() -> summers.approve(choiceId))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.NOT_FOUND));
        assertThatThrownBy(() -> summers.getChoice(choiceId))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.NOT_FOUND));
        assertThat(choiceRow(choiceId).get("status")).isEqualTo(SummerStatus.PENDING);
    }

    @Test
    void studentCannotApproveOwnChoice() {
        when(currentUser.get()).thenReturn(new Actor(studentId, Role.STUDENT, null, false, false));

        assertThatThrownBy(() -> summers.approve(choiceId))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void detailExposesActionsByRoleAndStatus() {
        Map<String, Object> staffDetail = detail(summers.getChoice(choiceId));
        assertThat(actions(staffDetail))
                .containsExactly(BusinessAction.APPROVE, BusinessAction.REJECT);

        when(currentUser.get()).thenReturn(new Actor(studentId, Role.STUDENT, null, false, false));
        Map<String, Object> studentDetail = detail(summers.getChoice(choiceId));
        assertThat(actions(studentDetail))
                .containsExactly(BusinessAction.EDIT, BusinessAction.CANCEL);
    }

    @Test
    void myCurrentChoiceReturnsLatestOfStudent() {
        Long latestId = createChoice(contractId, newSummerSemester(), SummerChoice.TEMP_LEAVE);
        when(currentUser.get()).thenReturn(new Actor(studentId, Role.STUDENT, null, false, false));

        Map<String, Object> detail = detail(summers.getMyCurrentChoice());

        assertThat(detail.get("id")).isEqualTo(latestId);
    }

    @Test
    void myCurrentChoiceIsNullWhenStudentHasNoChoice() {
        when(currentUser.get())
                .thenReturn(new Actor(otherStudentId, Role.STUDENT, null, false, false));

        assertThat(summers.getMyCurrentChoice()).isNull();
    }

    @Test
    void pendingItemsIncludeSummerChoiceInStaffScopeOnly() {
        var page = pendingItems.searchByPage(new SearchRequest());

        assertThat(page.content()).extracting(Item::type).contains("SUMMER_CHOICE");
        assertThat(page.content())
                .filteredOn(item -> "SUMMER_CHOICE".equals(item.type()))
                .extracting(Item::entityId)
                .contains(choiceId);

        asStaff(unitId + 1_000L);
        assertThat(pendingItems.searchByPage(new SearchRequest()).content()).isEmpty();
    }

    private void asStaff(Long unit) {
        when(currentUser.get()).thenReturn(new Actor(staffId, Role.STAFF, unit, false, false));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> detail(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<String> actions(Map<String, Object> detail) {
        return (List<String>) detail.get("allowedActions");
    }

    private Long createStudent(String studentCode, String fullName) {
        Long id = insertUser(studentCode, Role.STUDENT, null, fullName);
        jdbc.update(
                "INSERT INTO student_profile(user_id, student_code, gender) VALUES(?, ?, 'M')",
                id,
                studentCode);
        return id;
    }

    private Long createContract(Long student, Long periodId) {
        Long applicationId =
                jdbc.queryForObject(
                        """
                        INSERT INTO room_application(student_id, period_id, preferred_unit_id, status,
                            submitted_at)
                        VALUES(?, ?, ?, 'APPROVED', now())
                        RETURNING id
                        """,
                        Long.class,
                        student,
                        periodId,
                        unitId);
        return jdbc.queryForObject(
                """
                INSERT INTO dormitory_contract(code, student_id, application_id, start_date, end_date,
                    activation_deadline, deposit_required, activated_at, status)
                VALUES(?, ?, ?, ?, ?, now() + interval '7 days', 1000000, now(), ?)
                RETURNING id
                """,
                Long.class,
                "HD-" + suffix,
                student,
                applicationId,
                CONTRACT_START,
                CONTRACT_END,
                ContractStatus.ACTIVE);
    }

    /**
     * Học kỳ hè riêng cho lựa chọn phát sinh thêm trong test: {@code summer_choice} chặn trùng
     * {@code UNIQUE(contract_id, semester_id)} nên một hợp đồng chỉ có một lựa chọn cho mỗi học kỳ.
     * Học kỳ cấp phát động cũng không chồng daterange với học kỳ 2098 của fixture hay dữ liệu mẫu.
     */
    private Long newSummerSemester() {
        return FutureSemesterFixture.insert(jdbc, SemesterType.SUMMER, "Học kỳ hè " + suffix);
    }

    private Long createChoice(Long contract, Long semesterId, String choice) {
        return jdbc.queryForObject(
                """
                INSERT INTO summer_choice(contract_id, semester_id, choice, status)
                VALUES(?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                contract,
                semesterId,
                choice,
                SummerStatus.PENDING);
    }

    private Map<String, Object> choiceRow(Long id) {
        return jdbc.queryForMap(
                "SELECT status, decided_by, decided_at FROM summer_choice WHERE id = ?", id);
    }

    private LocalDate contractEnd(Long id) {
        return jdbc.queryForObject(
                "SELECT end_date FROM dormitory_contract WHERE id = ?", LocalDate.class, id);
    }

    private Long extensionCount(Long contract) {
        return count(
                "SELECT COUNT(*) FROM contract_extension_request WHERE contract_id = ?", contract);
    }

    private Map<String, Object> extensionRow(Long contract) {
        return jdbc.queryForMap(
                """
                SELECT source, status, old_end_date, new_end_date, decided_by
                FROM contract_extension_request WHERE contract_id = ?
                """,
                contract);
    }

    private Long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private Long insertUser(String username, String role, Long unit, String fullName) {
        return jdbc.queryForObject(
                """
                INSERT INTO app_user(username, role, management_unit_id, full_name, password_hash)
                VALUES(?, ?, ?, ?, 'hash')
                RETURNING id
                """,
                Long.class,
                username,
                role,
                unit,
                fullName);
    }
}
