package vn.edu.tlu.dms;

import java.util.Objects;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import vn.edu.tlu.dms.billing.repository.DepositTransactionRepository;
import vn.edu.tlu.dms.billing.repository.UtilityRateRepository;
import vn.edu.tlu.dms.billing.service.impl.UtilityRateServiceImpl;
import vn.edu.tlu.dms.common.service.StatusHistoryService;
import vn.edu.tlu.dms.contract.service.ContractService;
import vn.edu.tlu.dms.registration.service.impl.SummerCrudServiceImpl;
import vn.edu.tlu.dms.billing.service.impl.DepositServiceImpl;
import vn.edu.tlu.dms.common.constants.DmsConstants.ContractStatus;
import vn.edu.tlu.dms.common.constants.DmsConstants.NotificationScope;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.domain.SystemConfig;
import vn.edu.tlu.dms.common.dto.PageResponse;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.repository.SystemConfigRepository;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.impl.ConfigServiceImpl;
import vn.edu.tlu.dms.common.support.SqlPage;
import vn.edu.tlu.dms.notification.repository.NotificationRepository;
import vn.edu.tlu.dms.notification.service.impl.NotificationCrudServiceImpl;
import vn.edu.tlu.dms.notification.service.impl.OverviewServiceImpl;
import vn.edu.tlu.dms.room.service.RoomReferenceService;
import vn.edu.tlu.dms.room.service.FloorService;
import vn.edu.tlu.dms.support.FakeJdbc;

import java.util.List;
import java.util.Map;

// Mockito dùng null tạm cho matcher; ngoại lệ chỉ áp dụng ở các hàm giả lập bên dưới.

class CrudOperationalFilterTest {

    private static final Long ACTOR_ID = 20L;
    private static final Long UNIT_ID = 3L;
    private static final Long CONTRACT_ID = 91L;
    private static final String KEYWORD = "HD_10%";
    private static final String KEYWORD_PATTERN = "%HD\\_10\\%%";

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void storageBindsReturnStateContractAndKeywordBeforeCountAndPaging(boolean returned) {
        Fixture fixture = new Fixture();
        SearchRequest input = request(Map.of(
                "returned", returned,
                "contractId", CONTRACT_ID,
                "q", KEYWORD,
                "page", 1,
                "size", 5));

        var result = fixture.overview().searchStorageByPage(input, false);

        fixture.assertPagedQuery(
                " | " + UNIT_ID + " | " + returned + " | " + CONTRACT_ID
                        + " | " + KEYWORD_PATTERN + " | " + KEYWORD_PATTERN,
                input);
        assertThat(fixture.countSql()).contains(
                "ap.preferred_unit_id=? AND (s.returned_at IS NOT NULL)=?",
                "AND s.contract_id=?",
                "AND (u.full_name ILIKE ? OR c.code ILIKE ?)");
        assertThat(fixture.countSql()).doesNotContain(KEYWORD, CONTRACT_ID.toString());
        assertThat(result.totalElements()).isEqualTo(7);
        assertThat(result.totalPages()).isEqualTo(2);
        verify(fixture.guard).requirePermission(Permission.STORAGE_READ);
    }

    @Test
    void storageColumnFiltersAreBoundBeforeCountAndPagination() {
        Fixture fixture = new Fixture();
        SearchRequest input = request(Map.ofEntries(
                Map.entry("id", 15), Map.entry("packageCount", 2),
                Map.entry("location", "A_%"), Map.entry("description", "box'"),
                Map.entry("depositedFrom", "2026-01-01"), Map.entry("depositedTo", "2026-01-31"),
                Map.entry("returnedFrom", "2026-02-01"), Map.entry("returnedTo", "2026-02-28"),
                Map.entry("page", 1), Map.entry("size", 5)));

        fixture.overview().searchStorageByPage(input, false);

        assertThat(fixture.countSql()).contains(
                "ap.preferred_unit_id=?", "s.id=?", "s.package_count=?", "s.location ILIKE ?",
                "s.description ILIKE ?", "s.deposited_at>=?", "s.deposited_at<=?",
                "s.returned_at>=?", "s.returned_at<=?").doesNotContain("box'", "A_%");
        fixture.assertPagedQuery(" | " + UNIT_ID + " | 15 | 2 | %A\\_\\%% | %box'%"
                + " | 2026-01-01 | 2026-01-31 | 2026-02-01 | 2026-02-28 | %% | %%", input);
    }

    @Test
    void storageFilterValidationRejectsInvalidValuesAndDateRanges() {
        SearchRequest input = request(Map.of(
                "id", 0, "packageCount", -1, "location", "x".repeat(101),
                "description", "x".repeat(501), "depositedFrom", "2026-02-01",
                "depositedTo", "2026-01-01", "returnedFrom", "2026-03-01",
                "returnedTo", "2026-02-01"));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(input)).hasSize(6);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void storageStudentScopeCannotBeReplacedByContractFilter(boolean mine) {
        Fixture fixture = new Fixture(new Actor(ACTOR_ID, Role.STUDENT, null, false, false));
        SearchRequest input = request(Map.of("contractId", CONTRACT_ID, "id", 15,
                "returnedFrom", "2026-02-01"));

        fixture.overview().searchStorageByPage(input, mine);

        assertThat(fixture.countSql()).contains("WHERE c.student_id=? AND s.contract_id=?",
                "s.id=?", "s.returned_at>=?");
        fixture.assertPagedQuery(" | " + ACTOR_ID + " | " + CONTRACT_ID
                + " | 15 | 2026-02-01 | %% | %%", input);
    }

    @Test
    void storageWithoutOperationalFiltersKeepsExistingScope() {
        Fixture fixture = new Fixture();
        SearchRequest input = new SearchRequest();

        fixture.overview().searchStorageByPage(input, false);

        assertThat(fixture.countSql()).contains("WHERE ap.preferred_unit_id=?");
        assertThat(fixture.countSql()).doesNotContain("returned_at IS NOT NULL", "AND s.contract_id=?");
        fixture.assertPagedQuery(" | " + UNIT_ID + " | %% | %%", input);
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    void bothDepositListsBindAllStatusesAndProjectedTopupState(boolean overview, boolean hasTopup) {
        Fixture fixture = new Fixture();
        SearchRequest input = request(Map.of(
                "status", List.of(ContractStatus.ACTIVE, ContractStatus.PENDING),
                "hasTopup", hasTopup,
                "q", KEYWORD));

        fixture.searchDeposits(input, overview, false);

        assertThat(fixture.countSql()).contains("WHERE ap.preferred_unit_id");
        assertThat(fixture.countSql()).containsIgnoringCase(
                "AND c.status IN (?,?) AND (coalesce(v.topup_needed,0)>0)=?");
        assertThat(fixture.countSql()).contains("AND (u.full_name ILIKE ? OR c.code ILIKE ?)");
        assertThat(fixture.countSql()).doesNotContain(ContractStatus.ACTIVE, KEYWORD);
        fixture.assertPagedQuery(
                " | " + UNIT_ID + " | " + ContractStatus.ACTIVE + " | " + ContractStatus.PENDING
                        + " | " + hasTopup + " | " + KEYWORD_PATTERN + " | " + KEYWORD_PATTERN,
                input);
        verify(fixture.guard).requirePermission(Permission.DEPOSIT_READ);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void bothDepositListsAcceptScalarStatusAndKeepMineScope(boolean overview) {
        Fixture fixture = new Fixture(new Actor(ACTOR_ID, Role.STAFF, UNIT_ID, true, false));
        SearchRequest input = request(Map.of("status", ContractStatus.EXPIRED));

        fixture.searchDeposits(input, overview, true);

        assertThat(fixture.countSql()).contains("WHERE c.student_id", "AND c.status IN (?)");
        assertThat(fixture.countSql()).doesNotContain("topup_needed,0)>0");
        fixture.assertPagedQuery(" | " + ACTOR_ID + " | " + ContractStatus.EXPIRED + " | %% | %%", input);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void bothDepositListsOmitEmptyStatusesAndAbsentTopupFilter(boolean overview) {
        Fixture fixture = new Fixture();
        SearchRequest input = request(Map.of("status", List.of()));

        fixture.searchDeposits(input, overview, false);

        assertThat(fixture.countSql()).doesNotContain("c.status IN", "topup_needed,0)>0");
        fixture.assertPagedQuery(" | " + UNIT_ID + " | %% | %%", input);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void notificationScopeIsExactAndDoesNotReplaceOwnerOrManualSourceFilter(boolean admin) {
        Fixture fixture = new Fixture(new Actor(
                ACTOR_ID, admin ? Role.ADMIN : Role.STAFF, UNIT_ID, admin, false));
        SearchRequest input = request(Map.of("scope", NotificationScope.ROOM, "q", KEYWORD));

        fixture.notifications().searchByPage(input);

        assertThat(fixture.countSql()).contains(
                admin ? "WHERE TRUE AND n.scope=?" : "WHERE n.created_by=? AND n.scope=?",
                "AND n.source_type IS NULL AND (n.title ILIKE ? OR n.body ILIKE ?)");
        fixture.assertPagedQuery(
                (admin ? "" : " | " + ACTOR_ID) + " | " + NotificationScope.ROOM
                        + " | " + KEYWORD_PATTERN + " | " + KEYWORD_PATTERN,
                input);
        verify(fixture.guard).requirePermission(Permission.NOTIFICATION_READ);
    }

    @Test
    void unsupportedNotificationScopeIsBoundInsteadOfInterpolated() {
        Fixture fixture = new Fixture();
        String scope = "ROOM' OR TRUE --";
        SearchRequest input = request(Map.of("scope", scope));

        fixture.notifications().searchByPage(input);

        assertThat(fixture.countSql()).contains("n.scope=?").doesNotContain(scope);
        fixture.assertPagedQuery(" | " + ACTOR_ID + " | " + scope + " | %% | %%", input);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void blankNotificationScopeIsNotApplied(String scope) {
        Fixture fixture = new Fixture();
        SearchRequest input = scope == null ? new SearchRequest() : request(Map.of("scope", scope));

        fixture.notifications().searchByPage(input);

        assertThat(fixture.countSql()).doesNotContain("n.scope=?");
        fixture.assertPagedQuery(" | " + ACTOR_ID + " | %% | %%", input);
    }

    @Test
    void configKeyUsesExactEqualityCombinedWithKeywordSpecification() {
        ConfigFixture fixture = new ConfigFixture();
        String key = "deposit_%' OR TRUE --";
        SearchRequest input = request(Map.of("key", key, "q", "deposit", "page", 1, "size", 5));

        fixture.service.searchByPage(input);
        fixture.evaluateSpecification();

        verify(fixture.builder).equal(fixture.root.get("key"), key);
        verify(fixture.builder).and(any(), any());
        verify(fixture.builder).like(fixture.builder.lower(fixture.root.get("key")), "%deposit%");
        assertThat(fixture.pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(fixture.pageable.getValue().getPageSize()).isEqualTo(5);
        verify(fixture.guard).requirePermission(Permission.CONFIG_READ);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void blankConfigKeyLeavesKeywordSpecificationUnchanged(String key) {
        ConfigFixture fixture = new ConfigFixture();
        SearchRequest input = key == null ? new SearchRequest() : request(Map.of("key", key));

        fixture.service.searchByPage(input);
        fixture.evaluateSpecification();

        verify(fixture.builder, never()).equal(any(Expression.class), any(Object.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void summerFiltersRemainInsideAuthenticatedScopeAndBothQueries(boolean student) {
        Fixture fixture = new Fixture(new Actor(ACTOR_ID, student ? Role.STUDENT : Role.STAFF,
                UNIT_ID, false, false));
        SearchRequest input = request(Map.of("choice", " STAY ", "contractId", CONTRACT_ID,
                "studentId", 42L, "q", KEYWORD, "page", 1, "size", 5));
        fixture.summer().searchByPage(input, student);
        assertThat(fixture.countSql()).contains(student ? "WHERE c.student_id=?" : "WHERE a.preferred_unit_id=?",
                "AND (u.full_name ILIKE ? OR u.username ILIKE ? OR sp.student_code ILIKE ? OR c.code ILIKE ?)",
                "AND sc.choice=? AND sc.contract_id=? AND c.student_id=?");
        fixture.assertPagedQuery(" | " + (student ? ACTOR_ID : UNIT_ID) + " | " + KEYWORD_PATTERN
                + " | " + KEYWORD_PATTERN + " | " + KEYWORD_PATTERN + " | " + KEYWORD_PATTERN
                + " | STAY | " + CONTRACT_ID + " | 42", input);
        verify(fixture.guard).requirePermission(Permission.SUMMER_REGISTRATION_READ);
    }

    @Test
    void summerOmittedFiltersLeaveStaffScopeIntact() {
        Fixture fixture = new Fixture();
        SearchRequest input = new SearchRequest();
        fixture.summer().searchByPage(input, false);
        assertThat(fixture.countSql()).contains("WHERE a.preferred_unit_id=?")
                .doesNotContain("sc.choice=?", "sc.contract_id=?", "AND c.student_id=?");
        fixture.assertPagedQuery(" | " + UNIT_ID + " | %% | %% | %% | %%", input);
    }

    @Test
    void utilityRateZeroPriceAndInclusiveDatesAreBoundToCountAndData() {
        Fixture fixture = new Fixture();
        SearchRequest input = request(Map.of("unitPrice", 0, "effectiveFromFrom", "2026-01-01",
                "effectiveFromTo", "2026-01-31", "q", "power", "page", 1, "size", 5));
        fixture.utilityRates().searchByPage(input);
        assertThat(fixture.countSql()).contains("AND unit_price=? AND effective_from>=? AND effective_from<=?");
        fixture.assertPagedQuery(" | %power% | %power% | %power% | 0 | 2026-01-01 | 2026-01-31", input);
        verify(fixture.guard).requirePermission(Permission.UTILITY_RATE_READ);
        verify(fixture.guard).requireRole(Role.STAFF);
    }

    @Test
    void utilityRateOmittedFiltersDoNotBecomeZeroOrDates() {
        Fixture fixture = new Fixture();
        SearchRequest input = new SearchRequest();
        fixture.utilityRates().searchByPage(input);
        assertThat(fixture.countSql()).doesNotContain("unit_price=?", "effective_from>=?", "effective_from<=?");
        fixture.assertPagedQuery(" | %% | %% | %%", input);
    }

    private static SearchRequest request(Map<String, Object> fields) {
        return new ObjectMapper().findAndRegisterModules().convertValue(fields, SearchRequest.class);
    }

    private static final class Fixture {
        private final FakeJdbc jdbc = new FakeJdbc();
        private final ScopeGuard guard = mock(ScopeGuard.class);
        private final CurrentUser current = mock(CurrentUser.class);
        private final SqlPage pages = new SqlPage(jdbc);

        private Fixture() {
            this(new Actor(ACTOR_ID, Role.STAFF, UNIT_ID, false, false));
        }

        private Fixture(Actor actor) {
            when(current.get()).thenReturn(actor);
            jdbc.returningOne("SELECT count(*)", FakeJdbc.row("count", 7L));
            jdbc.returning("SELECT * FROM (", List.of());
        }

        private OverviewServiceImpl overview() {
            return new OverviewServiceImpl(pages, guard, current);
        }

        private SummerCrudServiceImpl summer() {
            return new SummerCrudServiceImpl(jdbc, pages, current,
                    mock(AuditService.class), guard, mock(StatusHistoryService.class),
                    mock(ContractService.class), mock(RoomReferenceService.class));
        }

        private UtilityRateServiceImpl utilityRates() {
            return new UtilityRateServiceImpl(
                    mock(UtilityRateRepository.class), current, guard,
                    jdbc, pages, mock(AuditService.class));
        }

        private NotificationCrudServiceImpl notifications() {
            return new NotificationCrudServiceImpl(
                    jdbc, pages, current, guard, mock(AuditService.class),
                    mock(NotificationRepository.class), mock(RoomReferenceService.class),
                    mock(FloorService.class));
        }

        private PageResponse<?> searchDeposits(SearchRequest input, boolean overview, boolean mine) {
            if (overview) {
                return overview().searchDepositsByPage(input, mine);
            }
            return new DepositServiceImpl(
                    mock(DepositTransactionRepository.class), jdbc, pages, guard, current,
                    mock(AuditService.class)).searchBalancesByPage(input, mine);
        }

        private String countSql() {
            return jdbc.statements().get(0).split(" \\| ", 2)[0];
        }

        private void assertPagedQuery(String boundArguments, SearchRequest input) {
            assertThat(jdbc.statements()).hasSize(2);
            String count = jdbc.statements().get(0);
            String data = jdbc.statements().get(1);
            String innerSql = countSql().substring("SELECT count(*) FROM (".length());
            innerSql = innerSql.substring(0, innerSql.length() - ") results".length());
            assertThat(count).endsWith(boundArguments);
            assertThat(data).startsWith("SELECT * FROM (" + innerSql + ") results ORDER BY ");
            assertThat(data).endsWith(boundArguments + " | " + input.size()
                    + " | " + ((long) input.page() * input.size()));
            assertThat(jdbc.writes()).isEmpty();
        }
    }

    private static final class ConfigFixture {
        private final SystemConfigRepository repository = mock(SystemConfigRepository.class);
        private final ScopeGuard guard = mock(ScopeGuard.class);
        private final ConfigServiceImpl service = new ConfigServiceImpl(
                repository, mock(CurrentUser.class), guard, mock(AuditService.class));
        @SuppressWarnings("unchecked")
        private final Root<SystemConfig> root = mock(Root.class, RETURNS_DEEP_STUBS);
        private final CriteriaBuilder builder = mock(CriteriaBuilder.class, RETURNS_MOCKS);
        private final ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        @SuppressWarnings("null")
        private ConfigFixture() {
            when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<SystemConfig>>argThat(specification -> specification != null), any(Pageable.class)))
                    .thenReturn(Page.empty());
        }

        @SuppressWarnings({"unchecked", "null"})
        private void evaluateSpecification() {
            ArgumentCaptor<Specification<SystemConfig>> specification =
                    ArgumentCaptor.forClass(Specification.class);
            verify(repository).findAll(specification.capture(), pageable.capture());
            specification.getValue().toPredicate(Objects.requireNonNull(root), mock(CriteriaQuery.class), Objects.requireNonNull(builder));
        }
    }
}
