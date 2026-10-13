package vn.edu.tlu.dms.account.service.impl;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import vn.edu.tlu.dms.account.repository.AppUserRepository;
import vn.edu.tlu.dms.account.security.DmsPrincipal;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;

@Service
public class CurrentUserImpl implements CurrentUser {

    private final AppUserRepository repository;

    public CurrentUserImpl(AppUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public Actor get() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof DmsPrincipal principal)) {
            throw new BusinessException("UNAUTHENTICATED", 401);
        }
        var user =
                repository
                        .findById(principal.id())
                        .orElseThrow(() -> new BusinessException("UNAUTHENTICATED", 401));
        if (Boolean.TRUE.equals(user.getLocked())) {
            throw new BusinessException("ACCOUNT_LOCKED", 403);
        }
        return new Actor(
                user.getId(),
                user.getRole(),
                user.getManagementUnitId(),
                Boolean.TRUE.equals(user.getCrossUnitAccess()),
                Boolean.TRUE.equals(user.getMustChangePassword()));
    }
}
