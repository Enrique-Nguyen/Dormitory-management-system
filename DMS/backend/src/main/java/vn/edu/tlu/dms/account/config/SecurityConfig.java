package vn.edu.tlu.dms.account.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.tlu.dms.account.security.AccountAccessFilter;
import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.KeycloakClaim;
import vn.edu.tlu.dms.common.dto.ApiError;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Xác thực Bearer JWT; phạm vi nghiệp vụ vẫn đọc trực tiếp từ CSDL DMS. */
@Configuration
@EnableMethodSecurity(proxyTargetClass = true, prePostEnabled = false)
@EnableConfigurationProperties(SecurityConfig.KeycloakProperties.class)
public class SecurityConfig {

    private static final Set<String> ROLES = DmsConstants.Role.ALL;

    /**
     * Advisor Spring Security đọc @RequirePrivilege trước khi gọi API. Quét mọi REST method được
     * mapped trong ứng dụng để API quên annotation cũng bị chặn. ObjectProvider đọc ScopeGuard lúc
     * thực thi, tránh khởi tạo Service/CSDL sớm khi Spring đang tạo proxy bảo mật.
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    static Advisor requirePrivilegeAdvisor(ObjectProvider<ScopeGuard> guards) {
        var mappedApi =
                new StaticMethodMatcherPointcut() {
                    @Override
                    public boolean matches(Method method, Class<?> targetClass) {
                        return targetClass.getPackageName().startsWith("vn.edu.tlu.dms")
                                && AnnotatedElementUtils.hasAnnotation(
                                        targetClass, RestController.class)
                                && AnnotatedElementUtils.hasAnnotation(
                                        method, RequestMapping.class);
                    }
                };
        return new AuthorizationManagerBeforeMethodInterceptor(
                mappedApi,
                (authentication, invocation) ->
                        new AuthorizationDecision(
                                guards.getObject().hasDeclaredPrivilege(invocation)));
    }

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
                                                        OAuth2ErrorCodes.INVALID_TOKEN,
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
                    Map<String, Object> access = jwt.getClaimAsMap(KeycloakClaim.REALM_ACCESS);
                    Object values = access == null ? null : access.get(KeycloakClaim.ROLES);
                    if (!(values instanceof Collection<?> roles)) {
                        return List.of();
                    }
                    return roles.stream()
                            .map(Object::toString)
                            .filter(ROLES::contains)
                            .map(
                                    role ->
                                            new SimpleGrantedAuthority(
                                                    KeycloakClaim.AUTHORITY_PREFIX + role))
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
                                        .hasRole(DmsConstants.Role.ADMIN)
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(
                        oauth ->
                                oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                                        .authenticationEntryPoint(
                                                (request, response, failure) -> {
                                                    response.setStatus(
                                                            HttpStatus.UNAUTHORIZED.value());
                                                    response.setContentType(
                                                            MediaType.APPLICATION_JSON_VALUE);
                                                    mapper.writeValue(
                                                            response.getWriter(),
                                                            new ApiError(
                                                                    ErrorCode.UNAUTHENTICATED,
                                                                    ErrorCode.UNAUTHENTICATED,
                                                                    Map.of(),
                                                                    null));
                                                }))
                .exceptionHandling(
                        errors ->
                                errors.accessDeniedHandler(
                                        (request, response, failure) -> {
                                            response.setStatus(HttpStatus.FORBIDDEN.value());
                                            response.setContentType(
                                                    MediaType.APPLICATION_JSON_VALUE);
                                            mapper.writeValue(
                                                    response.getWriter(),
                                                    new ApiError(
                                                            ErrorCode.FORBIDDEN,
                                                            ErrorCode.FORBIDDEN,
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

        @Override
        public String toString() {
            return "KeycloakProperties[serverUrl=" + serverUrl + ", realm=" + realm
                    + ", audience=" + audience + ", adminClientId=" + adminClientId
                    + ", adminClientSecret=REDACTED]";
        }

        public KeycloakProperties {
            serverUrl = serverUrl.replaceAll("/+$", "");
            if (!realm.matches("[A-Za-z0-9_-]+") || audience.isBlank() || adminClientId.isBlank()) {
                throw new IllegalArgumentException(
                        "Cấu hình realm, audience hoặc admin client không hợp lệ");
            }
        }
    }
}
