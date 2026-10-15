package vn.edu.tlu.dms.account.service.impl;

import org.springframework.stereotype.Service;

import vn.edu.tlu.dms.account.api.ManagementUnitRequest;
import vn.edu.tlu.dms.account.api.ManagementUnitResponse;
import vn.edu.tlu.dms.account.domain.ManagementUnit;
import vn.edu.tlu.dms.account.repository.ManagementUnitRepository;
import vn.edu.tlu.dms.account.service.ManagementUnitService;
import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.constants.DmsConstants.Resource;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.impl.AbstractCrudServiceImpl;

@Service
public class ManagementUnitServiceImpl
        extends AbstractCrudServiceImpl<
                ManagementUnit, ManagementUnitRequest, ManagementUnitResponse>
        implements ManagementUnitService {

    private final ScopeGuard guard;

    private final CurrentUser currentUser;

    public ManagementUnitServiceImpl(
            ManagementUnitRepository repository,
            ScopeGuard guard,
            CurrentUser currentUser,
            AuditService auditService) {
        super(repository, auditService, currentUser);
        this.guard = guard;
        this.currentUser = currentUser;
    }

    @Override
    protected void checkRead() {
        guard.requireRole(Role.STUDENT, Role.STAFF, Role.TECHNICIAN, Role.ADMIN);
    }

    @Override
    protected void checkWrite() {
        guard.requireRole(Role.ADMIN);
    }

    @Override
    protected void checkPermission(String action) {
        guard.requirePermission(
                Resource.MANAGEMENT_UNIT + DmsConstants.PERMISSION_SEPARATOR + action);
    }

    @Override
    protected ManagementUnit newEntity() {
        return new ManagementUnit();
    }

    @Override
    protected void apply(ManagementUnit entity, ManagementUnitRequest input, boolean creating) {
        if (creating) {
            requireValue(input.code(), "code");
        }
        if (input.code() != null) {
            entity.setCode(trimRequiredText(input.code(), "code"));
        }
        if (creating) {
            requireValue(input.name(), "name");
        }
        if (input.name() != null) {
            entity.setName(trimRequiredText(input.name(), "name"));
        }
    }

    @Override
    protected ManagementUnitResponse toResponse(ManagementUnit entity) {
        return new ManagementUnitResponse(entity.getId(), entity.getCode(), entity.getName());
    }

    @Override
    protected Long getEntityId(ManagementUnit entity) {
        return entity.getId();
    }
}
