package vn.edu.tlu.dms.account.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import vn.edu.tlu.dms.common.dto.ApiError;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;

import java.io.IOException;
import java.util.Set;

/** Đọc lại cờ tài khoản mỗi request để việc khóa/reset mật khẩu có hiệu lực với phiên đang mở. */
public class AccountAccessFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED =
            Set.of(
                    "/api/auth/me",
                    "/api/auth/change-password",
                    "/api/auth/logout",
                    "/api/auth/csrf");

    private final CurrentUser currentUser;

    private final ObjectMapper mapper;

    public AccountAccessFilter(CurrentUser currentUser, ObjectMapper mapper) {
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String apiPath = request.getRequestURI().substring(request.getContextPath().length());
        if (apiPath.startsWith("/api/")
                && auth != null
                && auth.getPrincipal() instanceof DmsPrincipal) {
            try {
                var actor = currentUser.get();
                if (actor.mustChangePassword() && !ALLOWED.contains(apiPath)) {
                    throw new BusinessException("PASSWORD_CHANGE_REQUIRED", 403);
                }
            } catch (BusinessException e) {
                // Tài khoản bị khóa vẫn phải đăng xuất được để xóa phiên đang mở.
                if (e.getErrorCode().equals("ACCOUNT_LOCKED")
                        && apiPath.equals("/api/auth/logout")) {
                    chain.doFilter(request, response);
                    return;
                }
                response.setStatus(e.getStatus());
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                mapper.writeValue(
                        response.getWriter(),
                        new ApiError(
                                e.getErrorCode(),
                                e.getErrorCode(),
                                e.getDetails(),
                                (String) request.getAttribute("requestId")));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
