package vn.edu.tlu.dms.account.service.impl;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.stereotype.Service;

import vn.edu.tlu.dms.account.repository.AppUserRepository;
import vn.edu.tlu.dms.account.security.DmsPrincipal;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;

@Service
public class CurrentUserImpl implements CurrentUser {

    /** Actor đã nạp trong request hiện tại, tránh SELECT app_user lặp lại nhiều lần mỗi request. */
    private static final String ACTOR_ATTRIBUTE = CurrentUserImpl.class.getName() + ".ACTOR";

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
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null
                && attributes.getAttribute(ACTOR_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)
                        instanceof Actor cached
                && cached.id().equals(principal.id())) {
            return cached;
        }
        var user =
                repository
                        .findById(principal.id())
                        .orElseThrow(() -> new BusinessException("UNAUTHENTICATED", 401));
        if (Boolean.TRUE.equals(user.getLocked())) {
            throw new BusinessException("ACCOUNT_LOCKED", 403);
        }
        var actor =
                new Actor(
                        user.getId(),
                        user.getRole(),
                        user.getManagementUnitId(),
                        Boolean.TRUE.equals(user.getCrossUnitAccess()),
                        Boolean.TRUE.equals(user.getMustChangePassword()));
        if (attributes != null) {
            attributes.setAttribute(ACTOR_ATTRIBUTE, actor, RequestAttributes.SCOPE_REQUEST);
        }
        return actor;
    }

    @Override
    public void invalidate() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.removeAttribute(ACTOR_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        }
    }
}
