package vn.edu.tlu.dms.account.service.impl;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.KeycloakClaim;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;

import java.util.Collection;
import java.util.Map;

@Service
public class CurrentUserImpl implements CurrentUser {

    /** Actor đã nạp trong request hiện tại, tránh SELECT app_user lặp lại nhiều lần mỗi request. */
    private static final String ACTOR_ATTRIBUTE = CurrentUserImpl.class.getName() + ".ACTOR";

    private final JdbcTemplate jdbc;

    public CurrentUserImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Actor get() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED);
        }
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null
                && attributes.getAttribute(ACTOR_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)
                        instanceof CachedActor cached
                && cached.subject().equals(jwt.getSubject())) {
            return cached.actor();
        }
        var actor = getKeycloakActor(jwt);
        if (attributes != null) {
            attributes.setAttribute(
                    ACTOR_ATTRIBUTE,
                    new CachedActor(jwt.getSubject(), actor),
                    RequestAttributes.SCOPE_REQUEST);
        }
        return actor;
    }

    /** Actor kèm subject của token đã dùng để nạp, để không dùng nhầm cho token khác. */
    private record CachedActor(String subject, Actor actor) {}

    /** Subject liên kết duy nhất; role và phiên bản token phải khớp dữ liệu mới nhất trong DMS. */
    private Actor getKeycloakActor(Jwt jwt) {
        return jdbc.query(
                "SELECT id,role,management_unit_id,cross_unit_access,locked,security_version FROM"
                        + " app_user WHERE keycloak_user_id=?",
                rows -> {
                    if (!rows.next()) {
                        throw new BusinessException(
                                ErrorCode.IDENTITY_NOT_LINKED, HttpStatus.FORBIDDEN);
                    }
                    if (rows.getBoolean("locked")) {
                        throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, HttpStatus.FORBIDDEN);
                    }
                    Map<String, Object> access = jwt.getClaimAsMap(KeycloakClaim.REALM_ACCESS);
                    Object roles = access == null ? null : access.get(KeycloakClaim.ROLES);
                    Object version = jwt.getClaim(KeycloakClaim.SECURITY_VERSION);
                    if (!(roles instanceof Collection<?> values)
                            || !values.contains(rows.getString("role"))
                            || version == null
                            || !Long.toString(rows.getLong("security_version"))
                                    .equals(version.toString())) {
                        throw new BusinessException(
                                ErrorCode.SESSION_REVOKED, HttpStatus.UNAUTHORIZED);
                    }
                    // Keycloak hoàn tất required action UPDATE_PASSWORD trước khi cấp token đăng
                    // nhập.
                    return new Actor(
                            rows.getLong("id"),
                            rows.getString("role"),
                            rows.getObject("management_unit_id", Long.class),
                            rows.getBoolean("cross_unit_access"),
                            false);
                },
                jwt.getSubject());
    }
}
