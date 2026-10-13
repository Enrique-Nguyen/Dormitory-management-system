package vn.edu.tlu.dms.common.security;

import org.springframework.stereotype.Component;

import vn.edu.tlu.dms.common.exception.BusinessException;

@Component
public class ScopeGuard {

    private final CurrentUser currentUser;

    public ScopeGuard(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    public void requireRole(String... roles) {
        Actor actor = currentUser.get();
        for (String role : roles) {
            if (actor.is(role)) {
                return;
            }
        }
        throw new BusinessException("FORBIDDEN", 403);
    }

    public void requireUnit(Long unitId) {
        Actor actor = currentUser.get();
        if (actor.unitId() == null || !actor.unitId().equals(unitId)) {
            throw BusinessException.notFound();
        }
    }

    /**
     * Quyền tổng hợp chỉ mở rộng phạm vi đọc; ngoại lệ tạo đợt nhiều đơn vị được quy định tại API
     * 4.2.
     */
    public void requireReadableUnit(Long unitId) {
        Actor actor = currentUser.get();
        if (actor.is("STAFF") && actor.crossUnitAccess()) {
            return;
        }
        requireUnit(unitId);
    }
}
