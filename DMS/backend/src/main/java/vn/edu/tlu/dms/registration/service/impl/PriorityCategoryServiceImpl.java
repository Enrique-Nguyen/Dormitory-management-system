package vn.edu.tlu.dms.registration.service.impl;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

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
import vn.edu.tlu.dms.registration.api.PriorityCategoryRequest;
import vn.edu.tlu.dms.registration.api.PriorityCategoryResponse;
import vn.edu.tlu.dms.registration.domain.PriorityCategory;
import vn.edu.tlu.dms.registration.repository.PriorityCategoryRepository;
import vn.edu.tlu.dms.registration.service.PriorityCategoryService;

import java.util.List;
import java.util.Set;

@Service
public class PriorityCategoryServiceImpl
        extends AbstractCrudServiceImpl<
                PriorityCategory, PriorityCategoryRequest, PriorityCategoryResponse>
        implements PriorityCategoryService {

    private final ScopeGuard guard;

    private final CurrentUser currentUser;

    public PriorityCategoryServiceImpl(
            PriorityCategoryRepository repository,
            ScopeGuard guard,
            CurrentUser currentUser,
            AuditService auditService) {
        super(repository, auditService, currentUser);
        this.guard = guard;
        this.currentUser = currentUser;
    }

    @Override
    protected void checkRead() {
        guard.requirePermission(Permission.PRIORITY_CATEGORY_READ);
    }

    @Override
    protected void checkWrite() {
        guard.requireRole(Role.ADMIN);
    }

    @Override
    protected void checkPermission(String action) {
        guard.requirePermission(
                Resource.PRIORITY_CATEGORY + DmsConstants.PERMISSION_SEPARATOR + action);
    }

    @Override
    protected Long getInputId(PriorityCategoryRequest input) {
        return input.id();
    }

    @Override
    protected Set<String> searchableFields() {
        return Set.of("code", "name");
    }

    @Override
    protected Set<String> sortableFields() {
        Actor actor = currentUser.get();
        if (actor.is(Role.STUDENT) || actor.is(Role.TECHNICIAN)) {
            // Trọng số đã được ẩn trong response; không cho suy ra thứ tự ưu tiên qua sort.
            return Set.of("id", "code", "name", "active");
        }
        return Set.of("id", "code", "name", "weight", "active");
    }

    @Override
    protected Specification<PriorityCategory> searchScope(SearchRequest search) {
        Actor actor = currentUser.get();
        // Không công khai diện đã ngừng dùng cho sinh viên/KTV khi chuyển sang phân trang.
        return (root, query, builder) ->
                actor.is(Role.STUDENT) || actor.is(Role.TECHNICIAN)
                        ? builder.isTrue(root.get("active"))
                        : builder.conjunction();
    }

    @Override
    protected PriorityCategory newEntity() {
        return new PriorityCategory();
    }

    @Override
    protected void apply(PriorityCategory entity, PriorityCategoryRequest input, boolean creating) {
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
            requireValue(input.weight(), "weight");
        }
        if (input.weight() != null) {
            entity.setWeight(input.weight());
        }
        if (creating) {
            entity.setActive(input.active() == null ? true : input.active());
        }
        if (input.active() != null) {
            entity.setActive(input.active());
        }
    }

    @Override
    public List<PriorityCategoryResponse> getAll() {
        checkRead();
        Actor actor = currentUser.get();
        return ((PriorityCategoryRepository) repository)
                        .findAll(
                                (r, q, b) ->
                                        actor.is(Role.STUDENT) || actor.is(Role.TECHNICIAN)
                                                ? b.isTrue(r.get("active"))
                                                : b.conjunction())
                        .stream()
                        .map(this::toResponse)
                        .toList();
    }

    @Override
    protected PriorityCategoryResponse toResponse(PriorityCategory entity) {
        return new PriorityCategoryResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                (currentUser.get().is(Role.STUDENT) || currentUser.get().is(Role.TECHNICIAN)
                        ? null
                        : entity.getWeight()),
                entity.getActive());
    }

    @Override
    protected Long getEntityId(PriorityCategory entity) {
        return entity.getId();
    }
}
