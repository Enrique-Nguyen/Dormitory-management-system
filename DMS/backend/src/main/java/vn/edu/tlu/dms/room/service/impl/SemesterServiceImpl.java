package vn.edu.tlu.dms.room.service.impl;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

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

import java.util.Set;

@Service
public class SemesterServiceImpl
        extends AbstractCrudServiceImpl<Semester, SemesterRequest, SemesterResponse>
        implements SemesterService {

    private final ScopeGuard guard;

    private final CurrentUser currentUser;

    public SemesterServiceImpl(
            SemesterRepository repository,
            ScopeGuard guard,
            CurrentUser currentUser,
            AuditService auditService) {
        super(repository, auditService, currentUser);
        this.guard = guard;
        this.currentUser = currentUser;
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
        return (root, query, builder) ->
                search.type() == null || search.type().isBlank()
                        ? builder.conjunction()
                        : builder.equal(root.get("type"), search.type());
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
        if (input.academicYear() != null) {
            entity.setAcademicYear(trimRequiredText(input.academicYear(), "academicYear"));
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
            throw BusinessException.invalid("endDate", "Ngày kết thúc phải sau ngày bắt đầu");
        }
        String[] years = entity.getAcademicYear().split("-");
        if (Integer.parseInt(years[1]) != Integer.parseInt(years[0]) + 1) {
            throw BusinessException.invalid("academicYear", "Hai năm phải liền nhau");
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
