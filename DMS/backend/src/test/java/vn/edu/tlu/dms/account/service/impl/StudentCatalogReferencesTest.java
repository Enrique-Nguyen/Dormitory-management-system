package vn.edu.tlu.dms.account.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;

import vn.edu.tlu.dms.account.api.AccountDtos;
import vn.edu.tlu.dms.account.api.StudentManagementController;
import vn.edu.tlu.dms.account.service.CatalogService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.Catalog;
import vn.edu.tlu.dms.common.exception.BusinessException;

import java.util.Map;

class StudentCatalogReferencesTest {

    private final CatalogService catalogs = mock(CatalogService.class);

    @Test
    void normalizedDtosValidateAllCatalogIdsAsPositive() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var student = new StudentManagementController.Save(
                    null, null, null, null, null, 0L, -1L, 0L, null, -1L, null);
            assertThat(validator.validate(student)).extracting(error -> error.getPropertyPath().toString())
                    .containsExactlyInAnyOrder(ApiField.SCHOOL_ID, ApiField.FACULTY_ID,
                            ApiField.COHORT_ID, ApiField.HOMETOWN_ID);
            var account = new AccountDtos.Save(null, null, null, null, null, null,
                    null, null, null, 0L, -1L, 0L, -1L);
            assertThat(validator.validate(account)).extracting(error -> error.getPropertyPath().toString())
                    .containsExactlyInAnyOrder(ApiField.SCHOOL_ID, ApiField.FACULTY_ID,
                            ApiField.COHORT_ID, ApiField.HOMETOWN_ID);
            assertThat(validator.validate(new AccountDtos.ProfileInput(null, 0L, null)))
                    .extracting(error -> error.getPropertyPath().toString())
                    .containsExactly(ApiField.HOMETOWN_ID);
        }
    }

    @Test
    void omittedReferencesPreserveNullWithoutLookups() {
        var stored = new StudentCatalogReferences(null, null, null, null);
        assertThat(StudentCatalogReferences.merge(catalogs, stored, null, null, null, null))
                .isEqualTo(stored);
        verifyNoInteractions(catalogs);
    }

    @Test
    void inactiveUnchangedReferencesSurvivePartialUpdate() {
        var stored = new StudentCatalogReferences(1L, 2L, 3L, 4L);
        when(catalogs.getReferenceById(anyString(), anyLong()))
                .thenReturn(Map.of(ApiField.ACTIVE, false));
        when(catalogs.getReferenceById(Catalog.FACULTIES, 2L))
                .thenReturn(Map.of(ApiField.ACTIVE, false, ApiField.SCHOOL_ID, 1L));
        assertThat(StudentCatalogReferences.merge(catalogs, stored, null, 2L, null, 4L))
                .isEqualTo(stored);
        verify(catalogs, never()).requireActive(anyString(), anyLong());
    }

    @Test
    void newReferencesUseActiveLookupAndRetainOtherIds() {
        var stored = new StudentCatalogReferences(null, null, 3L, null);
        when(catalogs.getReferenceById(Catalog.COHORTS, 3L)).thenReturn(Map.of(ApiField.ACTIVE, false));
        when(catalogs.requireActive(Catalog.SCHOOLS, 1L)).thenReturn(Map.of(ApiField.ID, 1L));
        when(catalogs.requireActive(Catalog.FACULTIES, 2L))
                .thenReturn(Map.of(ApiField.ID, 2L, ApiField.SCHOOL_ID, 1L));
        when(catalogs.requireActive(Catalog.HOMETOWNS, 4L)).thenReturn(Map.of(ApiField.ID, 4L));
        assertThat(StudentCatalogReferences.merge(catalogs, stored, 1L, 2L, null, 4L))
                .isEqualTo(new StudentCatalogReferences(1L, 2L, 3L, 4L));
        verify(catalogs).requireActive(Catalog.SCHOOLS, 1L);
        verify(catalogs).requireActive(Catalog.FACULTIES, 2L);
        verify(catalogs).requireActive(Catalog.HOMETOWNS, 4L);
    }

    @Test
    void inactiveNewReferenceHasProfileFieldMetadata() {
        when(catalogs.requireActive(Catalog.COHORTS, 5L))
                .thenThrow(BusinessException.invalid(ApiField.ID, Catalog.INVALID_VALUE));
        assertThatThrownBy(() -> StudentCatalogReferences.merge(catalogs,
                new StudentCatalogReferences(null, null, null, null), null, null, 5L, null))
                .isInstanceOfSatisfying(BusinessException.class, error ->
                        assertThat(error.getDetails().toString()).contains(ApiField.COHORT_ID));
    }

    @Test
    void schoolChangeCannotSilentlyKeepFacultyFromDifferentSchool() {
        var stored = new StudentCatalogReferences(1L, 2L, null, null);
        when(catalogs.requireActive(Catalog.SCHOOLS, 7L)).thenReturn(Map.of(ApiField.ID, 7L));
        when(catalogs.getReferenceById(Catalog.FACULTIES, 2L))
                .thenReturn(Map.of(ApiField.SCHOOL_ID, 1L));
        assertThatThrownBy(() -> StudentCatalogReferences.merge(catalogs, stored, 7L, null, null, null))
                .isInstanceOfSatisfying(BusinessException.class, error ->
                        assertThat(error.getDetails().toString())
                                .contains(ApiField.FACULTY_ID, Catalog.INVALID_HIERARCHY));
    }

    @Test
    void knownFacultySchoolMustBeExplicitRatherThanInferred() {
        when(catalogs.requireActive(Catalog.FACULTIES, 2L))
                .thenReturn(Map.of(ApiField.SCHOOL_ID, 1L));
        assertThatThrownBy(() -> StudentCatalogReferences.merge(catalogs,
                new StudentCatalogReferences(null, null, null, null), null, 2L, null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void migratedOrphanFacultyDoesNotInventSchool() {
        when(catalogs.getReferenceById(Catalog.FACULTIES, 2L)).thenReturn(Map.of(ApiField.NAME, "Legacy"));
        var stored = new StudentCatalogReferences(null, 2L, null, null);
        assertThat(StudentCatalogReferences.merge(catalogs, stored, null, null, null, null))
                .isEqualTo(stored);
        verify(catalogs, never()).requireActive(anyString(), anyLong());
    }

    @Test
    void labelsUseInactiveAllowedLookupAndNullSkipsLookup() {
        when(catalogs.getReferenceById(Catalog.HOMETOWNS, 4L))
                .thenReturn(Map.of(ApiField.NAME, "Old label", ApiField.ACTIVE, false));
        assertThat(StudentCatalogReferences.label(catalogs, Catalog.HOMETOWNS, 4L))
                .isEqualTo("Old label");
        assertThat(StudentCatalogReferences.label(catalogs, Catalog.SCHOOLS, null)).isNull();
        verify(catalogs, never()).requireActive(anyString(), anyLong());
    }
}
