package vn.edu.tlu.dms.account.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import vn.edu.tlu.dms.account.security.AccountAccessFilter;
import vn.edu.tlu.dms.common.dto.ApiError;
import vn.edu.tlu.dms.common.security.CurrentUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Xác thực Bearer JWT; phạm vi nghiệp vụ vẫn đọc trực tiếp từ CSDL DMS. */
@Configuration
@EnableConfigurationProperties(SecurityConfig.KeycloakProperties.class)
public class SecurityConfig {

    private static final Set<String> ROLES = Set.of("ADMIN", "STAFF", "TECHNICIAN", "STUDENT");

    @Bean
    public JwtDecoder jwtDecoder(KeycloakProperties properties) {
        String issuer = properties.serverUrl() + "/realms/" + properties.realm();
        // JWK tải khi xác thực token; backend vẫn khởi động được nếu Keycloak chưa sẵn sàng.
        var decoder =
                NimbusJwtDecoder.withJwkSetUri(issuer + "/protocol/openid-connect/certs").build();
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(issuer),
                        token ->
                                token.getAudience().contains(properties.audience())
                                        ? OAuth2TokenValidatorResult.success()
                                        : OAuth2TokenValidatorResult.failure(
                                                new OAuth2Error(
                                                        "invalid_token",
                                                        "Invalid audience",
                                                        null))));
        return decoder;
    }

    @Bean
    SecurityFilterChain keycloakFilterChain(
            HttpSecurity http, CurrentUser current, ObjectMapper mapper) throws Exception {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                jwt -> {
                    Map<String, Object> access = jwt.getClaimAsMap("realm_access");
                    Object values = access == null ? null : access.get("roles");
                    if (!(values instanceof Collection<?> roles)) {
                        return List.of();
                    }
                    return roles.stream()
                            .map(Object::toString)
                            .filter(ROLES::contains)
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .map(authority -> (GrantedAuthority) authority)
                            .toList();
                });
        // API chỉ nhận Bearer từ header, không xác thực bằng cookie; không dùng CSRF session.
        http.sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/actuator/health")
                                        .permitAll()
                                        .requestMatchers("/api/admin/**")
                                        .hasRole("ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(
                        oauth ->
                                oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                                        .authenticationEntryPoint(
                                                (request, response, failure) -> {
                                                    response.setStatus(401);
                                                    response.setContentType("application/json");
                                                    mapper.writeValue(
                                                            response.getWriter(),
                                                            new ApiError(
                                                                    "UNAUTHENTICATED",
                                                                    "UNAUTHENTICATED",
                                                                    Map.of(),
                                                                    null));
                                                }))
                .exceptionHandling(
                        errors ->
                                errors.accessDeniedHandler(
                                        (request, response, failure) -> {
                                            response.setStatus(403);
                                            response.setContentType("application/json");
                                            mapper.writeValue(
                                                    response.getWriter(),
                                                    new ApiError(
                                                            "FORBIDDEN",
                                                            "FORBIDDEN",
                                                            Map.of(),
                                                            null));
                                        }))
                .addFilterAfter(
                        new AccountAccessFilter(current, mapper),
                        AnonymousAuthenticationFilter.class);
        return http.build();
    }

    /** Secret chỉ thuộc backend; frontend chỉ nhận URL, realm và public client ID. */
    @ConfigurationProperties(prefix = "dms.keycloak")
    public static record KeycloakProperties(
            String serverUrl,
            String realm,
            String audience,
            String adminClientId,
            String adminClientSecret) {

        public KeycloakProperties {
            serverUrl = serverUrl.replaceAll("/+$", "");
            if (!realm.matches("[A-Za-z0-9_-]+") || audience.isBlank() || adminClientId.isBlank()) {
                throw new IllegalArgumentException(
                        "Cấu hình realm, audience hoặc admin client không hợp lệ");
            }
        }
    }
}
