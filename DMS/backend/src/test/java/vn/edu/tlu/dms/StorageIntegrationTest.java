package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentEndReason;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.contract.api.StorageDtos;
import vn.edu.tlu.dms.contract.service.StorageService;
import vn.edu.tlu.dms.room.service.BedOccupancyService;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Kiểm thử tích hợp gửi đồ tạm rời hè và nhận lại đồ trên PostgreSQL thật (FR-CON-11, FR-CON-12,
 * API 4.9): điều kiện lựa chọn TEMP_LEAVE đã duyệt và gia hạn qua kỳ hè, đóng phân phòng
 * SUMMER_LEAVE nhưng giữ hợp đồng ACTIVE/cọc (AT-24), phân lại giường khi giường cũ đã có người
 * (AT-25), chặn phiếu trùng, phạm vi đơn vị và trạng thái hợp đồng. Mọi case chạy trong transaction
 * được rollback.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class StorageIntegrationTest {

    private static final LocalDate CONTRACT_START = LocalDate.of(2070, 9, 1);

    private static final LocalDate CONTRACT_END = LocalDate.of(2071, 6, 30);

    private static final LocalDate SUMMER_START = LocalDate.of(2071, 5, 1);

    private static final LocalDate SUMMER_END = LocalDate.of(2071, 5, 31);

    private static final LocalDate DEPOSITED_AT = LocalDate.of(2071, 5, 20);

    @Autowired StorageService storage;

    @Autowired BedOccupancyService occupancy;

    @Autowired JdbcTemplate jdbc;

    @MockitoBean CurrentUser currentUser;

    private String suffix;

    private Long unitId;

    private Long staffId;

    private Long studentId;

    private Long periodId;

    private Long contractId;

    private Long roomId;

    private Long bedId;

    private Long summerChoiceId;

    @BeforeEach
    void createFixtureData() {
        suffix = UUID.randomUUID().toString().substring(0, 6);
        unitId =
                jdbc.queryForObject(
                        "SELECT id FROM management_unit WHERE code='K1'", Long.class);
        staffId = createUser(Role.STAFF, unitId, "Cán bộ " + suffix);
        when(currentUser.get()).thenReturn(new Actor(staffId, Role.STAFF, unitId, false, false));
        studentId = createStudent("SV" + suffix);
        Long semesterId =
                jdbc.queryForObject(
                        "INSERT INTO semester(academic_year,type,name,start_date,end_date)"
                                + " VALUES('2070-2071','SUMMER','Học kỳ hè',?,?) RETURNING id",
                        Long.class,
                        SUMMER_START,
                        SUMMER_END);
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
        createBed(roomId, "02");
        createBed(roomId, "03");
        occupancy.occupyStay(bedId, studentId, contractId, null);
        jdbc.update(
                "INSERT INTO deposit_transaction(contract_id,type,amount,confirmed_by)"
                        + " VALUES(?,'DEPOSIT',1000000,?)",
                contractId,
                staffId);
        jdbc.update("UPDATE room SET leader_student_id=? WHERE id=?", studentId, roomId);
        summerChoiceId = createChoice(semesterId);
    }

    /** AT-24: tạo phiếu, đóng phân phòng SUMMER_LEAVE, giữ hợp đồng ACTIVE và nguyên cọc. */
    @Test
    void createClosesAssignmentButKeepsContractActive() {
        StorageDtos.Record record = storage.create(contractId, createRequest());

        assertThat(record.id()).isNotNull();
        assertThat(record.contractId()).isEqualTo(contractId);
        assertThat(record.studentId()).isEqualTo(studentId);
        assertThat(record.summerChoiceId()).isEqualTo(summerChoiceId);
        assertThat(record.previousBedId()).isEqualTo(bedId);
        assertThat(record.packageCount()).isEqualTo((short) 3);
        assertThat(record.receivedBy()).isEqualTo(staffId);
        assertThat(record.returnedAt()).isNull();

        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM dormitory_contract WHERE id=?",
                                String.class,
                                contractId))
                .isEqualTo(ContractStatus.ACTIVE);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT end_reason FROM room_assignment_history"
                                        + " WHERE contract_id=?",
                                String.class,
                                contractId))
                .isEqualTo(AssignmentEndReason.SUMMER_LEAVE);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT closed_at IS NULL FROM dormitory_contract WHERE id=?",
                                Boolean.class,
                                contractId))
                .isTrue();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COALESCE(SUM(amount),0) FROM deposit_transaction"
                                        + " WHERE contract_id=?",
                                Long.class,
                                contractId))
                .isEqualTo(1000000L);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT leader_student_id FROM room WHERE id=?", Long.class, roomId))
                .isNull();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM invoice WHERE student_id=?",
                                Integer.class,
                                studentId))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM audit_log WHERE action=? AND entity_id=?",
                                Integer.class,
                                AuditAction.STORAGE_CREATE,
                                record.id()))
                .isEqualTo(1);

        // ux_storage_open: mỗi hợp đồng tối đa một phiếu chưa nhận lại.
        assertThatThrownBy(() -> storage.create(contractId, createRequest()))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STORAGE_RECORD_EXISTS));
    }

    /** Chỉ nhận lựa chọn TEMP_LEAVE đã APPROVED. */
    @Test
    void createRequiresApprovedTempLeave() {
        jdbc.update("UPDATE summer_choice SET status='PENDING' WHERE id=?", summerChoiceId);

        assertThatThrownBy(() -> storage.create(contractId, createRequest()))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.SUMMER_CHOICE_NOT_APPROVED));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM storage_record WHERE contract_id=?",
                                Integer.class,
                                contractId))
                .isZero();
    }

    /** Hợp đồng phải phủ qua kỳ hè hoặc có yêu cầu gia hạn APPROVED vượt mốc. */
    @Test
    void createRequiresExtensionPastSummerSemester() {
        jdbc.update(
                "UPDATE dormitory_contract SET end_date=? WHERE id=?",
                SUMMER_END.minusDays(10),
                contractId);

        assertThatThrownBy(() -> storage.create(contractId, createRequest()))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.TEMP_LEAVE_NOT_EXTENDED));

        createApprovedExtension(SUMMER_END.plusMonths(2));

        assertThat(storage.create(contractId, createRequest()).id()).isNotNull();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM dormitory_contract WHERE id=?",
                                String.class,
                                contractId))
                .isEqualTo(ContractStatus.ACTIVE);
    }

    /** AT-25: giường cũ đã có người, Staff nhận lại đồ bằng giường khác và ghi ngày nhận. */
    @Test
    void returnOccupiesAvailableBedAndRecordsDate() {
        StorageDtos.Record record = storage.create(contractId, createRequest());

        // Giường cũ được dùng lại trong hè bởi sinh viên khác.
        Long otherStudent = createStudent("SV" + suffix + "B");
        Long otherContract = createContract(otherStudent, CONTRACT_END);
        occupancy.occupyStay(bedId, otherStudent, otherContract, null);

        Long newBedId = createBed(roomId, "04");
        LocalDate returnedAt = LocalDate.of(2071, 8, 20);
        StorageDtos.Record returned =
                storage.returnStorage(
                        record.id(), new StorageDtos.ReturnRequest(newBedId, returnedAt));

        assertThat(returned.returnedAt()).isEqualTo(returnedAt);
        assertThat(returned.returnedBy()).isEqualTo(staffId);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT bed_id FROM room_assignment_history"
                                        + " WHERE contract_id=? AND ended_at IS NULL",
                                Long.class,
                                contractId))
                .isEqualTo(newBedId);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT kind FROM room_assignment_history"
                                        + " WHERE contract_id=? AND ended_at IS NULL",
                                String.class,
                                contractId))
                .isEqualTo("STAY");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM dormitory_contract WHERE id=?",
                                String.class,
                                contractId))
                .isEqualTo(ContractStatus.ACTIVE);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM audit_log WHERE action=? AND entity_id=?",
                                Integer.class,
                                AuditAction.STORAGE_RETURN,
                                record.id()))
                .isEqualTo(1);

        assertThatThrownBy(
                        () ->
                                storage.returnStorage(
                                        record.id(),
                                        new StorageDtos.ReturnRequest(newBedId, returnedAt)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
    }

    /** Hợp đồng đã kết thúc thì không nhận lại đồ. */
    @Test
    void returnRequiresActiveContract() {
        StorageDtos.Record record = storage.create(contractId, createRequest());
        jdbc.update(
                "UPDATE dormitory_contract SET status=? WHERE id=?",
                ContractStatus.EXPIRED,
                contractId);

        assertThatThrownBy(
                        () ->
                                storage.returnStorage(
                                        record.id(),
                                        new StorageDtos.ReturnRequest(bedId, DEPOSITED_AT)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONTRACT_NOT_ACTIVE));
    }

    /** Phiếu và giường cũ ngoài đơn vị của Staff trả 404. */
    @Test
    void storageOutsideStaffUnitIsNotFound() {
        StorageDtos.Record record = storage.create(contractId, createRequest());
        Long otherUnitId =
                jdbc.queryForObject(
                        "SELECT id FROM management_unit WHERE code='N23'", Long.class);
        Long otherStaff = createUser(Role.STAFF, otherUnitId, "Cán bộ N23 " + suffix);
        when(currentUser.get())
                .thenReturn(new Actor(otherStaff, Role.STAFF, otherUnitId, false, false));

        assertThatThrownBy(() -> storage.create(contractId, createRequest()))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        assertThatThrownBy(
                        () ->
                                storage.returnStorage(
                                        record.id(),
                                        new StorageDtos.ReturnRequest(bedId, DEPOSITED_AT)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM storage_record WHERE contract_id=?",
                                Integer.class,
                                contractId))
                .isEqualTo(1);
    }

    private StorageDtos.CreateRequest createRequest() {
        return new StorageDtos.CreateRequest(
                (short) 3, "2 thùng sách, 1 vali", "Kho K1 tầng 1, kệ B", DEPOSITED_AT);
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

    private Long createChoice(Long semesterId) {
        return jdbc.queryForObject(
                "INSERT INTO summer_choice(contract_id,semester_id,choice,status,decided_by,"
                        + "decided_at) VALUES(?,?,'TEMP_LEAVE','APPROVED',?,now()) RETURNING id",
                Long.class,
                contractId,
                semesterId,
                staffId);
    }

    private void createApprovedExtension(LocalDate newEndDate) {
        jdbc.update(
                "INSERT INTO contract_extension_request(contract_id,source,old_end_date,"
                        + "new_end_date,reason,status,decided_by,decided_at)"
                        + " VALUES(?,'STUDENT',?,?,'Gia hạn qua hè','APPROVED',?,now())",
                contractId,
                SUMMER_END.minusDays(10),
                newEndDate,
                staffId);
    }
}
