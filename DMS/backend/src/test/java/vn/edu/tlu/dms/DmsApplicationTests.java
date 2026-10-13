package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DmsApplicationTests {

    @Autowired JdbcTemplate jdbc;

    @Test
    void contextLoadsAndFlywayCreatesSchema() {
        Integer tables =
                jdbc.queryForObject(
                        "SELECT count(*) FROM information_schema.tables WHERE table_schema ="
                                + " 'public' AND table_type = 'BASE TABLE'",
                        Integer.class);
        // 33 bảng nghiệp vụ (CSDL v0.3) + 2 bảng Spring Session + flyway_schema_history
        assertThat(tables).isEqualTo(36);
    }
}
