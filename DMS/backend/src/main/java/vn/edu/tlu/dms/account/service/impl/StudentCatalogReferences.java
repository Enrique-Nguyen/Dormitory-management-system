package vn.edu.tlu.dms.account.service.impl;

import org.springframework.http.HttpStatus;

import vn.edu.tlu.dms.account.service.CatalogService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.Catalog;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.exception.BusinessException;

import java.util.Map;
import java.util.Objects;

/** Hợp nhất tham chiếu hồ sơ cho cả API tài khoản và API quản lý sinh viên. */
record StudentCatalogReferences(Long schoolId, Long facultyId, Long cohortId, Long hometownId) {

    static StudentCatalogReferences merge(
            CatalogService catalogs,
            StudentCatalogReferences stored,
            Long schoolId,
            Long facultyId,
            Long cohortId,
            Long hometownId) {
        Long school = schoolId == null ? stored.schoolId() : schoolId;
        Long faculty = facultyId == null ? stored.facultyId() : facultyId;
        Long cohort = cohortId == null ? stored.cohortId() : cohortId;
        Long hometown = hometownId == null ? stored.hometownId() : hometownId;
        reference(catalogs, Catalog.SCHOOLS, ApiField.SCHOOL_ID, school, stored.schoolId());
        Map<String, Object> facultyRow =
                reference(catalogs, Catalog.FACULTIES, ApiField.FACULTY_ID, faculty, stored.facultyId());
        reference(catalogs, Catalog.COHORTS, ApiField.COHORT_ID, cohort, stored.cohortId());
        reference(catalogs, Catalog.HOMETOWNS, ApiField.HOMETOWN_ID, hometown, stored.hometownId());
        if (facultyRow != null && facultyRow.get(ApiField.SCHOOL_ID) instanceof Number parent
                && !Objects.equals(parent.longValue(), school)) {
            // Không suy đoán trường của khoa cũ; client phải chọn cặp tham chiếu nhất quán.
            throw BusinessException.invalid(ApiField.FACULTY_ID, Catalog.INVALID_HIERARCHY);
        }
        return new StudentCatalogReferences(school, faculty, cohort, hometown);
    }

    static Map<String, Object> reference(
            CatalogService catalogs, String kind, String field, Long requested, Long stored) {
        if (requested == null) {
            return null;
        }
        if (requested <= 0) {
            throw BusinessException.invalid(field, ValidationMessage.ID_POSITIVE);
        }
        try {
            return Objects.equals(requested, stored)
                    ? catalogs.getReferenceById(kind, requested)
                    : catalogs.requireActive(kind, requested);
        } catch (BusinessException error) {
            if (error.getStatus() == HttpStatus.BAD_REQUEST.value()
                    || error.getStatus() == HttpStatus.NOT_FOUND.value()) {
                throw BusinessException.invalid(field, Catalog.INVALID_VALUE);
            }
            throw error;
        }
    }

    static String label(CatalogService catalogs, String kind, Long id) {
        if (id == null) {
            return null;
        }
        return (String) catalogs.getReferenceById(kind, id).get(ApiField.NAME);
    }
}
