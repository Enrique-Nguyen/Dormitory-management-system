package vn.edu.tlu.dms;

import java.util.Objects;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.edu.tlu.dms.account.repository.AppUserRepository;
import vn.edu.tlu.dms.account.repository.ManagementUnitRepository;
import vn.edu.tlu.dms.account.service.AuthService;
import vn.edu.tlu.dms.account.service.CatalogService;
import vn.edu.tlu.dms.account.service.IdentityAccountService;
import vn.edu.tlu.dms.account.service.impl.ManagementUnitServiceImpl;
import vn.edu.tlu.dms.account.service.impl.StudentManagementServiceImpl;
import vn.edu.tlu.dms.common.constants.DmsConstants.Gender;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.SemesterType;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.support.SqlPage;
import vn.edu.tlu.dms.registration.repository.PriorityCategoryRepository;
import vn.edu.tlu.dms.registration.service.impl.PriorityCategoryServiceImpl;
import vn.edu.tlu.dms.room.repository.RoomRepository;
import vn.edu.tlu.dms.room.repository.RoomTypeRepository;
import vn.edu.tlu.dms.room.repository.SemesterRepository;
import vn.edu.tlu.dms.room.service.impl.RoomTypeServiceImpl;
import vn.edu.tlu.dms.room.service.impl.SemesterServiceImpl;
import vn.edu.tlu.dms.support.FakeJdbc;
import vn.edu.tlu.dms.contract.repository.DormitoryContractRepository;
import vn.edu.tlu.dms.contract.service.impl.ContractServiceImpl;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.common.service.ConfigService;
import vn.edu.tlu.dms.contract.service.StorageService;
import vn.edu.tlu.dms.contract.service.SummerLeaveLookup;
import vn.edu.tlu.dms.room.service.BedOccupancyService;
import vn.edu.tlu.dms.room.service.OccupancyQuery;
import vn.edu.tlu.dms.room.service.RoomReferenceService;
import vn.edu.tlu.dms.billing.service.DepositService;
import vn.edu.tlu.dms.billing.repository.InvoiceRepository;
import vn.edu.tlu.dms.billing.service.impl.InvoiceServiceImpl;
import vn.edu.tlu.dms.maintenance.repository.MaintenanceRequestRepository;
import vn.edu.tlu.dms.maintenance.service.impl.MaintenanceRequestServiceImpl;
import org.springframework.data.jpa.repository.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Kiem tra bo loc, tham so bind va predicate dung chung cho du lieu/dem, khong can CSDL. */
// Mockito dùng null tạm cho matcher; ngoại lệ chỉ áp dụng ở các hàm giả lập bên dưới.
class CrudSearchFilterTest {

    private final ScopeGuard guard = mock(ScopeGuard.class);
    private final CurrentUser current = mock(CurrentUser.class);
    private final AuditService audit = mock(AuditService.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void newFilterRangesAreValidated() throws Exception {
        var json = new ObjectMapper().findAndRegisterModules();
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var valid = json.readValue("{\"unitPrice\":0,\"totalAmountMin\":5,\"totalAmountMax\":5,"
                + "\"effectiveFromFrom\":\"2026-01-01\",\"effectiveFromTo\":\"2026-01-01\","
                + "\"dueDateFrom\":\"2026-02-01\",\"dueDateTo\":\"2026-02-01\"}", SearchRequest.class);
        assertThat(validator.validate(valid)).isEmpty();
        var invalid = json.readValue("{\"unitPrice\":-1,\"totalAmountMin\":10,\"totalAmountMax\":5,"
                + "\"effectiveFromFrom\":\"2026-02-01\",\"effectiveFromTo\":\"2026-01-01\","
                + "\"dueDateFrom\":\"2026-02-01\",\"dueDateTo\":\"2026-01-01\"}", SearchRequest.class);
        assertThat(validator.validate(invalid)).hasSize(4);
    }

    @Test
    void periodTimeRangesAndDepositAreValidated() throws Exception {
        var json = new ObjectMapper().findAndRegisterModules();
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var valid = json.readValue("{\"depositAmount\":0,\"openAtFrom\":\"2026-01-01T00:00:00Z\","
                    + "\"openAtTo\":\"2026-01-01T00:00:00Z\",\"closeAtFrom\":\"2026-02-01T00:00:00Z\","
                    + "\"closeAtTo\":\"2026-02-01T00:00:00Z\"}", SearchRequest.class);
            assertThat(validator.validate(valid)).isEmpty();
            var invalid = json.readValue("{\"depositAmount\":-1,\"openAtFrom\":\"2026-02-01T00:00:00Z\","
                    + "\"openAtTo\":\"2026-01-01T00:00:00Z\",\"closeAtFrom\":\"2026-04-01T00:00:00Z\","
                    + "\"closeAtTo\":\"2026-03-01T00:00:00Z\"}", SearchRequest.class);
            assertThat(validator.validate(invalid)).extracting(v -> v.getPropertyPath().toString())
                    .containsExactlyInAnyOrder("depositAmount", "openAtRangeValid", "closeAtRangeValid");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void invoiceForwardsRangeFiltersAndActorScope(boolean mine) throws Exception {
        var repository = mock(InvoiceRepository.class);
        var rooms = mock(RoomReferenceService.class);
        var service = new InvoiceServiceImpl(repository, null, current, guard, null, null, null, null, audit, rooms);
        when(rooms.getSemesterLabels(any())).thenReturn(Map.of());
        when(current.get()).thenReturn(new Actor(10L, mine ? Role.STUDENT : Role.STAFF, 7L, false, false));
        when(repository.searchAccessibleInvoices(any(), any(), any(), anyBoolean(), anyBoolean(),
                org.mockito.ArgumentMatchers.isNull(), anyBoolean(), any(), any(),
                org.mockito.ArgumentMatchers.isNull(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Objects.requireNonNull(List.of())));
        SearchRequest input = new ObjectMapper().findAndRegisterModules().readValue(
                "{\"totalAmountMin\":0,\"totalAmountMax\":50,\"dueDateFrom\":\"2026-01-01\","
                        + "\"dueDateTo\":\"2026-01-31\",\"q\":\"A_%\"}", SearchRequest.class);
        service.searchByPage(input, mine);
        verify(repository).searchAccessibleInvoices(org.mockito.ArgumentMatchers.eq(mine ? Role.STUDENT : Role.STAFF),
                org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(false), org.mockito.ArgumentMatchers.eq(mine),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(false), any(),
                org.mockito.ArgumentMatchers.eq("%A\\_\\%%"), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(0L), org.mockito.ArgumentMatchers.eq(50L),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 1, 1)),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 1, 31)), any());
        verify(guard).requirePermission(Permission.INVOICE_READ);
    }

    @Test
    void invoiceOmittedRangesStayNullForCrossUnitStaff() {
        var repository = mock(InvoiceRepository.class);
        var rooms = mock(RoomReferenceService.class);
        var service = new InvoiceServiceImpl(repository, null, current, guard, null, null, null, null, audit, rooms);
        when(rooms.getSemesterLabels(any())).thenReturn(Map.of());
        when(current.get()).thenReturn(new Actor(10L, Role.STAFF, 7L, true, false));
        when(repository.searchAccessibleInvoices(any(), any(), any(), anyBoolean(), anyBoolean(),
                org.mockito.ArgumentMatchers.isNull(), anyBoolean(), any(), any(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), any())).thenReturn(new PageImpl<>(Objects.requireNonNull(List.of())));
        service.searchByPage(new SearchRequest(), false);
        verify(repository).searchAccessibleInvoices(org.mockito.ArgumentMatchers.eq(Role.STAFF),
                org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(true), org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(false), any(),
                org.mockito.ArgumentMatchers.eq("%%"), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), any());
    }

    @Test
    void invoiceCountAndDataQueriesShareScopeAndAllFilters() throws Exception {
        Query query = Stream.of(InvoiceRepository.class.getMethods())
                .filter(method -> method.getName().equals("searchAccessibleInvoices"))
                .findFirst().orElseThrow().getAnnotation(Query.class);
        assertThat(query.nativeQuery()).isTrue();
        assertThat(query.value().substring(query.value().indexOf(" WHERE ")))
                .isEqualTo(query.countQuery().substring(query.countQuery().indexOf(" WHERE ")))
                .contains(":role", ":crossUnit", ":unitId", ":mine", ":userId", ":totalAmountMin",
                        ":totalAmountMax", ":dueDateFrom", ":dueDateTo", ":billingMonth", ":q");
    }

    @Test
    void maintenanceForwardsCodeAndDescriptionWithoutReplacingStudentScope() {
        var repository = mock(MaintenanceRequestRepository.class);
        var rooms = mock(RoomReferenceService.class);
        var service = new MaintenanceRequestServiceImpl(repository, null, null, rooms, null, null, null,
                null, current, guard, null, null, audit, null, null, null, null);
        when(current.get()).thenReturn(new Actor(10L, Role.STUDENT, 7L, false, false));
        when(rooms.findCurrentStayingRoomId(10L)).thenReturn(java.util.Optional.of(99L));
        when(repository.searchVisibleRequests(any(), any(), any(), anyBoolean(), any(), anyBoolean(), any(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Objects.requireNonNull(List.of())));
        service.searchByPage(uncheckedRequest("{\"code\":\"A_%\",\"description\":\"pipe\",\"q\":\"leak\"}"));
        verify(repository).searchVisibleRequests(org.mockito.ArgumentMatchers.eq(Role.STUDENT),
                org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(false), org.mockito.ArgumentMatchers.eq(99L),
                org.mockito.ArgumentMatchers.eq(true), any(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq("%leak%"),
                org.mockito.ArgumentMatchers.eq("%A\\_\\%%"), org.mockito.ArgumentMatchers.eq("%pipe%"), any());
        verify(guard).requirePermission(Permission.MAINTENANCE_READ);
    }

    @ParameterizedTest
    @ValueSource(strings = {Role.STAFF, Role.TECHNICIAN, Role.ADMIN})
    void maintenanceOmittedFiltersPreserveActorScope(String role) {
        var repository = mock(MaintenanceRequestRepository.class);
        var service = new MaintenanceRequestServiceImpl(repository, null, null, null, null, null, null,
                null, current, guard, null, null, audit, null, null, null, null);
        when(current.get()).thenReturn(new Actor(10L, role, 7L, false, false));
        when(repository.searchVisibleRequests(any(), any(), any(), anyBoolean(), any(), anyBoolean(), any(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Objects.requireNonNull(List.of())));
        service.searchByPage(new SearchRequest());
        verify(repository).searchVisibleRequests(org.mockito.ArgumentMatchers.eq(role),
                org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(false), org.mockito.ArgumentMatchers.eq(-1L),
                org.mockito.ArgumentMatchers.eq(true), any(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq("%%"),
                org.mockito.ArgumentMatchers.eq("%%"), org.mockito.ArgumentMatchers.eq("%%"), any());
    }

    @Test
    void maintenanceCountAndDataQueriesShareCodeDescriptionAndRoleScope() {
        Query query = Stream.of(MaintenanceRequestRepository.class.getMethods())
                .filter(method -> method.getName().equals("searchVisibleRequests"))
                .findFirst().orElseThrow().getAnnotation(Query.class);
        assertThat(query.value().substring(query.value().indexOf("WHERE ")))
                .isEqualTo(query.countQuery().substring(query.countQuery().indexOf("WHERE ")))
                .contains(":role", ":userId", ":unitId", ":crossUnit", ":stayingRoomId",
                        "AND m.code ILIKE :code", "AND m.description ILIKE :description");
    }

    @Test
    void contractSearchForwardsFiltersAndAuthenticatedScope() throws Exception {
        var repository = mock(DormitoryContractRepository.class);
        var service = new ContractServiceImpl(repository, current, guard, mock(StatusHistoryService.class),
                mock(JdbcTemplate.class), mock(OccupancyQuery.class), mock(SummerLeaveLookup.class),
                mock(BedOccupancyService.class), mock(RoomReferenceService.class), mock(DepositService.class),
                mock(StorageService.class), mock(ConfigService.class), audit);
        var request = new ObjectMapper().findAndRegisterModules().readValue(
                "{\"studentId\":42,\"startDateFrom\":\"2026-01-01\",\"startDateTo\":\"2026-02-01\","
                + "\"endDateFrom\":\"2026-03-01\",\"endDateTo\":\"2026-04-01\",\"status\":\"PENDING\"}", SearchRequest.class);
        when(current.get()).thenReturn(new Actor(10L, Role.STUDENT, null, false, false));
        when(repository.searchAccessibleContracts(any(), any(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(false), org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.eq(false), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Objects.requireNonNull(List.of())));
        service.searchByPage(request, true);
        verify(repository).searchAccessibleContracts(org.mockito.ArgumentMatchers.eq(Role.STUDENT),
                org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(false), org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.eq(false), org.mockito.ArgumentMatchers.eq(List.of("PENDING")),
                org.mockito.ArgumentMatchers.eq("%%"), org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 1, 1)),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 2, 1)),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 3, 1)),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 4, 1)), any());
        verify(guard).requirePermission(Permission.CONTRACT_READ);
    }

    @Test
    void contractSearchValidatesIdsAndInclusiveDateRanges() throws Exception {
        ObjectMapper dates = new ObjectMapper().findAndRegisterModules();
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var valid = dates.readValue("{\"studentId\":1,\"startDateFrom\":\"2026-01-01\","
                    + "\"startDateTo\":\"2026-01-01\",\"endDateFrom\":\"2026-02-01\","
                    + "\"endDateTo\":\"2026-02-01\"}", SearchRequest.class);
            assertThat(validator.validate(valid)).isEmpty();
            var invalid = dates.readValue("{\"studentId\":0,\"startDateFrom\":\"2026-02-01\","
                    + "\"startDateTo\":\"2026-01-01\",\"endDateFrom\":\"2026-04-01\","
                    + "\"endDateTo\":\"2026-03-01\"}", SearchRequest.class);
            assertThat(validator.validate(invalid)).extracting(v -> v.getPropertyPath().toString())
                    .containsExactlyInAnyOrder("studentId", "startDateRangeValid", "endDateRangeValid");
            assertThat(validator.validate(dates.readValue("{}", SearchRequest.class))).isEmpty();
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> dates.readValue(
                    "{\"startDateFrom\":\"not-a-date\"}", SearchRequest.class))
                    .isInstanceOf(com.fasterxml.jackson.databind.JsonMappingException.class);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void studentFiltersPreserveStaffScopeInBothCountAndData(boolean locked) throws Exception {
        when(current.get()).thenReturn(new Actor(10L, Role.STAFF, 7L, false, false));
        FakeJdbc jdbc = studentJdbc();
        var service = students(jdbc);
        SearchRequest input = mapper.convertValue(
                Map.of("page", 1, "size", 10, "q", " O'Brien ", "gender", Gender.F,
                        "facultyId", 17L, "cohortId", 65L, "locked", locked,
                        "managementUnitId", 9L), SearchRequest.class);

        var page = service.searchByPage(input);

        verify(guard).requirePermission(Permission.STUDENT_READ);
        assertThat(page.totalElements()).isEqualTo(31);
        assertThat(page.totalPages()).isEqualTo(4);
        assertThat(jdbc.statements()).hasSize(2);
        String count = jdbc.statements().get(0);
        String data = jdbc.statements().get(1);
        String expectedWhere = " WHERE " + unitScope()
                + " AND (u.full_name ILIKE ? OR p.student_code ILIKE ?)"
                + " AND p.gender=? AND p.faculty_id=? AND p.cohort_id=? AND u.locked=?"
                + " AND " + unitScope();
        String expectedArgs = " | 7 | 7 | %O'Brien% | %O'Brien% | " + Gender.F
                + " | 17 | 65 | " + locked + " | 9 | 9";
        assertThat(count).contains(expectedWhere).endsWith(expectedArgs);
        assertThat(data).contains(expectedWhere).endsWith(expectedArgs + " | 10 | 10");
        assertThat(count.substring(0, count.indexOf(" | ")))
                .doesNotContain("O'Brien", "Science", "K65");
        assertThat(jdbc.writes()).isEmpty();
    }

    @Test
    void adminStudentUnitFilterUsesHistoryOrPreferredUnitWithoutAddingStaffScope() throws Exception {
        when(current.get()).thenReturn(new Actor(10L, Role.ADMIN, null, false, false));
        FakeJdbc jdbc = studentJdbc();
        students(jdbc).searchByPage(request("{\"managementUnitId\":9}"));

        for (String sql : jdbc.statements()) {
            assertThat(sql).contains(" WHERE TRUE AND (u.full_name ILIKE ? OR p.student_code ILIKE ?)"
                    + " AND " + unitScope());
            assertThat(sql).contains(" | %% | %% | 9 | 9");
        }
    }

    @Test
    void blankStudentTextFiltersAreIgnoredAndOmittedLockDoesNotBecomeFalse() throws Exception {
        when(current.get()).thenReturn(new Actor(10L, Role.ADMIN, null, false, false));
        FakeJdbc jdbc = studentJdbc();
        students(jdbc).searchByPage(request("{\"gender\":\" \",\"facultyId\":null,\"cohortId\":null}"));

        for (String sql : jdbc.statements()) {
            assertThat(sql).doesNotContain("p.gender=?", "p.faculty_id=?", "p.cohort_id=?",
                    "u.locked=?", "preferred_unit_id=?");
        }
    }

    @Test
    void managementUnitCodeIsExactAndCombinedWithKeyword() throws Exception {
        var repository = mock(ManagementUnitRepository.class);
        var service = new ManagementUnitServiceImpl(repository, guard, current, audit);
        List<String> predicates = pagePredicates(repository,
                () -> service.searchByPage(uncheckedRequest("{\"code\":\" K1 \",\"q\":\"needle\",\"page\":1,\"size\":10}")));

        assertThat(predicates).allSatisfy(predicate -> assertThat(predicate)
                .contains("code=K1", "lower(code) LIKE %needle%", "lower(name) LIKE %needle%", " AND ", " OR ")
                .doesNotContain("code LIKE"));
        verify(guard).requirePermission(Permission.MANAGEMENT_UNIT_READ);
    }

    @Test
    void namesAndBuildingFiltersCombineWithScopeAndPaging() {
        var units = mock(ManagementUnitRepository.class);
        var unitService = new ManagementUnitServiceImpl(units, guard, current, audit);
        assertThat(pagePredicates(units, () -> unitService.searchByPage(uncheckedRequest(
                "{\"code\":\"K1\",\"name\":\" Unit \" ,\"page\":1,\"size\":10}"))))
                .allSatisfy(p -> assertThat(p).contains("code=K1", "locate(lower(name),unit)>0"));

        var types = mock(RoomTypeRepository.class);
        var typeService = new RoomTypeServiceImpl(types, mock(RoomRepository.class), guard, current, audit, mock(CatalogService.class));
        assertThat(pagePredicates(types, () -> typeService.searchByPage(uncheckedRequest(
                "{\"name\":\" Suite \" ,\"active\":false,\"page\":1,\"size\":10}"))))
                .allSatisfy(p -> assertThat(p).contains("locate(lower(name),suite)>0", "active=false"));

        var buildings = mock(vn.edu.tlu.dms.room.repository.BuildingRepository.class);
        var buildingService = new vn.edu.tlu.dms.room.service.impl.BuildingServiceImpl(
                buildings, guard, current, audit, mock(vn.edu.tlu.dms.account.service.AccountReferenceService.class));
        when(current.get()).thenReturn(new Actor(10L, Role.STAFF, 7L, false, false));
        assertThat(pagePredicates(buildings, () -> buildingService.searchByPage(uncheckedRequest(
                "{\"managementUnitId\":9,\"code\":\" Ab \" ,\"name\":\" Hall \" ,\"depositAmount\":0,\"page\":1,\"size\":10}"))))
                .allSatisfy(p -> assertThat(p).contains("managementUnitId=7", "managementUnitId=9",
                        "locate(lower(code),ab)>0", "locate(lower(name),hall)>0", "depositAmount=0"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void roomTypeActiveFilterPreservesFalseAndKeyword(boolean active) {
        var repository = mock(RoomTypeRepository.class);
        var service = new RoomTypeServiceImpl(repository, mock(RoomRepository.class), guard, current, audit, mock(CatalogService.class));
        List<String> predicates = pagePredicates(repository,
                () -> service.searchByPage(uncheckedRequest("{\"active\":" + active
                        + ",\"q\":\"needle\",\"page\":1,\"size\":10}")));

        assertThat(predicates).allSatisfy(predicate -> assertThat(predicate)
                .contains("active=" + active, "lower(name) LIKE %needle%", " AND "));
        verify(guard).requirePermission(Permission.ROOM_TYPE_READ);
    }

    @Test
    void roomTypeNumericFiltersCombineWithActiveAndKeywordForDataAndCount() {
        var repository = mock(RoomTypeRepository.class);
        var service = new RoomTypeServiceImpl(repository, mock(RoomRepository.class), guard, current, audit, mock(CatalogService.class));
        List<String> predicates = pagePredicates(repository,
                () -> service.searchByPage(uncheckedRequest("{\"bedCount\":6,\"priceMain\":1000000,"
                        + "\"priceSummer\":0,\"active\":false,\"q\":\"needle\",\"page\":1,\"size\":10}")));

        assertThat(predicates).allSatisfy(predicate -> assertThat(predicate)
                .contains("bedCount=6", "priceMain=1000000", "priceSummer=0", "active=false",
                        "lower(name) LIKE %needle%", " AND "));
        verify(guard).requirePermission(Permission.ROOM_TYPE_READ);
    }

    @Test
    void roomTypeNumericFiltersAreOptionalAndIndependent() {
        for (String field : List.of("bedCount", "priceMain", "priceSummer")) {
            var repository = mock(RoomTypeRepository.class);
            var service = new RoomTypeServiceImpl(repository, mock(RoomRepository.class), guard, current, audit, mock(CatalogService.class));
            List<String> predicates = pagePredicates(repository,
                    () -> service.searchByPage(uncheckedRequest("{\"" + field + "\":0,\"page\":1,\"size\":10}")));
            assertThat(predicates).allSatisfy(predicate -> {
                assertThat(predicate).contains(field + "=0");
                for (String other : List.of("bedCount", "priceMain", "priceSummer")) {
                    if (!other.equals(field)) {
                        assertThat(predicate).doesNotContain(other + "=");
                    }
                }
            });
        }
    }

    @Test
    void roomTypeNumericFiltersRejectNegativeValuesAndAcceptZero() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(request("{\"bedCount\":0,\"priceMain\":0,\"priceSummer\":0}"))).isEmpty();
            assertThat(validator.validate(request("{\"bedCount\":-1,\"priceMain\":-1,\"priceSummer\":-1}")))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactlyInAnyOrder("bedCount", "priceMain", "priceSummer");
            assertThat(validator.validate(request("{}"))).isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void priorityActiveFilterCannotBroadenStudentOrTechnicianScope(boolean active) {
        for (String role : List.of(Role.STUDENT, Role.TECHNICIAN)) {
            when(current.get()).thenReturn(new Actor(10L, role, 7L, false, false));
            var repository = mock(PriorityCategoryRepository.class);
            var service = new PriorityCategoryServiceImpl(repository, guard, current, audit);
            List<String> predicates = pagePredicates(repository,
                    () -> service.searchByPage(uncheckedRequest("{\"active\":" + active
                            + ",\"q\":\"needle\",\"page\":1,\"size\":10}")));

            assertThat(predicates).allSatisfy(predicate -> assertThat(predicate)
                    .contains("(active IS TRUE AND active=" + active + ")",
                            "lower(code) LIKE %needle%", "lower(name) LIKE %needle%"));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void priorityAdminCanFilterEitherActiveState(boolean active) {
        when(current.get()).thenReturn(new Actor(10L, Role.ADMIN, null, false, false));
        var repository = mock(PriorityCategoryRepository.class);
        var service = new PriorityCategoryServiceImpl(repository, guard, current, audit);
        List<String> predicates = pagePredicates(repository,
                () -> service.searchByPage(uncheckedRequest("{\"active\":" + active + ",\"page\":1,\"size\":10}")));

        assertThat(predicates).allSatisfy(predicate -> assertThat(predicate)
                .contains("active=" + active).doesNotContain("active IS TRUE"));
        verify(guard).requirePermission(Permission.PRIORITY_CATEGORY_READ);
    }

    @Test
    void priorityCodeNameAndZeroWeightCombineForAdminCountAndData() {
        when(current.get()).thenReturn(new Actor(10L, Role.ADMIN, null, false, false));
        var repository = mock(PriorityCategoryRepository.class);
        var service = new PriorityCategoryServiceImpl(repository, guard, current, audit);
        List<String> predicates = pagePredicates(repository, () -> service.searchByPage(uncheckedRequest(
                "{\"code\":\" AB \",\"name\":\" Name \",\"weight\":0,\"active\":false,\"q\":\"needle\",\"page\":1,\"size\":10}")));
        assertThat(predicates).allSatisfy(predicate -> assertThat(predicate)
                .contains("locate(lower(code),ab)>0", "locate(lower(name),name)>0", "weight=0",
                        "active=false", "lower(code) LIKE %needle%", " AND "));
        verify(guard).requirePermission(Permission.PRIORITY_CATEGORY_READ);
    }

    @SuppressWarnings("null")
    @ParameterizedTest
    @ValueSource(strings = {Role.STUDENT, Role.TECHNICIAN})
    void priorityWeightFilterIsForbiddenForLimitedRolesEvenAtZero(String role) {
        when(current.get()).thenReturn(new Actor(10L, role, 7L, false, false));
        var repository = mock(PriorityCategoryRepository.class);
        var service = new PriorityCategoryServiceImpl(repository, guard, current, audit);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.searchByPage(
                uncheckedRequest("{\"weight\":0}")))
                .isInstanceOf(vn.edu.tlu.dms.common.exception.BusinessException.class);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).findAll(org.mockito.ArgumentMatchers.<Specification<vn.edu.tlu.dms.registration.domain.PriorityCategory>>argThat(specification -> specification != null), any(Pageable.class));
    }

    @Test
    void omittedPriorityActiveFilterStillRestrictsStudentLookup() {
        when(current.get()).thenReturn(new Actor(10L, Role.STUDENT, null, false, false));
        var repository = mock(PriorityCategoryRepository.class);
        var service = new PriorityCategoryServiceImpl(repository, guard, current, audit);
        List<String> predicates = pagePredicates(repository,
                () -> service.searchByPage(uncheckedRequest("{\"page\":1,\"size\":10}")));

        assertThat(predicates).allSatisfy(predicate -> assertThat(predicate)
                .contains("active IS TRUE").doesNotContain("active="));
    }

    @SuppressWarnings("null")
    @Test
    void registrationPeriodFiltersKeepOpenStudentScope() throws Exception {
        var repository = mock(vn.edu.tlu.dms.registration.repository.RegistrationPeriodRepository.class);
        var service = new vn.edu.tlu.dms.registration.service.impl.RegistrationPeriodServiceImpl(
                repository, null, null, null, null, current, guard, null, null, audit);
        when(current.get()).thenReturn(new Actor(10L, Role.STUDENT, null, false, false));
        var captured = new AtomicReference<Specification<vn.edu.tlu.dms.registration.domain.RegistrationPeriod>>();
        when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<vn.edu.tlu.dms.registration.domain.RegistrationPeriod>>argThat(specification -> specification != null), any(Pageable.class))).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return new PageImpl<>(Objects.requireNonNull(List.of()), Objects.requireNonNull(invocation.getArgument(1)), 31);
        });
        var input = new ObjectMapper().findAndRegisterModules().readValue(
                "{\"name\":\" Fall \" ,\"semesterId\":3,\"open\":false,\"openAtFrom\":\"2026-01-01T00:00:00Z\","
                        + "\"openAtTo\":\"2026-01-31T00:00:00Z\",\"closeAtFrom\":\"2026-02-01T00:00:00Z\","
                        + "\"closeAtTo\":\"2026-02-28T00:00:00Z\",\"page\":1,\"size\":10}", SearchRequest.class);
        var page = service.searchByPage(input, true);
        assertThat(page.totalElements()).isEqualTo(31);
        for (int i = 0; i < 2; i++) {
            String predicate = new CriteriaTrace<vn.edu.tlu.dms.registration.domain.RegistrationPeriod>()
                    .describe(captured.get(), mock(CriteriaQuery.class));
            assertThat(predicate).contains("semesterId=3", "locate(lower(name),fall)>0",
                    "openAt>=2026-01-01T00:00Z", "openAt<=2026-01-31T00:00Z",
                    "closeAt>=2026-02-01T00:00Z", "closeAt<=2026-02-28T00:00Z", "closeAt>");
        }
    }

    @Test
    void floorFiltersAreBoundBeforeDataAndCountWithStaffScope() {
        when(current.get()).thenReturn(new Actor(10L, Role.STAFF, 7L, false, false));
        FakeJdbc jdbc = new FakeJdbc().returningOne("SELECT count(*)", Map.of("count", 0L))
                .returning("SELECT * FROM", List.of());
        var service = new vn.edu.tlu.dms.room.service.impl.FloorServiceImpl(jdbc, new SqlPage(jdbc), current, guard, audit);
        service.searchByPage(uncheckedRequest("{\"buildingId\":9,\"floor\":0,\"name\":\" A_% \" ,\"active\":false}"));
        assertThat(jdbc.statements()).hasSize(2).allSatisfy(sql -> assertThat(sql)
                .contains("b.management_unit_id=?", "f.building_id=?", "f.floor=?",
                        "strpos(lower(f.name),lower(?))>0", "f.active=?", " | 7 | 9 | 0 | A_% | false"));
    }

    @Test
    void semesterAcademicYearIsExactAndCombinedWithTypeAndKeyword() {
        var repository = mock(SemesterRepository.class);
        var service = new SemesterServiceImpl(repository, guard, current, audit, mock(CatalogService.class));
        var input = mapper.convertValue(Map.of("academicYear", "2026-2027", "type", SemesterType.HK1,
                "q", "needle", "page", 1, "size", 10), SearchRequest.class);
        List<String> predicates = pagePredicates(repository, () -> service.searchByPage(input));

        assertThat(predicates).allSatisfy(predicate -> assertThat(predicate)
                .contains("(type=" + SemesterType.HK1 + " AND academicYear=2026-2027)",
                        "lower(name) LIKE %needle%", "lower(academicYear) LIKE %needle%"));
        verify(guard).requirePermission(Permission.SEMESTER_READ);
    }

    @Test
    void semesterNameAndDateBoundsCombineBeforePaging() throws Exception {
        var repository = mock(SemesterRepository.class);
        var service = new SemesterServiceImpl(repository, guard, current, audit, mock(CatalogService.class));
        var input = new ObjectMapper().findAndRegisterModules().readValue(
                "{\"name\":\" Fall \" ,\"startDateFrom\":\"2026-01-01\",\"startDateTo\":\"2026-01-31\","
                        + "\"endDateFrom\":\"2026-05-01\",\"endDateTo\":\"2026-05-31\",\"page\":1,\"size\":10}", SearchRequest.class);
        assertThat(pagePredicates(repository, () -> service.searchByPage(input)))
                .allSatisfy(p -> assertThat(p).contains("locate(lower(name),fall)>0", "startDate>=2026-01-01",
                        "startDate<=2026-01-31", "endDate>=2026-05-01", "endDate<=2026-05-31"));
    }

    @Test
    void omittedCatalogueFiltersDoNotAddUnrequestedPredicates() {
        SearchRequest input = uncheckedRequest("{\"page\":1,\"size\":10}");
        var units = mock(ManagementUnitRepository.class);
        var roomTypes = mock(RoomTypeRepository.class);
        var semesters = mock(SemesterRepository.class);
        var priorities = mock(PriorityCategoryRepository.class);
        when(current.get()).thenReturn(new Actor(10L, Role.ADMIN, null, false, false));
        assertThat(pagePredicates(units,
                () -> new ManagementUnitServiceImpl(units, guard, current, audit).searchByPage(input)))
                .containsOnly("(TRUE AND TRUE)");
        assertThat(pagePredicates(roomTypes,
                () -> new RoomTypeServiceImpl(roomTypes, mock(RoomRepository.class), guard, current, audit, mock(CatalogService.class)).searchByPage(input)))
                .containsOnly("(TRUE AND TRUE)");
        assertThat(pagePredicates(semesters,
                () -> new SemesterServiceImpl(semesters, guard, current, audit, mock(CatalogService.class)).searchByPage(input)))
                .containsOnly("(TRUE AND TRUE)");
        assertThat(pagePredicates(priorities,
                () -> new PriorityCategoryServiceImpl(priorities, guard, current, audit).searchByPage(input)))
                .containsOnly("(TRUE AND TRUE)");
    }

    private FakeJdbc studentJdbc() {
        return new FakeJdbc().returningOne("SELECT count(*)", Map.of("count", 31L))
                .returning("SELECT * FROM", List.of());
    }

    private StudentManagementServiceImpl students(FakeJdbc jdbc) {
        return new StudentManagementServiceImpl(jdbc, new SqlPage(jdbc), current, guard, audit,
                mock(AppUserRepository.class), mock(AuthService.class), mock(IdentityAccountService.class),
                mock(CatalogService.class));
    }

    private static String unitScope() {
        return "(EXISTS (SELECT 1 FROM room_assignment_history a JOIN bed b ON b.id=a.bed_id JOIN"
                + " room r ON r.id=b.room_id JOIN building bd ON bd.id=r.building_id WHERE"
                + " a.student_id=u.id AND bd.management_unit_id=?) OR EXISTS (SELECT 1 FROM"
                + " room_application a WHERE a.student_id=u.id AND a.preferred_unit_id=?))";
    }

    private SearchRequest request(String json) throws Exception {
        return mapper.readValue(json, SearchRequest.class);
    }

    private SearchRequest uncheckedRequest(String json) {
        try {
            return request(json);
        } catch (Exception exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    /** Lay Specification that truyen vao repository, danh gia cho ca query du lieu va query dem. */
    @SuppressWarnings({"unchecked", "null"})
    private static <E, R extends JpaRepository<E, Long> & JpaSpecificationExecutor<E>>
            List<String> pagePredicates(R repository, Supplier<PageResponse<?>> search) {
        var captured = new AtomicReference<Specification<E>>();
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    captured.set(invocation.getArgument(0));
                    Pageable pageable = invocation.getArgument(1);
                    return new PageImpl<E>(Objects.requireNonNull(List.of()), Objects.requireNonNull(pageable), 31);
                });
        PageResponse<?> page = search.get();
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.totalElements()).isEqualTo(31);
        assertThat(page.totalPages()).isEqualTo(4);
        assertThat(captured.get()).isNotNull();
        return List.of(new CriteriaTrace<E>().describe(captured.get(), mock(CriteriaQuery.class)),
                new CriteriaTrace<E>().describe(captured.get(), mock(CriteriaQuery.class)));
    }

    /** Criteria gia chi ghi bieu thuc; khong gia lap ket qua loc hay hanh vi cua JPA provider. */
    private static final class CriteriaTrace<E> {
        private final Map<Expression<?>, String> labels = new IdentityHashMap<>();
        private final CriteriaBuilder builder = mock(CriteriaBuilder.class);
        @SuppressWarnings("unchecked")
        private final Root<E> root = mock(Root.class);

        @SuppressWarnings("unchecked")
        private CriteriaTrace() {
            when(root.get(anyString())).thenAnswer(invocation -> {
                Path<Object> path = mock(Path.class);
                labels.put(path, invocation.getArgument(0));
                return path;
            });
            when(builder.conjunction()).thenAnswer(invocation -> node("TRUE"));
            when(builder.equal(any(Expression.class), any(Object.class))).thenAnswer(invocation ->
                    node(labels.get(invocation.getArgument(0)) + "=" + invocation.getArgument(1)));
            when(builder.isTrue(any(Expression.class))).thenAnswer(invocation ->
                    node(labels.get(invocation.getArgument(0)) + " IS TRUE"));
            when(builder.lower(any(Expression.class))).thenAnswer(invocation -> {
                Expression<String> expression = mock(Expression.class);
                labels.put(expression, "lower(" + labels.get(invocation.getArgument(0)) + ")");
                return expression;
            });
            when(builder.locate(any(Expression.class), anyString())).thenAnswer(invocation -> {
                Expression<Integer> expression = mock(Expression.class);
                labels.put(expression, "locate(" + labels.get(invocation.getArgument(0)) + ","
                        + invocation.getArgument(1) + ")");
                return expression;
            });
            when(builder.greaterThan(any(Expression.class), any(Comparable.class)))
                    .thenAnswer(invocation -> node(labels.get(invocation.getArgument(0)) + ">" + invocation.getArgument(1)));
            when(builder.greaterThan(any(Expression.class), org.mockito.ArgumentMatchers.eq(0)))
                    .thenAnswer(invocation -> node(labels.get(invocation.getArgument(0)) + ">0"));
            when(builder.greaterThanOrEqualTo(any(Expression.class), any(Comparable.class)))
                    .thenAnswer(invocation -> node(labels.get(invocation.getArgument(0)) + ">=" + invocation.getArgument(1)));
            when(builder.lessThanOrEqualTo(any(Expression.class), any(Comparable.class)))
                    .thenAnswer(invocation -> node(labels.get(invocation.getArgument(0)) + "<=" + invocation.getArgument(1)));
            when(builder.like(any(Expression.class), anyString())).thenAnswer(invocation ->
                    node(labels.get(invocation.getArgument(0)) + " LIKE " + invocation.getArgument(1)));
            when(builder.or(any(Predicate[].class))).thenAnswer(invocation -> {
                String text = Stream.of(invocation.getArguments())
                        .map(labels::get).collect(Collectors.joining(" OR ", "(", ")"));
                return node(text);
            });
            when(builder.and(any(Predicate[].class))).thenAnswer(invocation -> {
                String text = Stream.of(invocation.getArguments())
                        .map(labels::get).collect(Collectors.joining(" AND ", "(", ")"));
                return node(text);
            });
            when(builder.and(any(Expression.class), any(Expression.class))).thenAnswer(invocation ->
                    node("(" + labels.get(invocation.getArgument(0)) + " AND "
                            + labels.get(invocation.getArgument(1)) + ")"));
        }

        private Predicate node(String text) {
            Predicate predicate = mock(Predicate.class);
            labels.put(predicate, text);
            return predicate;
        }

        private String describe(Specification<E> specification, CriteriaQuery<?> query) {
            Predicate predicate = specification.toPredicate(Objects.requireNonNull(root), query, Objects.requireNonNull(builder));
            assertThat(predicate).isNotNull();
            return labels.get(predicate);
        }
    }
}
