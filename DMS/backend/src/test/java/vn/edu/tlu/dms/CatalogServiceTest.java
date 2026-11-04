package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import vn.edu.tlu.dms.account.api.CatalogRequest;
import vn.edu.tlu.dms.account.api.CatalogSearch;
import vn.edu.tlu.dms.account.service.impl.CatalogServiceImpl;
import vn.edu.tlu.dms.common.constants.DmsConstants.Action;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.Catalog;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;

import java.util.List;
import java.util.Map;

class CatalogServiceTest {
    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final ScopeGuard guard = mock(ScopeGuard.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final AuditService audit = mock(AuditService.class);
    private final CatalogServiceImpl service = new CatalogServiceImpl(jdbc, guard, user, audit);
    private final Map<String, Object> row = Map.of("id", 1L, "code", "ONE", "name", "One", "active", true);

    @BeforeEach
    void setUp() {
        when(user.get()).thenReturn(new Actor(7L, Role.ADMIN, null, false, false));
        when(jdbc.queryForList(anyString(), anyMap())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            return sql.startsWith("SELECT pg_advisory") ? List.of() : List.of(row);
        });
    }

    @Test
    void createChecksCreatePermissionAndAudits() {
        when(jdbc.queryForObject(startsWith("INSERT"), any(SqlParameterSource.class), eq(Long.class))).thenReturn(1L);
        var result = service.saveOrUpdate(Catalog.SCHOOLS, input(null, "ONE", "One", null, null));
        assertThat(result.get("id")).isEqualTo(1L);
        verify(guard).requirePermission(Permission.CATALOG_CREATE);
        verify(guard, never()).requirePermission(Permission.CATALOG_UPDATE);
        verify(audit).record(eq(7L), eq(AuditAction.CATALOG_CREATE), eq(EntityType.CATALOG), eq(1L),
                eq(null), any(), eq(null));
    }

    @Test
    void updateOnlyPermissionCannotCreate() {
        doThrow(BusinessException.forbidden()).when(guard).requirePermission(Permission.CATALOG_CREATE);
        assertThatThrownBy(() -> service.saveOrUpdate(Catalog.SCHOOLS, input(null, "ONE", "One", null, null)))
                .isInstanceOf(BusinessException.class);
        verify(jdbc, never()).queryForList(anyString(), anyMap());
    }

    @Test
    void createOnlyPermissionCannotUpdate() {
        doThrow(BusinessException.forbidden()).when(guard).requirePermission(Permission.CATALOG_UPDATE);
        assertThatThrownBy(() -> service.saveOrUpdate(Catalog.SCHOOLS, input(1L, null, null, null, null)))
                .isInstanceOf(BusinessException.class);
        verify(jdbc, never()).queryForList(anyString(), anyMap());
    }

    @Test
    void partialUpdatePreservesCodeNameAndFalse() {
        var inactive = Map.<String, Object>of("id", 1L, "code", "ONE", "name", "One", "active", false);
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(List.of(inactive));
        service.saveOrUpdate(Catalog.COHORTS, input(1L, null, null, null, null));
        verify(jdbc).update(eq("UPDATE student_cohort SET name=:name, active=:active WHERE id=:id"),
                org.mockito.ArgumentMatchers.<SqlParameterSource>argThat(p -> Boolean.FALSE.equals(p.getValue("active"))
                        && "One".equals(p.getValue("name"))));
        verify(jdbc).queryForList("SELECT * FROM student_cohort WHERE id=:id FOR UPDATE", Map.of("id", 1L));
    }

    @Test
    void missingUpdateNeverCreates() {
        when(jdbc.queryForList(startsWith("SELECT *"), anyMap())).thenReturn(List.of());
        assertThatThrownBy(() -> service.saveOrUpdate(Catalog.SCHOOLS, input(99L, null, null, null, null)))
                .isInstanceOf(BusinessException.class).hasMessage("NOT_FOUND");
        verify(jdbc, never()).queryForObject(startsWith("INSERT"), any(SqlParameterSource.class), eq(Long.class));
    }

    @Test
    void codeIsImmutable() {
        assertThatThrownBy(() -> service.saveOrUpdate(Catalog.SCHOOLS, input(1L, "TWO", null, null, null)))
                .isInstanceOf(BusinessException.class).hasMessage("VALIDATION_FAILED");
    }

    @Test
    void invalidAcademicYearRejected() {
        assertThatThrownBy(() -> service.saveOrUpdate(Catalog.ACADEMIC_YEARS,
                input(null, "2025-2027", "Year", null, null))).isInstanceOf(BusinessException.class);
    }

    @Test
    void readInactiveAndDeclareActions() {
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(List.of(
                Map.of("id", 1L, "code", "ONE", "name", "One", "active", false)));
        when(guard.hasPermission(Permission.CATALOG_UPDATE)).thenReturn(true);
        var result = service.getById(Catalog.SCHOOLS, 1L);
        assertThat(result.get("active")).isEqualTo(false);
        assertThat(result.get("allowedActions")).isEqualTo(List.of(Action.UPDATE));
        verify(guard).requirePermission(Permission.CATALOG_READ);
    }

    @Test
    void selectionUsesShareLockAndRejectsInactive() {
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(List.of(
                Map.of("id", 1L, "active", false)));
        assertThatThrownBy(() -> service.requireActive(Catalog.SCHOOLS, 1L)).isInstanceOf(BusinessException.class);
        verify(jdbc).queryForList("SELECT * FROM school WHERE id=:id FOR SHARE", Map.of("id", 1L));
        assertThat(service.requireActive(Catalog.SCHOOLS, null)).isNull();
    }

    @Test
    void importRequiresOneExactActiveMatchWithoutCreation() {
        assertThat(service.resolveImport(Catalog.SCHOOLS, "One")).isEqualTo(1L);
        verify(jdbc).queryForList("SELECT * FROM school WHERE (code=:value OR name=:value) FOR SHARE", Map.of("value", "One"));
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(List.of(row, row));
        assertThatThrownBy(() -> service.resolveImport(Catalog.SCHOOLS, "ONE")).isInstanceOf(BusinessException.class);
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(List.of());
        assertThatThrownBy(() -> service.resolveImport(Catalog.SCHOOLS, "Unknown")).isInstanceOf(BusinessException.class);
        verify(jdbc, never()).update(anyString(), any(SqlParameterSource.class));
    }

    @Test
    void pagingUsesSameBoundFiltersForCountAndRows() {
        when(jdbc.queryForObject(startsWith("SELECT count"), any(SqlParameterSource.class), eq(Long.class))).thenReturn(31L);
        when(jdbc.queryForList(anyString(), any(SqlParameterSource.class))).thenReturn(List.of(row));
        var result = service.searchByPage(Catalog.FACULTIES,
                new CatalogSearch(2, 10, " O'Brien% ", List.of("name,desc"), false, null, 3L));
        assertThat(result.totalElements()).isEqualTo(31);
        assertThat(result.totalPages()).isEqualTo(4);
        String where = " WHERE true AND (strpos(lower(code), lower(:q)) > 0 OR strpos(lower(name), lower(:q)) > 0)"
                + " AND active = :active AND school_id = :relation";
        verify(jdbc).queryForObject(eq("SELECT count(*) FROM faculty" + where), any(SqlParameterSource.class), eq(Long.class));
        verify(jdbc).queryForList(eq("SELECT * FROM faculty" + where + " ORDER BY name desc, id ASC LIMIT :limit OFFSET :offset"),
                org.mockito.ArgumentMatchers.<SqlParameterSource>argThat(p -> Long.valueOf(20).equals(p.getValue("offset"))
                        && "O'Brien%".equals(p.getValue("q")) && Boolean.FALSE.equals(p.getValue("active"))));
    }

    @Test
    void catalogSearchDeserializesSeparateCodeAndName() throws Exception {
        var request = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                "{\"code\":\"TLU\",\"name\":\"Thuy Loi\"}", CatalogSearch.class);
        assertThat(request.code()).isEqualTo("TLU");
        assertThat(request.name()).isEqualTo("Thuy Loi");
    }

    @Test
    void schoolCodeAndNameAreBoundSeparatelyForCountAndRows() {
        when(jdbc.queryForObject(startsWith("SELECT count"), any(SqlParameterSource.class), eq(Long.class))).thenReturn(1L);
        when(jdbc.queryForList(anyString(), any(SqlParameterSource.class))).thenReturn(List.of(row));
        service.searchByPage(Catalog.SCHOOLS,
                new CatalogSearch(0, 10, null, null, null, null, null, " A% ", " O'Brien "));
        String where = " WHERE true AND strpos(lower(code), lower(:code)) > 0"
                + " AND strpos(lower(name), lower(:name)) > 0";
        verify(jdbc).queryForObject(eq("SELECT count(*) FROM school" + where),
                org.mockito.ArgumentMatchers.<SqlParameterSource>argThat(p ->
                        "A%".equals(p.getValue("code")) && "O'Brien".equals(p.getValue("name"))), eq(Long.class));
        verify(jdbc).queryForList(startsWith("SELECT * FROM school" + where),
                org.mockito.ArgumentMatchers.<SqlParameterSource>argThat(p ->
                        "A%".equals(p.getValue("code")) && "O'Brien".equals(p.getValue("name"))));
    }

    @Test
    void blankCatalogFiltersAreOmittedFromCountAndData() {
        when(jdbc.queryForObject(startsWith("SELECT count"), any(SqlParameterSource.class), eq(Long.class))).thenReturn(0L);
        when(jdbc.queryForList(anyString(), any(SqlParameterSource.class))).thenReturn(List.of());
        service.searchByPage(Catalog.SCHOOLS,
                new CatalogSearch(0, 10, null, null, null, null, null, " ", ""));
        verify(jdbc).queryForObject(eq("SELECT count(*) FROM school WHERE true"), any(SqlParameterSource.class), eq(Long.class));
        verify(jdbc).queryForList(startsWith("SELECT * FROM school WHERE true ORDER BY"), any(SqlParameterSource.class));
        verify(guard).requirePermission(Permission.CATALOG_READ);
    }

    @Test
    void unknownKindBadPagingAndSqlSortRejected() {
        assertThatThrownBy(() -> service.getById("school;drop table school", 1L)).hasMessage("NOT_FOUND");
        assertThatThrownBy(() -> service.searchByPage(Catalog.SCHOOLS,
                new CatalogSearch(-1, 10, null, null, null, null, null))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.searchByPage(Catalog.SCHOOLS,
                new CatalogSearch(0, 10, null, List.of("name;DROP,asc"), null, null, null))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.getById(Catalog.SCHOOLS, 0L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void hierarchyRejectsCycleAndInactiveParent() {
        when(jdbc.queryForList(startsWith("WITH RECURSIVE"), anyMap(), eq(Long.class))).thenReturn(List.of(2L, 1L));
        assertThatThrownBy(() -> service.saveOrUpdate(Catalog.HOMETOWNS, input(1L, null, null, true, 2L)))
                .isInstanceOf(BusinessException.class);
        when(jdbc.queryForList(startsWith("WITH RECURSIVE"), anyMap(), eq(Long.class))).thenReturn(List.of(2L));
        when(jdbc.queryForList(eq("SELECT * FROM hometown WHERE id=:id FOR SHARE"), anyMap()))
                .thenReturn(List.of(Map.of("id", 2L, "active", false)));
        assertThatThrownBy(() -> service.saveOrUpdate(Catalog.HOMETOWNS, input(1L, null, null, true, 2L)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void activeChildrenPreventDeactivation() {
        when(jdbc.queryForObject(startsWith("SELECT count(*) FROM faculty"), anyMap(), eq(Long.class))).thenReturn(1L);
        assertThatThrownBy(() -> service.saveOrUpdate(Catalog.SCHOOLS, input(1L, null, null, false, null)))
                .hasMessage("RESOURCE_IN_USE");
    }

    @Test
    void foreignKeyDeleteFailureIsNotSwallowedOrAudited() {
        when(jdbc.update(eq("DELETE FROM school WHERE id=:id"), anyMap()))
                .thenThrow(new DataIntegrityViolationException("foreign key"));
        assertThatThrownBy(() -> service.delete(Catalog.SCHOOLS, 1L)).isInstanceOf(DataIntegrityViolationException.class);
        verify(guard).requirePermission(Permission.CATALOG_DELETE);
        verify(audit, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void roleEnvelopeAllowsSharedReadsAndOnlyStaffWrites() {
        assertThat(ScopeGuard.permissionsForRole(Role.STUDENT)).contains(Permission.CATALOG_READ)
                .doesNotContain(Permission.CATALOG_CREATE);
        assertThat(ScopeGuard.permissionsForRole(Role.TECHNICIAN)).contains(Permission.CATALOG_READ)
                .doesNotContain(Permission.CATALOG_UPDATE);
        assertThat(ScopeGuard.permissionsForRole(Role.STAFF)).contains(Permission.CATALOG_READ, Permission.CATALOG_CREATE,
                Permission.CATALOG_UPDATE, Permission.CATALOG_DELETE);
    }

    @Test
    void naturalCodeSelectionIsBoundAndInactiveIsRejected() {
        service.requireActiveCode(Catalog.MAINTENANCE_CATEGORIES, "ONE");
        verify(jdbc).queryForList("SELECT * FROM maintenance_category WHERE code=:code FOR SHARE", Map.of("code", "ONE"));
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(List.of());
        assertThatThrownBy(() -> service.requireActiveCode(Catalog.MAINTENANCE_CATEGORIES, "Unknown"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void readAndDeleteDenialsHaveNoDatabaseSideEffects() {
        doThrow(BusinessException.forbidden()).when(guard).requirePermission(Permission.CATALOG_READ);
        doThrow(BusinessException.forbidden()).when(guard).requirePermission(Permission.CATALOG_DELETE);
        assertThatThrownBy(() -> service.getById(Catalog.SCHOOLS, 1L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.searchByPage(Catalog.SCHOOLS,
                new CatalogSearch(null, null, null, null, null, null, null))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.delete(Catalog.SCHOOLS, 1L)).isInstanceOf(BusinessException.class);
        verify(jdbc, never()).queryForList(anyString(), anyMap());
    }

    @Test
    void relationFieldsAreRestrictedToTheirCatalogKinds() {
        assertThatThrownBy(() -> service.saveOrUpdate(Catalog.SCHOOLS, input(null, "ONE", "One", true, 2L)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.searchByPage(Catalog.COHORTS,
                new CatalogSearch(0, 10, null, null, null, null, 2L))).isInstanceOf(BusinessException.class);
    }

    private CatalogRequest input(Long id, String code, String name, Boolean active, Long parent) {
        return new CatalogRequest(id, code, name, active, parent, null);
    }
}
