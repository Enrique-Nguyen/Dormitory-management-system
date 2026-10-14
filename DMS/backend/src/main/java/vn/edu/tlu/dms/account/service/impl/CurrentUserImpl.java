package vn.edu.tlu.dms.account.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.Actor;
import vn.edu.tlu.dms.common.security.CurrentUser;

import java.util.Collection;
import java.util.Map;

@Service
public class CurrentUserImpl implements CurrentUser {

    private final JdbcTemplate jdbc;

    public CurrentUserImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Actor get() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessException("UNAUTHENTICATED", 401);
        }
        return getKeycloakActor(jwt);
    }

    /** Subject liên kết duy nhất; role và phiên bản token phải khớp dữ liệu mới nhất trong DMS. */
    private Actor getKeycloakActor(Jwt jwt) {
        return jdbc.query(
                "SELECT id,role,management_unit_id,cross_unit_access,locked,security_version FROM"
                        + " app_user WHERE keycloak_user_id=?",
                rows -> {
                    if (!rows.next()) {
                        throw new BusinessException("IDENTITY_NOT_LINKED", 403);
                    }
                    if (rows.getBoolean("locked")) {
                        throw new BusinessException("ACCOUNT_LOCKED", 403);
                    }
                    Map<String, Object> access = jwt.getClaimAsMap("realm_access");
                    Object roles = access == null ? null : access.get("roles");
                    Object version = jwt.getClaim("dms_security_version");
                    if (!(roles instanceof Collection<?> values)
                            || !values.contains(rows.getString("role"))
                            || version == null
                            || !Long.toString(rows.getLong("security_version"))
                                    .equals(version.toString())) {
                        throw new BusinessException("SESSION_REVOKED", 401);
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
