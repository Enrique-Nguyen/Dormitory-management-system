package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentEndReason;
import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditReason;
import vn.edu.tlu.dms.common.constants.DmsConstants.BusinessAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.StayRequestStatus;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.contract.api.ContractDtos;
import vn.edu.tlu.dms.contract.api.ContractDtos.TransferDetail;
import vn.edu.tlu.dms.contract.service.ContractCrudService;
import vn.edu.tlu.dms.contract.service.RoomTransferService;
import vn.edu.tlu.dms.room.api.RoomDtos.AvailableRoom;
import vn.edu.tlu.dms.room.service.BedOccupancyService;
import vn.edu.tlu.dms.room.service.OccupancyQuery;
import vn.edu.tlu.dms.room.service.RoomService;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Kiểm thử dịch vụ chiếm/trả/chuyển giường trên PostgreSQL thật: khóa, hai partial unique index,
 * chuyển phòng và tự gỡ trưởng phòng (BR-01, BR-02, BR-05, BR-11; AT-02, AT-03, AT-04, AT-08,
 * AT-21), cùng xét duyệt yêu cầu chuyển phòng (FR-CON-07/08). AuditService được mock vì audit_log
 * chỉ ghi thêm, không dọn được sau case commit thật.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class BedOccupancyIntegrationTest {

    @Autowired BedOccupancyService occupancy;

    @Autowired OccupancyQuery occupancyQuery;

    @Autowired RoomService roomService;

    @Autowired RoomTransferService transfers;

    @Autowired ContractCrudService contractCrud;

    @Autowired JdbcTemplate jdbc;

    @Autowired PlatformTransactionManager transactionManager;

    @MockitoBean CurrentUser currentUser;

    @MockitoBean AuditService audit;

    private String suffix;

    private Long firstUnitId;

    private Long otherUnitId;

    private Long staffId;

    private Long roomTypeId;

    private Long firstBuildingId;

    private Long otherBuildingId;

    private Long semesterId;

    private Long periodId;

    private Long roomA;

    private Long roomB;

    private Long femaleRoom;

    private Long otherUnitRoom;

    private List<Long> beds;

    private final List<Long> students = new ArrayList<>();

    @BeforeEach
    void createFixtureData() {
        suffix = UUID.randomUUID().toString().substring(0, 6);
        firstUnitId =
                jdbc.queryForObject("SELECT id FROM management_unit WHERE code = 'K1'", Long.class);
        otherUnitId =
                jdbc.queryForObject(
                        "SELECT id FROM management_unit WHERE code = 'N23'", Long.class);
        staffId = createUser(Role.STAFF, firstUnitId);
        actAs(staffId, Role.STAFF, firstUnitId);
        roomTypeId =
                jdbc.queryForObject(
                        "INSERT INTO room_type(name,bed_count,price_main,price_summer)"
                                + " VALUES (?,2,1500000,700000) RETURNING id",
                        Long.class,
                        "Loại " + suffix);
        firstBuildingId = createBuilding(firstUnitId, "A" + suffix);
        otherBuildingId = createBuilding(otherUnitId, "B" + suffix);
        roomA = createRoom(firstBuildingId, "A101", "M");
        roomB = createRoom(firstBuildingId, "A102", "M");
        femaleRoom = createRoom(firstBuildingId, "A103", "F");
        otherUnitRoom = createRoom(otherBuildingId, "B101", "M");
        beds =
                List.of(
                        createBed(roomA, "01"),
                        createBed(roomA, "02"),
                        createBed(roomB, "01"),
                        createBed(roomB, "02"),
                        createBed(femaleRoom, "01"),
                        createBed(otherUnitRoom, "01"));
        semesterId =
                jdbc.queryForObject(
                        "INSERT INTO semester(academic_year,type,name,start_date,end_date)"
                                + " VALUES('2050-2051','HK1','HK1','2050-09-01','2051-01-31')"
                                + " RETURNING id",
                        Long.class);
        periodId =
                jdbc.queryForObject(
                        "INSERT INTO registration_period(semester_id,kind,name,open_at,close_at,"
                                + "created_by) VALUES(?,'REGULAR','Đợt',now(),now()+interval '1"
                                + " day',?) RETURNING id",
                        Long.class,
                        semesterId,
                        staffId);
    }

    @Test
    void occupyHoldSnapshotsRoomTypePriceAndCanBecomeStay() {
        Long student = createStudent("M");
        Long contract = createContract(student, "PENDING");

        var held = occupancy.occupyHold(beds.get(0), student, contract, "Phân giường");

        assertThat(held.kind()).isEqualTo(AssignmentKind.HOLD);
        assertThat(held.priceMain()).isEqualTo(1500000L);
        assertThat(held.priceSummer()).isEqualTo(700000L);
        assertThat(held.bedCode()).isEqualTo("A101-01");
        assertThat(occupancyQuery.findOpenAssignmentByContract(contract))
                .get()
                .satisfies(
                        bed -> {
                            assertThat(bed.bedId()).isEqualTo(beds.get(0));
                            assertThat(bed.managementUnitId()).isEqualTo(firstUnitId);
                        });

        var stay = occupancy.convertHoldToStay(contract, staffId);
        assertThat(stay.id()).isEqualTo(held.id());
        assertThat(stay.kind()).isEqualTo(AssignmentKind.STAY);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT stay_started_at IS NOT NULL FROM room_assignment_history"
                                        + " WHERE id=?",
                                Boolean.class,
                                held.id()))
                .isTrue();
        assertConflict(
                () -> occupancy.convertHoldToStay(contract, staffId),
                ErrorCode.INVALID_STATE_TRANSITION);
    }

    @Test
    void occupyRejectsBedOutsideStaffUnitAsNotFound() {
        Long student = createStudent("M");
        Long contract = createContract(student, "PENDING");

        assertConflict(
                () -> occupancy.occupyHold(beds.get(5), student, contract, null),
                ErrorCode.NOT_FOUND);
    }

    @Test
    void occupyRejectsInactiveBedOrRoomAndGenderMismatch() {
        Long student = createStudent("M");
        Long contract = createContract(student, "PENDING");
        jdbc.update("UPDATE bed SET status='BROKEN' WHERE id=?", beds.get(0));
        jdbc.update("UPDATE room SET status='MAINTENANCE' WHERE id=?", roomB);

        assertConflict(
                () -> occupancy.occupyHold(beds.get(0), student, contract, null),
                ErrorCode.BED_NOT_AVAILABLE);
        assertConflict(
                () -> occupancy.occupyHold(beds.get(2), student, contract, null),
                ErrorCode.BED_NOT_AVAILABLE);
        assertConflict(
                () -> occupancy.occupyHold(beds.get(4), student, contract, null),
                ErrorCode.GENDER_MISMATCH);
    }

    @Test
    void takenBedAndRepeatedAssignmentAreRejected() {
        Long first = createStudent("M");
        Long second = createStudent("M");
        Long firstContract = createContract(first, "PENDING");
        Long secondContract = createContract(second, "PENDING");
        occupancy.occupyHold(beds.get(0), first, firstContract, null);

        assertConflict(
                () -> occupancy.occupyHold(beds.get(0), second, secondContract, null),
                ErrorCode.BED_TAKEN);
        // AT-04: gửi lặp phân phòng cho cùng sinh viên.
        assertConflict(
                () -> occupancy.occupyHold(beds.get(1), first, firstContract, null),
                ErrorCode.STUDENT_ALREADY_ASSIGNED);
    }

    @Test
    void databaseRejectsTwoOpenAssignmentsOnOneBed() {
        // AT-03: chèn trực tiếp bằng SQL, bỏ qua Service.
        Long first = createStudent("M");
        Long second = createStudent("M");
        insertOpenAssignment(first, createContract(first, "PENDING"), beds.get(0));

        assertThatThrownBy(
                        () ->
                                insertOpenAssignment(
                                        second, createContract(second, "PENDING"), beds.get(0)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ux_assignment_open_bed");
    }

    @Test
    void releaseClosesAssignmentAndRemovesRoomLeader() {
        // AT-21: trưởng phòng trả phòng thì phòng mất chỉ định trưởng phòng.
        Long student = createStudent("M");
        Long contract = createContract(student, "ACTIVE");
        occupancy.occupyStay(beds.get(0), student, contract, null);
        jdbc.update("UPDATE room SET leader_student_id=? WHERE id=?", student, roomA);

        var released =
                occupancy.release(contract, AssignmentEndReason.MOVE_OUT, staffId, "Trả phòng");

        assertThat(released).isPresent();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT end_reason FROM room_assignment_history WHERE id=?",
                                String.class,
                                released.get().id()))
                .isEqualTo(AssignmentEndReason.MOVE_OUT);
        assertThat(leaderOf(roomA)).isNull();
        verify(audit)
                .record(
                        eq(staffId),
                        eq(AuditAction.ROOM_LEADER_CHANGE),
                        eq(EntityType.ROOM),
                        eq(roomA),
                        eq(student),
                        isNull(),
                        eq(AuditReason.LEADER_AUTO_REMOVED));
        assertThat(occupancy.release(contract, AssignmentEndReason.MOVE_OUT, null, null)).isEmpty();
        assertConflict(
                () -> occupancy.release(contract, AssignmentEndReason.TRANSFER, null, null),
                ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void transferOpensConsecutiveStayWithTargetPriceAndRemovesLeader() {
        Long student = createStudent("M");
        Long contract = createContract(student, "ACTIVE");
        var before = occupancy.occupyStay(beds.get(0), student, contract, null);
        jdbc.update("UPDATE room SET leader_student_id=? WHERE id=?", student, roomA);
        jdbc.update("UPDATE room_type SET price_main=1800000 WHERE id=?", roomTypeId);

        var after = occupancy.transfer(contract, beds.get(2), "Chuyển phòng");

        assertThat(after.bedId()).isEqualTo(beds.get(2));
        assertThat(after.kind()).isEqualTo(AssignmentKind.STAY);
        assertThat(after.priceMain()).isEqualTo(1800000L);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT end_reason FROM room_assignment_history WHERE id=?",
                                String.class,
                                before.id()))
                .isEqualTo(AssignmentEndReason.TRANSFER);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM dormitory_contract WHERE id=?",
                                String.class,
                                contract))
                .isEqualTo("ACTIVE");
        assertThat(leaderOf(roomA)).isNull();
        verify(audit)
                .record(
                        eq(staffId),
                        eq(AuditAction.ROOM_TRANSFER),
                        eq(EntityType.ASSIGNMENT),
                        eq(after.id()),
                        any(),
                        any(),
                        eq("Chuyển phòng"));
    }

    @Test
    void transferValidatesSourceAndTarget() {
        Long student = createStudent("M");
        Long other = createStudent("M");
        Long contract = createContract(student, "ACTIVE");
        Long otherContract = createContract(other, "ACTIVE");
        occupancy.occupyStay(beds.get(0), student, contract, null);
        occupancy.occupyStay(beds.get(2), other, otherContract, null);

        assertConflict(() -> occupancy.transfer(contract, beds.get(0), null), ErrorCode.SAME_BED);
        // AT-08: giường đích thuộc đơn vị khác.
        assertConflict(
                () -> occupancy.transfer(contract, beds.get(5), null),
                ErrorCode.TRANSFER_CROSS_UNIT);
        assertConflict(() -> occupancy.transfer(contract, beds.get(2), null), ErrorCode.BED_TAKEN);
        assertConflict(
                () -> occupancy.transfer(contract, beds.get(4), null), ErrorCode.GENDER_MISMATCH);

        Long holder = createStudent("M");
        Long holdContract = createContract(holder, "PENDING");
        occupancy.occupyHold(beds.get(1), holder, holdContract, null);
        assertConflict(
                () -> occupancy.transfer(holdContract, beds.get(3), null), ErrorCode.NOT_STAYING);
    }

    @Test
    void transferOutsideStaffUnitIsNotFound() {
        Long student = createStudent("M");
        Long contract = createContract(student, "ACTIVE");
        occupancy.occupyStay(beds.get(0), student, contract, null);
        Long otherStaff = createUser(Role.STAFF, otherUnitId);
        actAs(otherStaff, Role.STAFF, otherUnitId);

        assertConflict(() -> occupancy.transfer(contract, beds.get(2), null), ErrorCode.NOT_FOUND);
    }

    @Test
    void availableBedsForApplicationAreGroupedAndPreferredTypeFirst() {
        Long preferredType =
                jdbc.queryForObject(
                        "INSERT INTO room_type(name,bed_count,price_main,price_summer)"
                                + " VALUES (?,2,2000000,900000) RETURNING id",
                        Long.class,
                        "Ưu tiên " + suffix);
        jdbc.update("UPDATE room SET room_type_id=? WHERE id=?", preferredType, roomB);
        jdbc.update("UPDATE bed SET status='BROKEN' WHERE id=?", beds.get(3));
        Long holder = createStudent("M");
        occupancy.occupyHold(beds.get(0), holder, createContract(holder, "PENDING"), null);
        Long applicant = createStudent("M");
        Long application = createApplication(applicant, "APPROVED", preferredType);

        List<AvailableRoom> rooms = roomService.findAvailableBeds(application, null, null, null);

        assertThat(rooms).extracting(r -> r.room().id()).containsExactly(roomB, roomA);
        assertThat(rooms.get(0).matchesPreferredType()).isTrue();
        assertThat(rooms.get(0).room().priceMain()).isEqualTo(2000000L);
        assertThat(rooms.get(0).freeBeds()).extracting(b -> b.id()).containsExactly(beds.get(2));
        assertThat(rooms.get(1).matchesPreferredType()).isFalse();
        assertThat(rooms.get(1).freeBeds()).extracting(b -> b.id()).containsExactly(beds.get(1));
        assertThat(rooms.get(1).heldCount()).isEqualTo(1L);
        assertThat(roomService.findAvailableBeds(application, null, null, roomTypeId))
                .extracting(r -> r.room().id())
                .containsExactly(roomA);
    }

    @Test
    void availableBedsForTransferStayInSourceUnitAndExcludeCurrentBed() {
        Long student = createStudent("M");
        Long contract = createContract(student, "ACTIVE");
        occupancy.occupyStay(beds.get(0), student, contract, null);
        Long request =
                jdbc.queryForObject(
                        "INSERT INTO room_transfer_request(contract_id,reason) VALUES(?,?)"
                                + " RETURNING id",
                        Long.class,
                        contract,
                        "Muốn đổi phòng");

        List<AvailableRoom> rooms = roomService.findAvailableBeds(null, request, null, null);

        assertThat(rooms)
                .flatExtracting(AvailableRoom::freeBeds)
                .extracting(b -> b.id())
                .containsExactly(beds.get(1), beds.get(2), beds.get(3));

        jdbc.update("UPDATE room_transfer_request SET status='CANCELLED' WHERE id=?", request);
        assertConflict(
                () -> roomService.findAvailableBeds(null, request, null, null),
                ErrorCode.INVALID_STATE_TRANSITION);
    }

    @Test
    void availableBedsValidateSourceStateAndScope() {
        Long applicant = createStudent("M");
        Long submitted = createApplication(applicant, "SUBMITTED", null);

        assertConflict(
                () -> roomService.findAvailableBeds(submitted, null, null, null),
                ErrorCode.INVALID_STATE_TRANSITION);
        assertConflict(
                () -> roomService.findAvailableBeds(submitted, 1L, null, null),
                ErrorCode.VALIDATION_FAILED);
        Long otherStaff = createUser(Role.STAFF, otherUnitId);
        actAs(otherStaff, Role.STAFF, otherUnitId);
        assertConflict(
                () -> roomService.findAvailableBeds(submitted, null, null, null),
                ErrorCode.NOT_FOUND);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentRequestsForLastBedHaveExactlyOneWinner() throws Exception {
        // AT-02: hai request đồng thời tranh một giường.
        try {
            Long first = createStudent("M");
            Long second = createStudent("M");
            Long firstContract = createContract(first, "PENDING");
            Long secondContract = createContract(second, "PENDING");

            var results =
                    runConcurrently(
                            () -> occupancy.occupyHold(beds.get(0), first, firstContract, null),
                            () -> occupancy.occupyHold(beds.get(0), second, secondContract, null));

            assertThat(results).containsExactlyInAnyOrder("OK", ErrorCode.BED_TAKEN);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT count(*) FROM room_assignment_history WHERE bed_id=?",
                                    Long.class,
                                    beds.get(0)))
                    .isEqualTo(1L);
        } finally {
            cleanUp();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void crossingTransfersDoNotDeadlock() throws Exception {
        // Hai yêu cầu chuyển chéo nhau khóa cùng cặp giường; thứ tự bed_id tăng dần loại deadlock.
        try {
            Long first = createStudent("M");
            Long second = createStudent("M");
            Long firstContract = createContract(first, "ACTIVE");
            Long secondContract = createContract(second, "ACTIVE");
            inTransaction(() -> occupancy.occupyStay(beds.get(0), first, firstContract, null));
            inTransaction(() -> occupancy.occupyStay(beds.get(2), second, secondContract, null));

            var results =
                    runConcurrently(
                            () -> occupancy.transfer(firstContract, beds.get(2), null),
                            () -> occupancy.transfer(secondContract, beds.get(0), null));

            assertThat(results).containsOnly(ErrorCode.BED_TAKEN);
        } finally {
            cleanUp();
        }
    }

    @Test
    void approveTransferMovesStudentAndRecordsDecision() {
        Long student = createStudent("M");
        Long contract = createContract(student, "ACTIVE");
        occupancy.occupyStay(beds.get(0), student, contract, null);
        Long request = createTransferRequest(contract);

        TransferDetail detail = transfers.getTransferRequest(request);
        assertThat(detail.currentBed().bedId()).isEqualTo(beds.get(0));
        assertThat(detail.allowedActions())
                .containsExactly(BusinessAction.APPROVE, BusinessAction.REJECT);

        TransferDetail approved = transfers.approve(request, beds.get(2));

        assertThat(approved.status()).isEqualTo(StayRequestStatus.APPROVED);
        assertThat(approved.targetBedId()).isEqualTo(beds.get(2));
        assertThat(approved.decidedBy()).isEqualTo(staffId);
        assertThat(approved.decidedAt()).isNotNull();
        assertThat(approved.currentBed().bedId()).isEqualTo(beds.get(2));
        assertThat(approved.allowedActions()).isEmpty();
        assertThat(approved.timeline())
                .extracting(e -> e.fromStatus() + ">" + e.toStatus())
                .containsExactly(StayRequestStatus.PENDING + ">" + StayRequestStatus.APPROVED);
        assertThat(
                        jdbc.queryForList(
                                "SELECT end_reason FROM room_assignment_history WHERE contract_id=?"
                                        + " ORDER BY id",
                                String.class,
                                contract))
                .containsExactly(AssignmentEndReason.TRANSFER, null);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM dormitory_contract WHERE id=?",
                                String.class,
                                contract))
                .isEqualTo("ACTIVE");
        // Đã duyệt thì không duyệt lại được, phân phòng không đổi thêm.
        assertConflict(
                () -> transfers.approve(request, beds.get(3)), ErrorCode.INVALID_STATE_TRANSITION);
    }

    @Test
    void approveValidatesScopeTargetBedAndState() {
        Long student = createStudent("M");
        Long contract = createContract(student, "ACTIVE");
        occupancy.occupyStay(beds.get(0), student, contract, null);
        Long other = createStudent("M");
        Long otherContract = createContract(other, "ACTIVE");
        occupancy.occupyStay(beds.get(2), other, otherContract, null);
        Long request = createTransferRequest(contract);

        assertConflict(() -> transfers.approve(request, beds.get(2)), ErrorCode.BED_TAKEN);
        assertConflict(
                () -> transfers.approve(request, beds.get(5)), ErrorCode.TRANSFER_CROSS_UNIT);
        assertThat(requestStatus(request)).isEqualTo(StayRequestStatus.PENDING);

        Long otherStaff = createUser(Role.STAFF, otherUnitId);
        actAs(otherStaff, Role.STAFF, otherUnitId);
        assertConflict(() -> transfers.approve(request, beds.get(1)), ErrorCode.NOT_FOUND);
        assertConflict(() -> transfers.getTransferRequest(request), ErrorCode.NOT_FOUND);

        actAs(student, Role.STUDENT, null);
        assertConflict(() -> transfers.approve(request, beds.get(1)), ErrorCode.FORBIDDEN);
    }

    @Test
    void rejectRequiresReasonAndCancelBelongsToOwner() {
        Long student = createStudent("M");
        Long contract = createContract(student, "ACTIVE");
        occupancy.occupyStay(beds.get(0), student, contract, null);
        Long rejected = createTransferRequest(contract);

        assertConflict(() -> transfers.reject(rejected, " "), ErrorCode.VALIDATION_FAILED);
        TransferDetail detail = transfers.reject(rejected, "Hết phòng phù hợp");
        assertThat(detail.status()).isEqualTo(StayRequestStatus.REJECTED);
        assertThat(detail.decisionReason()).isEqualTo("Hết phòng phù hợp");
        assertThat(detail.timeline()).extracting(e -> e.reason()).containsExactly("Hết phòng phù hợp");
        assertConflict(
                () -> transfers.approve(rejected, beds.get(1)), ErrorCode.INVALID_STATE_TRANSITION);

        Long cancelled = createTransferRequest(contract);
        assertConflict(() -> transfers.cancel(cancelled), ErrorCode.FORBIDDEN);

        Long stranger = createStudent("M");
        actAs(stranger, Role.STUDENT, null);
        assertConflict(() -> transfers.cancel(cancelled), ErrorCode.NOT_FOUND);

        actAs(student, Role.STUDENT, null);
        assertThat(transfers.getTransferRequest(cancelled).allowedActions())
                .containsExactly(BusinessAction.CANCEL);
        assertThat(transfers.cancel(cancelled).status()).isEqualTo(StayRequestStatus.CANCELLED);
        assertConflict(() -> transfers.cancel(cancelled), ErrorCode.INVALID_STATE_TRANSITION);
        assertThat(occupancyQuery.findOpenAssignmentByContract(contract).orElseThrow().bedId())
                .isEqualTo(beds.get(0));
    }

    @Test
    void studentCreatesTransferRequestOnlyWhileStaying() {
        Long student = createStudent("M");
        Long contract = createContract(student, "ACTIVE");
        occupancy.occupyHold(beds.get(0), student, contract, null);
        actAs(student, Role.STUDENT, null);
        var input = new ContractDtos.StaySave(null, contract, null, "Muốn đổi phòng");

        assertConflict(
                () -> contractCrud.saveOrUpdateStayRequest(false, input),
                ErrorCode.NOT_STAYING);

        actAs(staffId, Role.STAFF, firstUnitId);
        occupancy.convertHoldToStay(contract, staffId);
        actAs(student, Role.STUDENT, null);
        contractCrud.saveOrUpdateStayRequest(false, input);
        assertThatThrownBy(() -> contractCrud.saveOrUpdateStayRequest(false, input))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ux_transfer_pending");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentApprovalsMoveStudentOnlyOnce() throws Exception {
        try {
            Long student = createStudent("M");
            Long contract = createContract(student, "ACTIVE");
            inTransaction(() -> occupancy.occupyStay(beds.get(0), student, contract, null));
            Long request = createTransferRequest(contract);

            var results =
                    runConcurrently(
                            () -> transfers.approve(request, beds.get(2)),
                            () -> transfers.approve(request, beds.get(3)));

            assertThat(results).contains("OK").hasSize(2);
            assertThat(results.stream().filter(r -> !"OK".equals(r)).toList())
                    .hasSize(1)
                    .allMatch(
                            code ->
                                    ErrorCode.CONCURRENT_UPDATE.equals(code)
                                            || ErrorCode.INVALID_STATE_TRANSITION.equals(code));
            assertThat(requestStatus(request)).isEqualTo(StayRequestStatus.APPROVED);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT count(*) FROM room_assignment_history WHERE"
                                            + " contract_id=?",
                                    Long.class,
                                    contract))
                    .isEqualTo(2L);
        } finally {
            cleanUp();
        }
    }

    private Long createTransferRequest(Long contract) {
        return jdbc.queryForObject(
                "INSERT INTO room_transfer_request(contract_id,reason) VALUES(?,?) RETURNING id",
                Long.class,
                contract,
                "Muốn đổi phòng");
    }

    private String requestStatus(Long request) {
        return jdbc.queryForObject(
                "SELECT status FROM room_transfer_request WHERE id=?", String.class, request);
    }

    private List<String> runConcurrently(Runnable... actions) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(actions.length);
        try {
            var ready = new CountDownLatch(actions.length);
            var start = new CountDownLatch(1);
            List<Future<String>> futures = new ArrayList<>();
            for (Runnable action : actions) {
                futures.add(
                        pool.submit(
                                () -> {
                                    ready.countDown();
                                    start.await();
                                    try {
                                        inTransaction(action);
                                        return "OK";
                                    } catch (BusinessException e) {
                                        return e.getErrorCode();
                                    }
                                }));
            }
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            List<String> results = Collections.synchronizedList(new ArrayList<>());
            for (Future<String> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private void inTransaction(Runnable action) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
    }

    private void assertConflict(Runnable action, String code) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(code));
    }

    private void actAs(Long id, String role, Long unitId) {
        when(currentUser.get()).thenReturn(new Actor(id, role, unitId, false, false));
    }

    private Long leaderOf(Long roomId) {
        return jdbc.queryForObject(
                "SELECT leader_student_id FROM room WHERE id=?", Long.class, roomId);
    }

    private void insertOpenAssignment(Long student, Long contract, Long bed) {
        jdbc.update(
                "INSERT INTO room_assignment_history(student_id,contract_id,bed_id,kind,"
                        + "price_main,price_summer,created_by) VALUES(?,?,?,'HOLD',1,1,?)",
                student,
                contract,
                bed,
                staffId);
    }

    private Long createStudent(String gender) {
        Long id = createUser(Role.STUDENT, null);
        jdbc.update(
                "INSERT INTO student_profile(user_id,student_code,gender) VALUES (?,?,?)",
                id,
                "SV" + suffix + students.size(),
                gender);
        students.add(id);
        return id;
    }

    private Long createApplication(Long student, String status, Long preferredTypeId) {
        return jdbc.queryForObject(
                "INSERT INTO room_application(student_id,period_id,preferred_unit_id,"
                        + "preferred_room_type_id,status) VALUES(?,?,?,?,?) RETURNING id",
                Long.class,
                student,
                periodId,
                firstUnitId,
                preferredTypeId,
                status);
    }

    private Long createContract(Long student, String status) {
        Long application = createApplication(student, "ASSIGNED", null);
        boolean active = "ACTIVE".equals(status);
        return jdbc.queryForObject(
                "INSERT INTO dormitory_contract(code,student_id,application_id,start_date,"
                        + "end_date,status,activation_deadline,deposit_required,activated_at)"
                        + " VALUES(?,?,?,'2050-09-01','2051-06-30',?,now()+interval '7 days',?,?)"
                        + " RETURNING id",
                Long.class,
                "HD" + suffix + application,
                student,
                application,
                status,
                active ? 1000000L : null,
                active ? Timestamp.from(Instant.now()) : null);
    }

    private Long createUser(String role, Long unitId) {
        return jdbc.queryForObject(
                "INSERT INTO app_user(username,role,full_name,management_unit_id,"
                        + "must_change_password,keycloak_user_id) VALUES(?,?,?,?,false,?)"
                        + " RETURNING id",
                Long.class,
                role + "-" + UUID.randomUUID(),
                role,
                "Người dùng " + role,
                unitId,
                UUID.randomUUID().toString());
    }

    private Long createBuilding(Long unitId, String code) {
        return jdbc.queryForObject(
                "INSERT INTO building(management_unit_id,code,name,deposit_amount)"
                        + " VALUES(?,?,?,1000000) RETURNING id",
                Long.class,
                unitId,
                code,
                "Tòa " + code);
    }

    private Long createRoom(Long buildingId, String code, String gender) {
        return jdbc.queryForObject(
                "INSERT INTO room(building_id,room_type_id,floor,code,gender)"
                        + " VALUES(?,?,1,?,?) RETURNING id",
                Long.class,
                buildingId,
                roomTypeId,
                code,
                gender);
    }

    private Long createBed(Long roomId, String code) {
        return jdbc.queryForObject(
                "INSERT INTO bed(room_id,code) VALUES(?,?) RETURNING id",
                Long.class,
                roomId,
                code);
    }

    /** Dọn dữ liệu đã commit của case đồng thời, theo thứ tự khóa ngoại. */
    private void cleanUp() {
        List<Long> rooms = List.of(roomA, roomB, femaleRoom, otherUnitRoom);
        String roomIds = String.join(",", rooms.stream().map(String::valueOf).toList());
        jdbc.update(
                "UPDATE room SET leader_student_id=NULL WHERE id IN (" + roomIds + ")");
        jdbc.update(
                "DELETE FROM room_assignment_history WHERE bed_id IN (SELECT id FROM bed WHERE"
                        + " room_id IN ("
                        + roomIds
                        + "))");
        String contracts =
                "SELECT c.id FROM dormitory_contract c JOIN room_application a ON"
                        + " a.id=c.application_id WHERE a.period_id=?";
        jdbc.update(
                "DELETE FROM status_history WHERE entity_type='TRANSFER' AND entity_id IN (SELECT"
                        + " id FROM room_transfer_request WHERE contract_id IN ("
                        + contracts
                        + "))",
                periodId);
        jdbc.update(
                "DELETE FROM room_transfer_request WHERE contract_id IN (" + contracts + ")",
                periodId);
        jdbc.update(
                "DELETE FROM dormitory_contract WHERE application_id IN (SELECT id FROM"
                        + " room_application WHERE period_id=?)",
                periodId);
        jdbc.update("DELETE FROM room_application WHERE period_id=?", periodId);
        jdbc.update("DELETE FROM registration_period WHERE id=?", periodId);
        jdbc.update("DELETE FROM semester WHERE id=?", semesterId);
        jdbc.update("DELETE FROM bed WHERE room_id IN (" + roomIds + ")");
        jdbc.update("DELETE FROM room WHERE id IN (" + roomIds + ")");
        jdbc.update(
                "DELETE FROM building WHERE id IN (?, ?)", firstBuildingId, otherBuildingId);
        jdbc.update("DELETE FROM room_type WHERE id=?", roomTypeId);
        for (Long student : students) {
            jdbc.update("DELETE FROM student_profile WHERE user_id=?", student);
            jdbc.update("DELETE FROM app_user WHERE id=?", student);
        }
        jdbc.update("DELETE FROM app_user WHERE id=?", staffId);
    }
}
