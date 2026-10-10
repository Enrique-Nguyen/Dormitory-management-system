package vn.edu.tlu.dms.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Cấu hình bảo mật khung (SAD mục 7). Mới có phần nền: CSRF bằng cookie {@code XSRF-TOKEN},
 * BCrypt, mọi API đều phải đăng nhập.
 *
 * <p>TODO(TV1): đăng nhập JSON {@code POST /api/auth/login}, {@code /api/auth/me}, đăng xuất,
 * trả 401/403 dạng JSON thay vì trang mặc định, bộ lọc bắt đổi mật khẩu.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // Axios gửi lại nguyên giá trị cookie trong header X-XSRF-TOKEN (SPA, không mã hóa BREACH)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
