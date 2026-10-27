package vn.edu.tlu.dms.contract.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import vn.edu.tlu.dms.common.constants.DmsConstants.AssignmentKind;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.Gender;
import vn.edu.tlu.dms.contract.domain.AssetCondition;
import vn.edu.tlu.dms.room.service.OccupancyQuery.CurrentBed;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Hợp đồng chi tiết (API 4.5) bổ sung các khối student, deposit, activatedBy, assignments,
 * pendingRequests, openStorageRecordId, moveOut. Bản danh sách phải giữ đúng 22 trường phẳng và để
 * trống các khối mới để không phát sinh truy vấn N+1 khi phân trang.
 */
class ContractDetailResponseTest {

    private static final Long CONTRACT_ID = 377L;

    private static final Long STUDENT_ID = 501L;

    private static final LocalDate START_DATE = LocalDate.of(2026, 8, 20);

    private static final LocalDate END_DATE = LocalDate.of(2027, 6, 30);

    private static final OffsetDateTime DEADLINE =
            OffsetDateTime.parse("2026-08-27T10:20:00+07:00");

    @Test
    void listResponseKeepsFlatFieldsAndLeavesDetailBlocksEmpty() {
        CurrentBed bed = currentBed();
        ContractDtos.Response list =
                new ContractDtos.Response(
                        CONTRACT_ID,
                        "HD-2026-0377",
                        STUDENT_ID,
                        900L,
                        START_DATE,
                        END_DATE,
                        ContractStatus.ACTIVE,
                        DEADLINE,
                        1_000_000L,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        "2251060001",
                        "Nguyễn Văn A",
                        List.of(),
                        314L,
                        950_000L,
                        bed,
                        null,
                        null,
                        null);

        assertThat(list.status()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(list.currentAssignment()).isSameAs(bed);
        assertThat(list.depositBalance()).isEqualTo(950_000L);
        assertThat(list.student()).isNull();
        assertThat(list.deposit()).isNull();
        assertThat(list.activatedBy()).isNull();
        assertThat(list.assignments()).isNull();
        assertThat(list.pendingRequests()).isNull();
        assertThat(list.openStorageRecordId()).isNull();
        assertThat(list.moveOut()).isNull();
    }

    @Test
    void detailResponseExposesEveryDocumentedBlock() {
        ContractDtos.StudentInfo student =
                new ContractDtos.StudentInfo(
                        STUDENT_ID, "2251060001", "Nguyễn Văn A", Gender.M);
        ContractDtos.DepositInfo deposit =
                new ContractDtos.DepositInfo(1_000_000L, 950_000L, 50_000L, false);
        ContractDtos.ActorInfo actor =
                new ContractDtos.ActorInfo(31L, "Trần Thị B", "staff.k1.b");
        ContractDtos.AssignmentInfo assignment =
                new ContractDtos.AssignmentInfo(
                        940L,
                        AssignmentKind.STAY,
                        new ContractDtos.BedInfo(
                                1532L, "K1-A101-03", 210L, "A101", 1L, "K1", 1, 1L),
                        OffsetDateTime.parse("2026-08-18T10:20:00+07:00"),
                        OffsetDateTime.parse("2026-08-21T09:00:00+07:00"),
                        null,
                        null,
                        1_800_000L,
                        900_000L);
        ContractDtos.PendingRequestsInfo pending =
                new ContractDtos.PendingRequestsInfo(null, 12L, null);
        ContractDtos.MoveOutInfo moveOut =
                new ContractDtos.MoveOutInfo(
                        77L,
                        OffsetDateTime.parse("2027-06-30T08:00:00+07:00"),
                        AssetCondition.OK.name(),
                        OffsetDateTime.parse("2027-06-30T08:00:00+07:00"));
        ContractDtos.Response detail = detailResponse(student, deposit, actor, pending, moveOut);

        assertThat(detail.student()).isEqualTo(student);
        assertThat(detail.deposit()).isEqualTo(deposit);
        assertThat(detail.activatedBy()).isEqualTo(actor);
        assertThat(detail.assignments()).containsExactly(assignment);
        assertThat(detail.pendingRequests()).isEqualTo(pending);
        assertThat(detail.openStorageRecordId()).isEqualTo(55L);
        assertThat(detail.moveOut()).isEqualTo(moveOut);
        assertThat(detail.assignments()).hasSize(1);
        assertThat(detail.assignments().get(0).bed().code()).isEqualTo("K1-A101-03");
        assertThat(detail.assignments().get(0).kind()).isEqualTo(AssignmentKind.STAY);
    }

    private static ContractDtos.Response detailResponse(
            ContractDtos.StudentInfo student,
            ContractDtos.DepositInfo deposit,
            ContractDtos.ActorInfo actor,
            ContractDtos.PendingRequestsInfo pending,
            ContractDtos.MoveOutInfo moveOut) {
        return new ContractDtos.Response(
                CONTRACT_ID,
                "HD-2026-0377",
                STUDENT_ID,
                900L,
                START_DATE,
                END_DATE,
                ContractStatus.ACTIVE,
                DEADLINE,
                1_000_000L,
                OffsetDateTime.parse("2026-08-21T09:00:00+07:00"),
                null,
                List.of(),
                List.of(),
                "2251060001",
                "Nguyễn Văn A",
                List.of(),
                314L,
                950_000L,
                currentBed(),
                null,
                12L,
                null,
                student,
                deposit,
                actor,
                List.of(assignment()),
                pending,
                55L,
                moveOut);
    }

    private static ContractDtos.AssignmentInfo assignment() {
        return new ContractDtos.AssignmentInfo(
                940L,
                AssignmentKind.STAY,
                new ContractDtos.BedInfo(1532L, "K1-A101-03", 210L, "A101", 1L, "K1", 1, 1L),
                OffsetDateTime.parse("2026-08-18T10:20:00+07:00"),
                OffsetDateTime.parse("2026-08-21T09:00:00+07:00"),
                null,
                null,
                1_800_000L,
                900_000L);
    }

    /** Giường hiện tại dạng phẳng của khung cũ; khối assignments dùng DTO lồng riêng. */
    private static CurrentBed currentBed() {
        return new CurrentBed(
                940L,
                STUDENT_ID,
                CONTRACT_ID,
                AssignmentKind.STAY,
                1532L,
                "03",
                210L,
                "A101",
                1L,
                "K1",
                1L);
    }
}
