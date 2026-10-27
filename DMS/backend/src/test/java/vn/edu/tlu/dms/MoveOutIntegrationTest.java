package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentEndReason;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.contract.api.MoveOutDtos;
import vn.edu.tlu.dms.contract.service.MoveOutService;
import vn.edu.tlu.dms.room.service.BedOccupancyService;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Kiểm thử tích hợp trả phòng trên PostgreSQL thật (FR-CON-09, FR-CON-10, FR-CON-13, API 4.8):
 * preview kiểu kết thúc/cọc/công nợ/ticket, cảnh báo bắt buộc xác nhận, hoàn cọc, gỡ trưởng phòng
 * (AT-19, AT-21), chặn trả phòng lần hai, số dư cọc âm và phạm vi đơn vị. Mọi case chạy trong
 * transaction được rollback.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class MoveOutIntegrationTest {

    private static final LocalDate CONTRACT_START = LocalDate.of(2070, 9, 1);

    private static final LocalDate CONTRACT_END = LocalDate.of(2071, 6, 30);

    @Autowired MoveOutService moveOuts;

    @Autowired BedOccupancyService occupancy;

    @Autowired JdbcTemplate jdbc;

    @MockitoBean CurrentUser currentUser;

    private String suffix;

    private Long unitId;

    private Long staffId;

    private Long studentId;

    private Long semesterId;

    private Long periodId;

    private Long contractId;

    private Long roomId;

    private Long bedId;

    @BeforeEach
    void createFixtureData() {
        suffix = UUID.randomUUID().toString().substring(0, 6);
        unitId =
                jdbc.queryForObject(
                        "SELECT id FROM management_unit WHERE code='K1'", Long.class);
        staffId = createUser(Role.STAFF, unitId, "Cán bộ " + suffix);
        when(currentUser.get()).thenReturn(new Actor(staffId, Role.STAFF, unitId, false, false));
        studentId = createStudent("SV" + suffix);
        semesterId =
                jdbc.queryForObject(
                        "INSERT INTO semester(academic_year,type,name,start_date,end_date)"
                                + " VALUES('2070-2071','HK1','HK1','2070-09-01','2071-01-31')"
                                + " RETURNING id",
                        Long.class);
        periodId =
                jdbc.queryForObject(
                        "INSERT INTO registration_period(semester_id,kind,name,open_at,close_at,"
                                + " created_by) VALUES(?,'REGULAR','Đợt',now(),now()+interval '1"
                                + " day',?) RETURNING id",
                        Long.class,
                        semesterId,
                        staffId);
        contractId = createContract(studentId, CONTRACT_END);
        Long roomTypeId =
                jdbc.queryForObject(
                        "INSERT INTO room_type(name,bed_count,price_main,price_summer)"
                                + " VALUES(?,2,1500000,700000) RETURNING id",
                        Long.class,
                        "Loại " + suffix);
        Long buildingId =
                jdbc.queryForObject(
                        "INSERT INTO building(management_unit_id,code,name,deposit_amount)"
                                + " VALUES(?,?,?,1000000) RETURNING id",
                        Long.class,
                        unitId,
                        "B" + suffix,
                        "Tòa " + suffix);
        roomId =
                jdbc.queryForObject(
                        "INSERT INTO room(building_id,room_type_id,floor,code,gender)"
                                + " VALUES(?,?,1,?,'M') RETURNING id",
                        Long.class,
                        buildingId,
                        roomTypeId,
                        "P" + suffix);
        bedId = createBed(roomId, "01");
        occupancy.occupyStay(bedId, studentId, contractId, null);
        jdbc.update(
                "INSERT INTO deposit_transaction(contract_id,type,amount,confirmed_by)"
                        + " VALUES(?,'DEPOSIT',1000000,?)",
                contractId,
                staffId);
        jdbc.update("UPDATE room SET leader_student_id=? WHERE id=?", studentId, roomId);
    }

    /** AT-19/AT-21: kết thúc hợp đồng, đóng phân phòng, hoàn cọc đúng số dư và gỡ trưởng phòng. */
    @Test
    void moveOutRefundsBalanceAndReleasesBed() {
        Long ticketId = createTicket("CLOSED", "MISUSE", 200000L);
        addDeduction(ticketId, 200000L);

        MoveOutDtos.Preview preview = moveOuts.preview(contractId, CONTRACT_END);
        assertThat(preview.depositBalance()).isEqualTo(800000L);
        assertThat(preview.refundAmount()).isEqualTo(800000L);
        assertThat(preview.negativeBalanceDebt()).isZero();
        assertThat(preview.warnings()).isFalse();

        MoveOutDtos.Result result =
                moveOuts.execute(
                        contractId, new MoveOutDtos.Request(CONTRACT_END, "OK", null, false));

        assertThat(result.contractId()).isEqualTo(contractId);
        assertThat(result.contractStatus()).isEqualTo(ContractStatus.EXPIRED);
        assertThat(result.endStatus()).isEqualTo(ContractStatus.EXPIRED);
        assertThat(result.endDate()).isEqualTo(CONTRACT_END);
        assertThat(result.refundAmount()).isEqualTo(800000L);
        assertThat(result.moveOutRecordId()).isNotNull();
        assertThat(result.depositBalance()).isEqualTo(800000L);
        assertThat(result.refundAmount()).isEqualTo(800000L);
        assertThat(result.unpaidRoomFee()).isZero();
        assertThat(result.confirmedBy()).isEqualTo(staffId);

        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM dormitory_contract WHERE id=?",
                                String.class,
                                contractId))
                .isEqualTo(ContractStatus.EXPIRED);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT closed_at IS NOT NULL FROM dormitory_contract WHERE id=?",
                                Boolean.class,
                                contractId))
                .isTrue();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT end_reason FROM room_assignment_history"
                                        + " WHERE contract_id=?",
                                String.class,
                                contractId))
                .isEqualTo(AssignmentEndReason.MOVE_OUT);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT leader_student_id FROM room WHERE id=?",
                                Long.class,
                                roomId))
                .isNull();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT amount FROM deposit_transaction"
                                        + " WHERE contract_id=? AND type='REFUND'",
                                Long.class,
                                contractId))
                .isEqualTo(-800000L);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM status_history WHERE entity_type=?"
                                        + " AND entity_id=? AND from_status=? AND to_status=?",
                                Integer.class,
                                EntityType.CONTRACT,
                                contractId,
                                ContractStatus.ACTIVE,
                                ContractStatus.EXPIRED))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM audit_log WHERE action=? AND entity_id=?",
                                Integer.class,
                                AuditAction.CONTRACT_MOVE_OUT,
                                result.moveOutRecordId()))
                .isEqualTo(1);
        // AT-19: sau khi hoàn cọc, sổ cọc về 0 và không còn nợ bù cọc trên hợp đồng đã kết thúc.
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COALESCE(SUM(amount),0) FROM deposit_transaction"
                                        + " WHERE contract_id=?",
                                Long.class,
                                contractId))
                .isZero();
        var balanceAfter =
                jdbc.queryForMap(
                        "SELECT topup_needed, deposit_debt FROM v_deposit_balance"
                                + " WHERE contract_id=?",
                        contractId);
        assertThat(((Number) balanceAfter.get("topup_needed")).longValue()).isZero();
        assertThat(((Number) balanceAfter.get("deposit_debt")).longValue()).isZero();
    }

    /** Preview trả cảnh báo cho hóa đơn và ticket chưa kết luận; execute đòi xác nhận. */
    @Test
    void warningsRequireAcknowledgement() {
        createRoomFeeInvoice(300000L);
        createTicket("OPEN", null, null);

        MoveOutDtos.Preview preview = moveOuts.preview(contractId, CONTRACT_END.minusDays(1));
        assertThat(preview.endStatus()).isEqualTo(ContractStatus.TERMINATED);
        assertThat(preview.unpaidRoomFeeInvoices()).hasSize(1);
        assertThat(preview.unsettledTickets()).hasSize(1);
        assertThat(preview.isRoomLeader()).isTrue();
        assertThat(preview.warnings()).isTrue();

        assertThatThrownBy(
                        () ->
                                moveOuts.execute(
                                        contractId,
                                        new MoveOutDtos.Request(
                                                CONTRACT_END.minusDays(1), "OK", null, false)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> {
                            assertThat(e.getErrorCode())
                                    .isEqualTo(ErrorCode.MOVE_OUT_WARNINGS_NOT_ACKNOWLEDGED);
                            assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
                            assertThat(e.getDetails())
                                    .containsKeys(
                                            "unpaidRoomFeeInvoices",
                                            "unpaidRoomUtilityInvoices",
                                            "unsettledTickets",
                                            "endStatus");
                        });
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM dormitory_contract WHERE id=?",
                                String.class,
                                contractId))
                .isEqualTo(ContractStatus.ACTIVE);

        MoveOutDtos.Result result =
                moveOuts.execute(
                        contractId,
                        new MoveOutDtos.Request(CONTRACT_END.minusDays(1), "OK", null, true));
        assertThat(result.contractStatus()).isEqualTo(ContractStatus.TERMINATED);
        assertThat(result.warnings()).isTrue();
        assertThat(result.unpaidRoomFee()).isEqualTo(300000L);
        assertThat(result.depositBalance()).isEqualTo(1000000L);
        assertThat(result.refundAmount()).isEqualTo(1000000L);
    }

    /** Số dư cọc âm chỉ ghi công nợ, không tạo giao dịch hoàn cọc. */
    @Test
    void negativeBalanceBecomesDebtWithoutRefund() {
        Long ticketId = createTicket("CLOSED", "MISUSE", 1200000L);
        addDeduction(ticketId, 1200000L);

        MoveOutDtos.Preview preview = moveOuts.preview(contractId, CONTRACT_END);
        assertThat(preview.depositBalance()).isEqualTo(-200000L);
        assertThat(preview.refundAmount()).isZero();
        assertThat(preview.negativeBalanceDebt()).isEqualTo(200000L);

        MoveOutDtos.Result result =
                moveOuts.execute(
                        contractId, new MoveOutDtos.Request(CONTRACT_END, "OK", null, false));

        assertThat(result.depositBalance()).isEqualTo(-200000L);
        assertThat(result.refundAmount()).isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM deposit_transaction"
                                        + " WHERE contract_id=? AND type='REFUND'",
                                Integer.class,
                                contractId))
                .isZero();
    }

    /** Số dư cọc đúng 0 thì không tạo giao dịch hoàn cọc. */
    @Test
    void zeroBalanceDoesNotCreateRefund() {
        Long ticketId = createTicket("CLOSED", "MISUSE", 1000000L);
        addDeduction(ticketId, 1000000L);

        MoveOutDtos.Result result =
                moveOuts.execute(
                        contractId, new MoveOutDtos.Request(CONTRACT_END, "OK", null, false));

        assertThat(result.depositBalance()).isZero();
        assertThat(result.refundAmount()).isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM deposit_transaction"
                                        + " WHERE contract_id=? AND type='REFUND'",
                                Integer.class,
                                contractId))
                .isZero();
    }

    /** Trả phòng lần hai bị chặn; tài sản DAMAGED bắt buộc ghi chú. */
    @Test
    void duplicateMoveOutAndDamagedWithoutNoteRejected() {
        assertThatThrownBy(
                        () ->
                                moveOuts.execute(
                                        contractId,
                                        new MoveOutDtos.Request(CONTRACT_END, "DAMAGED", " ", false)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> {
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                            assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                        });

        moveOuts.execute(
                contractId, new MoveOutDtos.Request(CONTRACT_END, "DAMAGED", "Gãy chân ghế", true));

        assertThatThrownBy(
                        () ->
                                moveOuts.execute(
                                        contractId,
                                        new MoveOutDtos.Request(CONTRACT_END, "OK", null, false)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM move_out_record WHERE contract_id=?",
                                Integer.class,
                                contractId))
                .isEqualTo(1);
    }

    /** Hợp đồng ngoài đơn vị của Staff trả 404 ở cả preview và execute. */
    @Test
    void moveOutOutsideStaffUnitIsNotFound() {
        Long otherUnitId =
                jdbc.queryForObject(
                        "SELECT id FROM management_unit WHERE code='N23'", Long.class);
        Long otherStaff = createUser(Role.STAFF, otherUnitId, "Cán bộ N23 " + suffix);
        when(currentUser.get())
                .thenReturn(new Actor(otherStaff, Role.STAFF, otherUnitId, false, false));

        assertThatThrownBy(() -> moveOuts.preview(contractId, CONTRACT_END))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        assertThatThrownBy(
                        () ->
                                moveOuts.execute(
                                        contractId,
                                        new MoveOutDtos.Request(CONTRACT_END, "OK", null, false)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM dormitory_contract WHERE id=?",
                                String.class,
                                contractId))
                .isEqualTo(ContractStatus.ACTIVE);
    }

    private Long createUser(String role, Long unitId, String name) {
        return jdbc.queryForObject(
                "INSERT INTO app_user(username,password_hash,role,full_name,"
                        + "management_unit_id,must_change_password) VALUES(?,?,?,?,?,false)"
                        + " RETURNING id",
                Long.class,
                role + "-" + UUID.randomUUID(),
                "hash",
                role,
                name,
                unitId);
    }

    private Long createStudent(String code) {
        Long id =
                jdbc.queryForObject(
                        "INSERT INTO app_user(username,password_hash,role,full_name,"
                                + "must_change_password) VALUES(?,?,'STUDENT',?,false)"
                                + " RETURNING id",
                        Long.class,
                        code,
                        "hash",
                        "Sinh viên " + code);
        jdbc.update(
                "INSERT INTO student_profile(user_id,student_code,gender) VALUES(?,?,'M')",
                id,
                code);
        return id;
    }

    private Long createContract(Long student, LocalDate endDate) {
        Long application =
                jdbc.queryForObject(
                        "INSERT INTO room_application(student_id,period_id,preferred_unit_id,"
                                + "status) VALUES(?,?,?,'APPROVED') RETURNING id",
                        Long.class,
                        student,
                        periodId,
                        unitId);
        return jdbc.queryForObject(
                "INSERT INTO dormitory_contract(code,student_id,application_id,start_date,"
                        + "end_date,status,activation_deadline,deposit_required,activated_at)"
                        + " VALUES(?,?,?,?,?,'ACTIVE',now()+interval '7 days',1000000,now())"
                        + " RETURNING id",
                Long.class,
                "HD" + suffix + "-" + application,
                student,
                application,
                CONTRACT_START,
                endDate);
    }

    private Long createBed(Long room, String code) {
        return jdbc.queryForObject(
                "INSERT INTO bed(room_id,code) VALUES(?,?) RETURNING id",
                Long.class,
                room,
                code);
    }

    /** Hóa đơn phí phòng UNPAID của sinh viên trong học kỳ của fixture. */
    private Long createRoomFeeInvoice(long amount) {
        return jdbc.queryForObject(
                "INSERT INTO invoice(code,type,student_id,semester_id,total_amount,due_date,"
                        + "issued_by) VALUES(?,'ROOM_FEE',?,?,?,CURRENT_DATE+7,?) RETURNING id",
                Long.class,
                "HP-" + UUID.randomUUID().toString().substring(0, 8),
                studentId,
                semesterId,
                amount,
                staffId);
    }

    /**
     * Ticket của phòng; cause chỉ có khi ticket CLOSED, đúng ràng buộc CSDL. Dùng clock_timestamp()
     * để thời điểm tạo ticket nằm sau thời điểm bắt đầu ở (now() là thời điểm mở transaction).
     */
    private Long createTicket(String status, String cause, Long repairCost) {
        return jdbc.queryForObject(
                "INSERT INTO maintenance_request(code,requester_id,room_id,category,severity,"
                        + "description,status,cause,repair_cost,created_at)"
                        + " VALUES(?,?,?,'WATER','LOW','Hỏng vòi nước',?,?,?,clock_timestamp())"
                        + " RETURNING id",
                Long.class,
                "TK-" + UUID.randomUUID().toString().substring(0, 8),
                studentId,
                roomId,
                status,
                cause,
                repairCost);
    }

    private void addDeduction(Long ticketId, long amount) {
        jdbc.update(
                "INSERT INTO deposit_transaction(contract_id,type,amount,maintenance_request_id,"
                        + "due_date,confirmed_by,note) VALUES(?,'DEDUCTION',?,?,CURRENT_DATE+7,?,?)",
                contractId,
                -amount,
                ticketId,
                staffId,
                "Trừ cọc sửa chữa");
    }
}
