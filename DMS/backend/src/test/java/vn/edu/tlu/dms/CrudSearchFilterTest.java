package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Kiem tra bo loc, tham so bind va predicate dung chung cho du lieu/dem, khong can CSDL. */
class CrudSearchFilterTest {

    private final ScopeGuard guard = mock(ScopeGuard.class);
    private final CurrentUser current = mock(CurrentUser.class);
    private final AuditService audit = mock(AuditService.class);
    private final ObjectMapper mapper = new ObjectMapper();

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
    void omittedPriorityActiveFilterStillRestrictsStudentLookup() {
        when(current.get()).thenReturn(new Actor(10L, Role.STUDENT, null, false, false));
        var repository = mock(PriorityCategoryRepository.class);
        var service = new PriorityCategoryServiceImpl(repository, guard, current, audit);
        List<String> predicates = pagePredicates(repository,
                () -> service.searchByPage(uncheckedRequest("{\"page\":1,\"size\":10}")));

        assertThat(predicates).allSatisfy(predicate -> assertThat(predicate)
                .contains("active IS TRUE").doesNotContain("active="));
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
    @SuppressWarnings("unchecked")
    private static <E, R extends JpaRepository<E, Long> & JpaSpecificationExecutor<E>>
            List<String> pagePredicates(R repository, Supplier<PageResponse<?>> search) {
        var captured = new AtomicReference<Specification<E>>();
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    captured.set(invocation.getArgument(0));
                    Pageable pageable = invocation.getArgument(1);
                    return new PageImpl<E>(List.of(), pageable, 31);
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
            when(builder.like(any(Expression.class), anyString())).thenAnswer(invocation ->
                    node(labels.get(invocation.getArgument(0)) + " LIKE " + invocation.getArgument(1)));
            when(builder.or(any(Predicate[].class))).thenAnswer(invocation -> {
                String text = Stream.of(invocation.getArguments())
                        .map(labels::get).collect(Collectors.joining(" OR ", "(", ")"));
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
            Predicate predicate = specification.toPredicate(root, query, builder);
            assertThat(predicate).isNotNull();
            return labels.get(predicate);
        }
    }
}
