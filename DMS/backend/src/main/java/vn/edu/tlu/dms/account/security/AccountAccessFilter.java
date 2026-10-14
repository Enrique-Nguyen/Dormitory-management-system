package vn.edu.tlu.dms.account.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import vn.edu.tlu.dms.common.dto.ApiError;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;

import java.io.IOException;

/** Kiểm tra liên kết, khóa, vai trò và version DMS ngay cả khi JWT vẫn còn hạn. */
public class AccountAccessFilter extends OncePerRequestFilter {

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
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")
                && authentication != null
                && authentication.getPrincipal() instanceof Jwt) {
            try {
                currentUser.get();
            } catch (BusinessException error) {
                SecurityContextHolder.clearContext();
                response.setStatus(error.getStatus());
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                mapper.writeValue(
                        response.getWriter(),
                        new ApiError(
                                error.getErrorCode(),
                                error.getErrorCode(),
                                error.getDetails(),
                                (String) request.getAttribute("requestId")));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
