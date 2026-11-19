package vn.edu.tlu.dms.account.service.impl;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.edu.tlu.dms.account.api.ManagementUnitRequest;
import vn.edu.tlu.dms.account.api.ManagementUnitResponse;
import vn.edu.tlu.dms.account.domain.ManagementUnit;
import vn.edu.tlu.dms.account.repository.ManagementUnitRepository;
import vn.edu.tlu.dms.account.service.AccountReferenceService;
import vn.edu.tlu.dms.account.service.ManagementUnitService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Resource;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;
import vn.edu.tlu.dms.common.constants.DmsConstants.ValidationMessage;
import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.dto.SearchRequest;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;
import vn.edu.tlu.dms.common.service.impl.AbstractCrudServiceImpl;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ManagementUnitServiceImpl
        extends AbstractCrudServiceImpl<
                ManagementUnit, ManagementUnitRequest, ManagementUnitResponse>
        implements ManagementUnitService {

    private final ScopeGuard guard;

    private final AccountReferenceService references;

    public ManagementUnitServiceImpl(
            ManagementUnitRepository repository,
            ScopeGuard guard,
            CurrentUser currentUser,
            AuditService auditService,
            AccountReferenceService references) {
        super(repository, auditService, currentUser);
        this.guard = guard;
        this.references = references;
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
        return Set.of("id", "code", "name", "shortName", "active");
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
            if (search.active() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("active"), search.active()));
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
            requireValue(input.name(), "name");
        }
        if (input.code() != null) {
            entity.setCode(trimRequiredText(input.code(), "code"));
        }
        if (input.name() != null) {
            entity.setName(trimRequiredText(input.name(), "name"));
        }
        entity.setShortName(mergeText(input.shortName(), entity.getShortName()));
        entity.setAddress(mergeText(input.address(), entity.getAddress()));
        entity.setHotline(mergeText(input.hotline(), entity.getHotline()));
        entity.setEmail(mergeText(input.email(), entity.getEmail()));
        entity.setWorkingHours(mergeText(input.workingHours(), entity.getWorkingHours()));
        entity.setNote(mergeText(input.note(), entity.getNote()));
        entity.setActive(mergeValue(input.active(), creating ? Boolean.TRUE : entity.getActive()));
        if (input.headUserId() != null) {
            if (creating) {
                // Trưởng ban phải thuộc đơn vị đã tồn tại; chọn sau khi tạo đơn vị và gán cán bộ.
                throw BusinessException.invalid("headUserId", ValidationMessage.FIELD_NOT_WRITABLE);
            }
            references.requireStaffOfUnit(input.headUserId(), entity.getId());
            entity.setHeadUserId(input.headUserId());
        }
    }

    @Override
    protected ManagementUnitResponse toResponse(ManagementUnit entity) {
        String headName = null;
        if (entity.getHeadUserId() != null) {
            var head = references.users(List.of(entity.getHeadUserId())).get(entity.getHeadUserId());
            headName = head == null ? null : head.fullName();
        }
        return new ManagementUnitResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getShortName(),
                entity.getAddress(),
                entity.getHotline(),
                entity.getEmail(),
                entity.getHeadUserId(),
                headName,
                entity.getWorkingHours(),
                entity.getActive(),
                entity.getNote());
    }

    @Override
    protected Long getEntityId(ManagementUnit entity) {
        return entity.getId();
    }
}
