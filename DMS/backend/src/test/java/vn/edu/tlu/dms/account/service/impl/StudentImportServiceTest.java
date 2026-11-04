package vn.edu.tlu.dms.account.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import vn.edu.tlu.dms.account.api.AccountDtos.TemporaryPassword;
import vn.edu.tlu.dms.account.api.StudentManagementController.Save;
import vn.edu.tlu.dms.account.service.CatalogService;
import vn.edu.tlu.dms.account.service.StudentManagementService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.Catalog;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.StudentImport;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

class StudentImportServiceTest {

    private final CatalogService catalogs = mock(CatalogService.class);
    private final StudentManagementService students = mock(StudentManagementService.class);
    private final CurrentUser current = mock(CurrentUser.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private StudentImportServiceImpl service;

    @BeforeEach
    void setUp() {
        when(current.get()).thenReturn(new Actor(1L, Role.ADMIN, null, false, false));
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(catalogs.resolveImport(Catalog.FACULTIES, "FACULTY_CODE")).thenReturn(2L);
        when(catalogs.resolveImport(Catalog.COHORTS, "COHORT_CODE")).thenReturn(3L);
        when(catalogs.requireActive(Catalog.FACULTIES, 2L))
                .thenReturn(Map.of(ApiField.ID, 2L, ApiField.NAME, "Faculty"));
        when(students.saveOrUpdate(any())).thenReturn(new TemporaryPassword(5L, "temporary"));
        service = new StudentImportServiceImpl(mock(JdbcTemplate.class), students,
                mock(ScopeGuard.class), current, mock(AuditService.class), manager, catalogs);
    }

    @Test
    void exactCatalogCodesBecomeIdsAndOptionalColumnsMayBeAbsent() throws Exception {
        var result = service.importStudents(file("FACULTY_CODE", "COHORT_CODE"), false);
        assertThat(result.createdRows()).isEqualTo(1);
        var saved = ArgumentCaptor.forClass(Save.class);
        verify(students).saveOrUpdate(saved.capture());
        assertThat(saved.getValue().facultyId()).isEqualTo(2L);
        assertThat(saved.getValue().cohortId()).isEqualTo(3L);
        assertThat(saved.getValue().schoolId()).isNull();
        assertThat(saved.getValue().hometownId()).isNull();
        assertThat(saved.getValue().phone()).isNullOrEmpty();
    }

    @Test
    void unknownCatalogIsPerRowErrorWithoutAnyIdentityCreation() throws Exception {
        when(catalogs.resolveImport(Catalog.FACULTIES, "UNKNOWN"))
                .thenThrow(BusinessException.invalid(ApiField.NAME, Catalog.INVALID_VALUE));
        var result = service.importStudents(file("UNKNOWN", "COHORT_CODE"), false);
        assertThat(result.createdRows()).isZero();
        assertThat(result.errorRows()).isEqualTo(1);
        assertThat(result.rows().get(0).errors()).anySatisfy(error -> {
            assertThat(error.column()).isEqualTo(ApiField.FACULTY);
            assertThat(error.code()).isEqualTo(StudentImport.INVALID);
        });
        verify(students, never()).saveOrUpdate(any());
    }

    @Test
    void inactiveCatalogIsRejectedDuringPreview() throws Exception {
        when(catalogs.resolveImport(Catalog.COHORTS, "INACTIVE"))
                .thenThrow(BusinessException.invalid(ApiField.NAME, Catalog.INVALID_VALUE));
        var result = service.importStudents(file("FACULTY_CODE", "INACTIVE"), true);
        assertThat(result.validRows()).isZero();
        assertThat(result.rows().get(0).errors()).anySatisfy(error ->
                assertThat(error.column()).isEqualTo(ApiField.COHORT));
        verify(students, never()).saveOrUpdate(any());
    }

    @Test
    void facultyWithKnownSchoolDoesNotInferMissingSchool() throws Exception {
        when(catalogs.requireActive(Catalog.FACULTIES, 2L))
                .thenReturn(Map.of(ApiField.ID, 2L, ApiField.SCHOOL_ID, 1L));
        var result = service.importStudents(file("FACULTY_CODE", "COHORT_CODE"), false);
        assertThat(result.errorRows()).isEqualTo(1);
        assertThat(result.rows().get(0).errors()).anySatisfy(error ->
                assertThat(error.column()).isEqualTo(ApiField.FACULTY));
        verify(students, never()).saveOrUpdate(any());
    }

    private MockMultipartFile file(String faculty, String cohort) throws Exception {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet();
            var header = sheet.createRow(0);
            // Chỉ sáu cột bắt buộc; không yêu cầu các cột tùy chọn của registry.
            for (int column = 0; column < 6; column++) {
                header.createCell(column).setCellValue(StudentImport.HEADERS.get(column));
            }
            var row = sheet.createRow(1);
            var values = List.of("9900100001", "Student Name", "Nam", "20/10/2005", faculty, cohort);
            for (int column = 0; column < values.size(); column++) {
                row.createCell(column).setCellValue(values.get(column));
            }
            workbook.write(output);
            return new MockMultipartFile("file", "students.xlsx", null, output.toByteArray());
        }
    }
}
