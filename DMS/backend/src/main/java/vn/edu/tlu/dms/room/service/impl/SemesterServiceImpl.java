package vn.edu.tlu.dms.room.service.impl;

import org.springframework.stereotype.Service;

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
        guard.requireRole("STUDENT", "STAFF", "TECHNICIAN", "ADMIN");
    }

    @Override
    protected void checkWrite() {
        guard.requireRole("STAFF");
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
