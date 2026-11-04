package vn.edu.tlu.dms.account.service.impl;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.edu.tlu.dms.account.api.ManagementUnitRequest;
import vn.edu.tlu.dms.account.api.ManagementUnitResponse;
import vn.edu.tlu.dms.account.domain.ManagementUnit;
import vn.edu.tlu.dms.account.repository.ManagementUnitRepository;
import vn.edu.tlu.dms.account.service.ManagementUnitService;
import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.constants.DmsConstants.Resource;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.impl.AbstractCrudServiceImpl;

import java.util.Locale;
import java.util.Set;

@Service
public class ManagementUnitServiceImpl
        extends AbstractCrudServiceImpl<
                ManagementUnit, ManagementUnitRequest, ManagementUnitResponse>
        implements ManagementUnitService {

    private final ScopeGuard guard;

    public ManagementUnitServiceImpl(
            ManagementUnitRepository repository,
            ScopeGuard guard,
            CurrentUser currentUser,
            AuditService auditService) {
        super(repository, auditService, currentUser);
        this.guard = guard;
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
    protected Long getInputId(ManagementUnitRequest input) {
        return input.id();
    }

    @Override
    protected Set<String> searchableFields() {
        return Set.of("code", "name");
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of("id", "code", "name");
    }

    @Override
    protected Specification<ManagementUnit> searchScope(SearchRequest search) {
        return (root, query, builder) -> {
            var predicate = builder.conjunction();
            if (search.code() != null && !search.code().isBlank()) {
                predicate = builder.and(predicate, builder.equal(root.get("code"), search.code().trim()));
            }
            if (search.name() != null && !search.name().isBlank()) {
                predicate = builder.and(predicate, builder.greaterThan(builder.locate(
                        builder.lower(root.get("name")), search.name().trim().toLowerCase(Locale.ROOT)), 0));
            }
            return predicate;
        };
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
