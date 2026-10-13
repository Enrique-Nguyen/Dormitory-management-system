package vn.edu.tlu.dms.account.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import vn.edu.tlu.dms.account.security.AccountAccessFilter;
import vn.edu.tlu.dms.common.dto.ApiError;
import vn.edu.tlu.dms.common.security.CurrentUser;

import java.io.IOException;
import java.util.Map;

/**
 * Bảo mật cho SPA dùng session: cookie SESSION là HttpOnly, còn XSRF-TOKEN phải cho JavaScript đọc
 * để gửi header chống CSRF. Vai trò theo URL là lớp bảo vệ đầu tiên; Service tiếp tục kiểm tra đơn
 * vị, đối tượng và trạng thái nghiệp vụ.
 */
@Configuration
public class SecurityConfig {

    @Bean
    /** Đăng nhập JSON lưu SecurityContext vào session một cách tường minh. */
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CurrentUser currentUser,
            ObjectMapper mapper,
            SecurityContextRepository contextRepository)
            throws Exception {
        http.csrf(
                        csrf ->
                                csrf.csrfTokenRepository(
                                                CookieCsrfTokenRepository.withHttpOnlyFalse())
                                        .csrfTokenRequestHandler(
                                                new CsrfTokenRequestAttributeHandler()))
                .securityContext(context -> context.securityContextRepository(contextRepository))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/actuator/health",
                                                "/api/auth/login",
                                                "/api/auth/csrf")
                                        .permitAll()
                                        .requestMatchers("/api/admin/**")
                                        .hasRole("ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        errors ->
                                errors.authenticationEntryPoint(
                                                (req, res, e) ->
                                                        error(
                                                                mapper,
                                                                req,
                                                                res,
                                                                401,
                                                                "UNAUTHENTICATED"))
                                        .accessDeniedHandler(
                                                (req, res, e) ->
                                                        error(mapper, req, res, 403, "FORBIDDEN")))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .addFilterAfter(
                        new AccountAccessFilter(currentUser, mapper),
                        AnonymousAuthenticationFilter.class);
        return http.build();
    }

    private void error(
            ObjectMapper mapper,
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String code)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(
                response.getWriter(),
                new ApiError(code, code, Map.of(), (String) request.getAttribute("requestId")));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
