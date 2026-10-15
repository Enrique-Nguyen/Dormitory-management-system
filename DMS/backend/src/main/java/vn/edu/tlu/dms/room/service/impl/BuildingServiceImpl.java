package vn.edu.tlu.dms.room.service.impl;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.edu.tlu.dms.account.service.AccountReferenceService;
import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Resource;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.impl.AbstractCrudServiceImpl;
import vn.edu.tlu.dms.room.api.BuildingRequest;
import vn.edu.tlu.dms.room.api.BuildingResponse;
import vn.edu.tlu.dms.room.domain.Building;
import vn.edu.tlu.dms.room.repository.BuildingRepository;
import vn.edu.tlu.dms.room.service.BuildingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class BuildingServiceImpl
        extends AbstractCrudServiceImpl<Building, BuildingRequest, BuildingResponse>
        implements BuildingService {

    private final ScopeGuard guard;

    private final CurrentUser currentUser;

    private final AccountReferenceService references;

    public BuildingServiceImpl(
            BuildingRepository repository,
            ScopeGuard guard,
            CurrentUser currentUser,
            AuditService auditService,
            AccountReferenceService references) {
        super(repository, auditService, currentUser);
        this.guard = guard;
        this.currentUser = currentUser;
        this.references = references;
    }

    @Override
    protected void checkRead() {
        guard.requirePermission(Permission.BUILDING_READ);
    }

    @Override
    protected void checkWrite() {
        guard.requireRole(Role.ADMIN);
    }

    @Override
    protected void checkPermission(String action) {
        guard.requirePermission(Resource.BUILDING + DmsConstants.PERMISSION_SEPARATOR + action);
    }

    @Override
    protected Long getInputId(BuildingRequest input) {
        return input.id();
    }

    @Override
    protected Set<String> searchableFields() {
        return Set.of("code", "name");
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of("id", "managementUnitId", "code", "name", "depositAmount");
    }

    @Override
    protected Specification<Building> searchScope(SearchRequest search) {
        Actor actor = currentUser.get();
        return (root, query, builder) -> {
            var filters = new ArrayList<Predicate>();
            // Cùng giới hạn như getAll: lọc trước cả truy vấn dữ liệu và truy vấn đếm.
            if (actor.is(Role.STAFF) && !actor.canReadAllUnits() || actor.is(Role.TECHNICIAN)) {
                filters.add(builder.equal(root.get("managementUnitId"), actor.unitId()));
            }
            if (search.managementUnitId() != null) {
                filters.add(builder.equal(root.get("managementUnitId"), search.managementUnitId()));
            }
            return builder.and(filters.toArray(Predicate[]::new));
        };
    }

    @Override
    protected Building newEntity() {
        return new Building();
    }

    @Override
    protected void apply(Building entity, BuildingRequest input, boolean creating) {
        if (creating) {
            requireValue(input.managementUnitId(), "managementUnitId");
        }
        if (input.managementUnitId() != null) {
            entity.setManagementUnitId(input.managementUnitId());
        }
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
        if (creating) {
            requireValue(input.depositAmount(), "depositAmount");
        }
        if (input.depositAmount() != null) {
            entity.setDepositAmount(input.depositAmount());
        }
        references.requireUnit(entity.getManagementUnitId());
    }

    @Override
    public List<BuildingResponse> getAll() {
        checkRead();
        Actor actor = currentUser.get();
        if (actor.is(Role.STAFF) && !actor.canReadAllUnits() || actor.is(Role.TECHNICIAN)) {
            return ((BuildingRepository) repository)
                            .findAll(
                                    (r, q, b) -> b.equal(r.get("managementUnitId"), actor.unitId()))
                            .stream()
                            .map(this::toResponse)
                            .toList();
        }
        return super.getAll();
    }

    @Override
    public BuildingResponse getById(Long id) {
        checkRead();
        var entity = findEntityById(id);
        Actor actor = currentUser.get();
        if (actor.is(Role.STAFF) || actor.is(Role.TECHNICIAN)) {
            guard.requireReadableUnit(entity.getManagementUnitId());
        }
        return toResponse(entity);
    }

    @Override
    protected BuildingResponse toResponse(Building entity) {
        return new BuildingResponse(
                entity.getId(),
                entity.getManagementUnitId(),
                entity.getCode(),
                entity.getName(),
                entity.getDepositAmount());
    }

    @Override
    protected Long getEntityId(Building entity) {
        return entity.getId();
    }
}
