package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Dùng cookie/header CSRF thật, không dùng helper csrf() vì helper thay repository trong context.
 * Property riêng buộc tách context khỏi các case MockMvc giả lập token của CrudApiIntegrationTest.
 */
@SpringBootTest(properties = "dms.test.csrf=browser")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CsrfSessionIntegrationTest {

    @Autowired MockMvc mvc;

    @Autowired JdbcTemplate jdbc;

    @Autowired ObjectMapper mapper;

    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void browserCookieCsrfFlowRotatesTokenAndLogsOut() throws Exception {
        String username = "browser-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update(
                """
                INSERT INTO app_user(username,password_hash,role,full_name,must_change_password)
                VALUES(?,?,'ADMIN','Admin kiểm thử',false)
                """,
                username,
                passwordEncoder.encode("Password123"));
        var tokenResponse =
                mvc.perform(get("/api/auth/csrf")).andExpect(status().isNoContent()).andReturn();
        var initialToken = tokenResponse.getResponse().getCookie("XSRF-TOKEN");
        assertThat(initialToken).isNotNull();
        var login =
                mvc.perform(
                                post("/api/auth/login")
                                        .cookie(initialToken)
                                        .header("X-XSRF-TOKEN", initialToken.getValue())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                mapper.writeValueAsString(
                                                        Map.of(
                                                                "username",
                                                                username,
                                                                "password",
                                                                "Password123"))))
                        .andExpect(status().isOk())
                        .andReturn();
        var sessionCookie = login.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).isNotNull();
        var nextTokenResponse =
                mvc.perform(get("/api/auth/csrf").cookie(sessionCookie))
                        .andExpect(status().isNoContent())
                        .andReturn();
        var nextToken = nextTokenResponse.getResponse().getCookie("XSRF-TOKEN");
        assertThat(nextToken).isNotNull();
        assertThat(nextToken.getValue()).isNotEqualTo(initialToken.getValue());
        mvc.perform(post("/api/auth/logout").cookie(sessionCookie))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/auth/logout")
                                .cookie(sessionCookie, nextToken)
                                .header("X-XSRF-TOKEN", nextToken.getValue()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").cookie(sessionCookie)).andExpect(status().isUnauthorized());
    }
}
