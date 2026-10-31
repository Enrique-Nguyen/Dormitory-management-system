package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.billing.api.InvoiceDtos;
import vn.edu.tlu.dms.billing.service.InvoiceCrudService;
import vn.edu.tlu.dms.billing.service.InvoiceService;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentEndReason;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.InvoiceStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.InvoiceType;
import vn.edu.tlu.dms.common.constants.DmsConstants.PaymentMethod;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.RegistrationKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.SemesterType;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.service.RolePermissionService;
import vn.edu.tlu.dms.support.FutureSemesterFixture;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Kiểm tra quyền ghi hóa đơn theo học kỳ; dùng PostgreSQL test, không sửa dữ liệu dev. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class InvoiceCrudScopeIntegrationTest {

    @Autowired InvoiceCrudService crud;
    @Autowired InvoiceService invoices;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @MockitoBean CurrentUser currentUser;
    @MockitoBean RolePermissionService rolePermissions;

    private String suffix;
    private Long oldUnit;
    private Long targetUnit;
    private Long staffId;
    private Long studentId;
    private Long oldApplication;
    private Long contractId;
    private FutureSemesterFixture.Created oldSemester;
    private FutureSemesterFixture.Created targetSemester;
    private final LocalDate dueDate = LocalDate.now().plusDays(30);

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        oldUnit = insertUnit("K");
        targetUnit = insertUnit("N");
        staffId = insertUser("staff", Role.STAFF, oldUnit);
        studentId = insertUser("student", Role.STUDENT, null);
        jdbc.update(
                "INSERT INTO student_profile(user_id, student_code, gender) VALUES(?, ?, 'M')",
                studentId,
                suffix);
        oldSemester = FutureSemesterFixture.create(jdbc, SemesterType.HK1, "Old " + suffix, 30);
        targetSemester = FutureSemesterFixture.create(jdbc, SemesterType.HK2, "Next " + suffix, 30);
        assertThat(oldSemester.endDate()).isBefore(targetSemester.startDate());
        oldApplication = insertApplication(oldSemester.id(), oldUnit);
        insertApplication(targetSemester.id(), targetUnit);
        when(rolePermissions.getGrantedPermissions(anyString()))
                .thenReturn(List.of(
                        Permission.INVOICE_READ,
                        Permission.INVOICE_CREATE,
                        Permission.INVOICE_UPDATE,
                        Permission.INVOICE_DELETE));
        asStaff(oldUnit, false);
        // A prior stay must not grant write access to a later semester either.
        insertAssignment(oldSemester);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void oldUnitCannotCreateNextSemesterInvoiceEvenWithAggregateRead(boolean crossUnit) {
        asStaff(oldUnit, crossUnit);
        long beforeAudit = auditCount();

        assertStatus(() -> crud.saveOrUpdate(createInput()), HttpStatus.NOT_FOUND);

        assertThat(invoiceCount()).isZero();
        assertThat(auditCount()).isEqualTo(beforeAudit);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void oldUnitCannotUpdateOrDeleteNextSemesterByDirectId(boolean crossUnit) {
        Long id = insertDraft();
        Map<String, Object> before = stored(id);
        long beforeAudit = auditCount();
        asStaff(oldUnit, crossUnit);

        if (crossUnit) {
            assertThat(invoices.getInvoiceById(id).id()).isEqualTo(id);
        } else {
            assertStatus(() -> invoices.getInvoiceById(id), HttpStatus.NOT_FOUND);
        }
        assertStatus(() -> crud.saveOrUpdate(updateInput(id)), HttpStatus.NOT_FOUND);
        assertStatus(() -> crud.deleteUnusedInvoiceRecord(id), HttpStatus.NOT_FOUND);

        assertThat(stored(id)).isEqualTo(before);
        assertThat(invoiceCount()).isEqualTo(1);
        assertThat(auditCount()).isEqualTo(beforeAudit);
    }

    @Test
    void targetUnitCanCreateUpdateAndDeleteWithOnlyMatchingSemesterApplication() {
        asStaff(targetUnit, false);
        assertSuccessfulCrud();
    }

    @Test
    void adminCanCreateUpdateAndDeleteAcrossUnits() {
        Long adminId = insertUser("admin", Role.ADMIN, null);
        when(currentUser.get()).thenReturn(new Actor(adminId, Role.ADMIN, null, false, false));
        assertSuccessfulCrud();
    }

    @Test
    void overlappingAssignmentAllowsWritesWithoutMatchingUnitApplication() {
        insertAssignment(targetSemester);
        asStaff(oldUnit, false);
        assertSuccessfulCrud();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void paidInvoiceCannotBeEditedOrDeletedEvenByAdmin(boolean admin) {
        Long id = insertDraft();
        jdbc.update(
                "UPDATE invoice SET status=?, paid_at=now(), confirmed_by=?, payment_method=? WHERE id=?",
                InvoiceStatus.PAID,
                staffId,
                PaymentMethod.CASH,
                id);
        if (admin) {
            Long adminId = insertUser("admin", Role.ADMIN, null);
            when(currentUser.get()).thenReturn(new Actor(adminId, Role.ADMIN, null, false, false));
        } else {
            asStaff(targetUnit, false);
        }
        Map<String, Object> before = stored(id);
        long beforeAudit = auditCount();

        assertThatThrownBy(() -> crud.saveOrUpdate(updateInput(id)))
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
                    assertThat(error.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_IN_USE);
                });
        assertThatThrownBy(() -> crud.deleteUnusedInvoiceRecord(id))
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
                    assertThat(error.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_IN_USE);
                });
        assertThat(stored(id)).isEqualTo(before);
        assertThat(auditCount()).isEqualTo(beforeAudit);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        Permission.INVOICE_CREATE, Permission.INVOICE_UPDATE, Permission.INVOICE_DELETE
    })
    void aggregateReadAndMatchingScopeDoNotBypassRevokedWritePermissions(String revoked) {
        Long id = insertDraft();
        Map<String, Object> before = stored(id);
        long beforeAudit = auditCount();
        asStaff(targetUnit, true);
        when(rolePermissions.getGrantedPermissions(Role.STAFF))
                .thenReturn(List.of(
                                Permission.INVOICE_READ,
                                Permission.INVOICE_CREATE,
                                Permission.INVOICE_UPDATE,
                                Permission.INVOICE_DELETE)
                        .stream().filter(permission -> !permission.equals(revoked)).toList());

        assertThat(invoices.getInvoiceById(id).id()).isEqualTo(id);
        switch (revoked) {
            case Permission.INVOICE_CREATE ->
                    assertStatus(() -> crud.saveOrUpdate(createInput()), HttpStatus.FORBIDDEN);
            case Permission.INVOICE_UPDATE ->
                    assertStatus(() -> crud.saveOrUpdate(updateInput(id)), HttpStatus.FORBIDDEN);
            case Permission.INVOICE_DELETE ->
                    assertStatus(() -> crud.deleteUnusedInvoiceRecord(id), HttpStatus.FORBIDDEN);
            default -> throw new AssertionError("Unexpected permission: " + revoked);
        }

        assertThat(stored(id)).isEqualTo(before);
        assertThat(invoiceCount()).isEqualTo(1);
        assertThat(auditCount()).isEqualTo(beforeAudit);
    }

    private void assertSuccessfulCrud() {
        long beforeAudit = auditCount();
        Object result = crud.saveOrUpdate(createInput());
        assertThat(result).isInstanceOf(Map.class);
        Long id = ((Number) ((Map<?, ?>) result).get("id")).longValue();
        assertThat(invoiceCount()).isEqualTo(1);
        assertThat(invoices.getInvoiceById(id).id()).isEqualTo(id);
        assertThat(stored(id).get("semester_id")).isEqualTo(targetSemester.id());
        assertThat(auditCount()).isEqualTo(beforeAudit + 1);

        crud.saveOrUpdate(updateInput(id));
        assertThat(stored(id).get("total_amount")).isEqualTo(200L);
        assertThat(stored(id).get("student_id")).isEqualTo(studentId);
        assertThat(stored(id).get("semester_id")).isEqualTo(targetSemester.id());
        assertThat(auditCount()).isEqualTo(beforeAudit + 2);

        crud.deleteUnusedInvoiceRecord(id);
        assertThat(invoiceCount()).isZero();
        assertThat(auditCount()).isEqualTo(beforeAudit + 3);
        assertThat(jdbc.queryForList(
                        "SELECT action FROM audit_log WHERE entity_type=? AND entity_id=? ORDER BY id",
                        String.class,
                        EntityType.INVOICE,
                        id))
                .containsExactly(AuditAction.CRUD_CREATE, AuditAction.CRUD_UPDATE, AuditAction.CRUD_DELETE);
    }

    private InvoiceDtos.Save createInput() {
        return new InvoiceDtos.Save(
                null, InvoiceType.ROOM_FEE, studentId, targetSemester.id(), null, null, 100L, dueDate);
    }

    private InvoiceDtos.Save updateInput(Long id) {
        return new InvoiceDtos.Save(id, null, null, null, null, null, 200L, null);
    }

    private void assertStatus(Runnable action, HttpStatus status) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(status.value()));
    }

    private void asStaff(Long unit, boolean crossUnit) {
        when(currentUser.get()).thenReturn(new Actor(staffId, Role.STAFF, unit, crossUnit, false));
    }

    private Long insertUnit(String prefix) {
        return jdbc.queryForObject(
                "INSERT INTO management_unit(code, name) VALUES(?, ?) RETURNING id",
                Long.class,
                prefix + suffix,
                prefix + " " + suffix);
    }

    private Long insertUser(String prefix, String role, Long unit) {
        return jdbc.queryForObject(
                "INSERT INTO app_user(username, role, management_unit_id, full_name, password_hash)"
                        + " VALUES(?, ?, ?, ?, 'hash') RETURNING id",
                Long.class,
                prefix + "_" + suffix,
                role,
                unit,
                prefix);
    }

    private Long insertApplication(Long semester, Long unit) {
        Long period = jdbc.queryForObject(
                "INSERT INTO registration_period(semester_id, kind, name, open_at, close_at, created_by)"
                        + " VALUES(?, ?, ?, now(), now() + interval '1 day', ?) RETURNING id",
                Long.class,
                semester,
                RegistrationKind.REGULAR,
                "Period " + suffix,
                staffId);
        return jdbc.queryForObject(
                "INSERT INTO room_application(student_id, period_id, preferred_unit_id)"
                        + " VALUES(?, ?, ?) RETURNING id",
                Long.class,
                studentId,
                period,
                unit);
    }

    private Long insertDraft() {
        return jdbc.queryForObject(
                "INSERT INTO invoice(code, type, student_id, semester_id, total_amount, due_date,"
                        + " status, issued_by) VALUES(?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                "HP-" + suffix,
                InvoiceType.ROOM_FEE,
                studentId,
                targetSemester.id(),
                100L,
                dueDate,
                InvoiceStatus.UNPAID,
                staffId);
    }

    private void insertAssignment(FutureSemesterFixture.Created semester) {
        if (contractId == null) {
            contractId = jdbc.queryForObject(
                    "INSERT INTO dormitory_contract(code, student_id, application_id, start_date,"
                            + " end_date, activation_deadline) VALUES(?, ?, ?, ?, ?, now()) RETURNING id",
                    Long.class,
                    "HD-" + suffix,
                    studentId,
                    oldApplication,
                    oldSemester.startDate(),
                    targetSemester.endDate());
        }
        String code = UUID.randomUUID().toString().substring(0, 8);
        Long building = jdbc.queryForObject(
                "INSERT INTO building(code, name, management_unit_id, deposit_amount)"
                        + " VALUES(?, ?, ?, 1000) RETURNING id",
                Long.class, code, "Building " + code, oldUnit);
        Long roomType = jdbc.queryForObject(
                "INSERT INTO room_type(name, bed_count, price_main, price_summer)"
                        + " VALUES(?, 1, 100, 100) RETURNING id",
                Long.class, "Type " + code);
        Long room = jdbc.queryForObject(
                "INSERT INTO room(building_id, room_type_id, code, floor, gender)"
                        + " VALUES(?, ?, ?, 1, 'M') RETURNING id",
                Long.class, building, roomType, code);
        Long bed = jdbc.queryForObject(
                "INSERT INTO bed(room_id, code) VALUES(?, '01') RETURNING id", Long.class, room);
        jdbc.update(
                "INSERT INTO room_assignment_history(student_id, contract_id, bed_id, kind,"
                        + " started_at, ended_at, end_reason, price_main, price_summer, created_by)"
                        + " VALUES(?, ?, ?, ?, ?, ?, ?, 100, 100, ?)",
                studentId,
                contractId,
                bed,
                AssignmentKind.HOLD,
                semester.startDate(),
                semester.endDate(),
                AssignmentEndReason.HOLD_CANCELLED,
                staffId);
    }

    private Map<String, Object> stored(Long id) {
        return jdbc.queryForMap("SELECT * FROM invoice WHERE id=?", id);
    }

    private long invoiceCount() {
        return jdbc.queryForObject(
                "SELECT count(*) FROM invoice WHERE student_id=? AND semester_id=?",
                Long.class,
                studentId,
                targetSemester.id());
    }

    private long auditCount() {
        entityManager.flush();
        return jdbc.queryForObject(
                "SELECT count(*) FROM audit_log WHERE actor_id IN (?, ?) AND entity_type=?",
                Long.class,
                staffId,
                currentUser.get().id(),
                EntityType.INVOICE);
    }
}
