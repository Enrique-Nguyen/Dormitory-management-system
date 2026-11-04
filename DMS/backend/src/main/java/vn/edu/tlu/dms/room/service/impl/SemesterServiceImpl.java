package vn.edu.tlu.dms.room.service.impl;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.edu.tlu.dms.account.service.CatalogService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.Catalog;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Resource;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.impl.AbstractCrudServiceImpl;
import vn.edu.tlu.dms.room.api.SemesterRequest;
import vn.edu.tlu.dms.room.api.SemesterResponse;
import vn.edu.tlu.dms.room.domain.Semester;
import vn.edu.tlu.dms.room.repository.SemesterRepository;
import vn.edu.tlu.dms.room.service.SemesterService;

import java.util.Locale;
import java.util.Set;

@Service
public class SemesterServiceImpl
        extends AbstractCrudServiceImpl<Semester, SemesterRequest, SemesterResponse>
        implements SemesterService {

    private final ScopeGuard guard;

    private final CatalogService catalogs;

    public SemesterServiceImpl(
            SemesterRepository repository,
            ScopeGuard guard,
            CurrentUser currentUser,
            AuditService auditService,
            CatalogService catalogs) {
        super(repository, auditService, currentUser);
        this.guard = guard;
        this.catalogs = catalogs;
    }

    @Override
    protected void checkRead() {
        guard.requirePermission(Permission.SEMESTER_READ);
    }

    @Override
    protected void checkWrite() {
        guard.requireRole(Role.STAFF);
    }

    @Override
    protected void checkPermission(String action) {
        guard.requirePermission(Resource.SEMESTER + DmsConstants.PERMISSION_SEPARATOR + action);
    }

    @Override
    protected Long getInputId(SemesterRequest input) {
        return input.id();
    }

    @Override
    protected Set<String> searchableFields() {
        return Set.of("name", "academicYear");
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of("id", "name", "academicYear", "type", "startDate", "endDate");
    }

    @Override
    protected Specification<Semester> searchScope(SearchRequest search) {
        Specification<Semester> scope =
                (root, query, builder) ->
                        search.type() == null || search.type().isBlank()
                                ? builder.conjunction()
                                : builder.equal(root.get("type"), search.type());
        if (search.academicYear() != null && !search.academicYear().isBlank()) {
            scope =
                    scope.and(
                            (root, query, builder) ->
                                    builder.equal(
                                            root.get(ApiField.ACADEMIC_YEAR),
                                            search.academicYear().trim()));
        }
        if (search.name() != null && !search.name().isBlank()) {
            scope = scope.and((root, query, builder) -> builder.greaterThan(builder.locate(
                    builder.lower(root.get("name")), search.name().trim().toLowerCase(Locale.ROOT)), 0));
        }
        if (search.startDateFrom() != null) {
            scope = scope.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("startDate"), search.startDateFrom()));
        }
        if (search.startDateTo() != null) {
            scope = scope.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("startDate"), search.startDateTo()));
        }
        if (search.endDateFrom() != null) {
            scope = scope.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("endDate"), search.endDateFrom()));
        }
        if (search.endDateTo() != null) {
            scope = scope.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("endDate"), search.endDateTo()));
        }
        return scope;
    }

    @Override
    protected Semester newEntity() {
        return new Semester();
    }

    @Override
    protected void apply(Semester entity, SemesterRequest input, boolean creating) {
        if (creating) {
            requireValue(input.name(), "name");
        }
        if (input.name() != null) {
            entity.setName(trimRequiredText(input.name(), "name"));
        }
        if (creating) {
            requireValue(input.academicYear(), "academicYear");
        }
        boolean academicYearChanged = input.academicYear() != null
                && !input.academicYear().trim().equals(entity.getAcademicYear());
        if (input.academicYear() != null) {
            String academicYear = trimRequiredText(input.academicYear(), ApiField.ACADEMIC_YEAR);
            if (creating || !academicYear.equals(entity.getAcademicYear())) {
                catalogs.requireActiveCode(Catalog.ACADEMIC_YEARS, academicYear);
            }
            entity.setAcademicYear(academicYear);
        }
        if (creating) {
            requireValue(input.type(), "type");
        }
        if (input.type() != null) {
            entity.setType(trimRequiredText(input.type(), "type"));
        }
        if (creating) {
            requireValue(input.startDate(), "startDate");
        }
        if (input.startDate() != null) {
            entity.setStartDate(input.startDate());
        }
        if (creating) {
            requireValue(input.endDate(), "endDate");
        }
        if (input.endDate() != null) {
            entity.setEndDate(input.endDate());
        }
        if (!entity.getEndDate().isAfter(entity.getStartDate())) {
            throw BusinessException.invalid(ApiField.END_DATE, ValidationMessage.END_DATE_AFTER_START);
        }
        String[] years = entity.getAcademicYear().split("-");
        if ((creating || academicYearChanged) && Integer.parseInt(years[1]) != Integer.parseInt(years[0]) + 1) {
            throw BusinessException.invalid(ApiField.ACADEMIC_YEAR, ValidationMessage.ACADEMIC_YEAR_CONSECUTIVE);
        }
    }

    @Override
    protected SemesterResponse toResponse(Semester entity) {
        return new SemesterResponse(
                entity.getId(),
                entity.getName(),
                entity.getAcademicYear(),
                entity.getType(),
                entity.getStartDate(),
                entity.getEndDate());
    }

    @Override
    protected Long getEntityId(Semester entity) {
        return entity.getId();
    }
}
