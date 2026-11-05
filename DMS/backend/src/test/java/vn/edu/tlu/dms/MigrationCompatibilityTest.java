package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Kiểm tra nâng cấp V10 cũ trên CSDL riêng, không chỉ kiểm tra cài mới. */
@Testcontainers
class MigrationCompatibilityTest {
    private static final int LEGACY_CHECKSUM = -1224946798;
    private static final int CURRENT_CHECKSUM = 1020332633;

    @Container
    final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private JdbcTemplate jdbc;

    @BeforeEach
    void createDatabaseAtVersion15() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                Objects.requireNonNull(postgres.getJdbcUrl()),
                Objects.requireNonNull(postgres.getUsername()),
                Objects.requireNonNull(postgres.getPassword())));
        flyway("15").migrate();
    }

    @Test
    void legacyChecksumIsRecoveredAndVersion16PreservesData() throws Exception {
        legacyVersion10();
        jdbc.update("INSERT INTO academic_year(code,name) VALUES('2098-2099','Giữ nguyên năm học cũ')");
        var otherMigrations = jdbc.queryForList(
                "SELECT version,checksum,success FROM flyway_schema_history WHERE version<>'10' ORDER BY installed_rank");
        assertThatThrownBy(() -> flyway(null).migrate())
                .isInstanceOf(FlywayValidateException.class).hasMessageContaining("version 10");

        recoverVersion10();
        assertThat(checksum()).isEqualTo(CURRENT_CHECKSUM);
        assertThat(jdbc.queryForList(
                "SELECT version,checksum,success FROM flyway_schema_history WHERE version<>'10' ORDER BY installed_rank"))
                .isEqualTo(otherMigrations);
        flyway(null).migrate();

        assertThat(flyway(null).validateWithResult().validationSuccessful).isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT name FROM academic_year WHERE code='2098-2099'", String.class))
                .isEqualTo("Giữ nguyên năm học cũ");
        assertThat(jdbc.queryForObject(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid='academic_year'::regclass AND conname='ck_academic_year_code'",
                String.class)).doesNotContain("substring");
    }

    @Test
    void currentChecksumCanBeCheckedRepeatedlyWithoutChangingHistory() throws Exception {
        var before = jdbc.queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank");
        recoverVersion10();
        recoverVersion10();
        assertThat(jdbc.queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank"))
                .isEqualTo(before);
        flyway(null).migrate();
        assertThat(flyway(null).validateWithResult().validationSuccessful).isTrue();
    }

    @Test
    void unknownChecksumIsRejectedWithoutChangingHistory() {
        jdbc.update("UPDATE flyway_schema_history SET checksum=123 WHERE version='10'");
        assertThatThrownBy(this::recoverVersion10).hasMessageContaining("Checksum V10");
        assertThat(checksum()).isEqualTo(123);
    }

    @Test
    void unexpectedSchemaIsRejectedWithoutChangingHistory() {
        legacyVersion10();
        jdbc.execute("ALTER TABLE academic_year DROP CONSTRAINT ck_academic_year_code");
        jdbc.execute("ALTER TABLE academic_year ADD CONSTRAINT ck_academic_year_code CHECK(length(code)=9)");
        assertThatThrownBy(this::recoverVersion10).hasMessageContaining("Ràng buộc năm học");
        assertThat(checksum()).isEqualTo(LEGACY_CHECKSUM);
    }

    @Test
    void failedMigrationIsRejectedWithoutMarkingItSuccessful() {
        legacyVersion10();
        jdbc.update("UPDATE flyway_schema_history SET success=false WHERE version='9'");
        assertThatThrownBy(this::recoverVersion10).hasMessageContaining("migration thất bại");
        assertThat(checksum()).isEqualTo(LEGACY_CHECKSUM);
        assertThat(jdbc.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version='9'", Boolean.class)).isFalse();
    }

    private Flyway flyway(String target) {
        var configuration = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration");
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private void legacyVersion10() {
        jdbc.execute("ALTER TABLE academic_year DROP CONSTRAINT ck_academic_year_code");
        jdbc.execute("ALTER TABLE academic_year ADD CONSTRAINT ck_academic_year_code CHECK ("
                + "code ~ '^[0-9]{4}-[0-9]{4}$' AND substring(code,6,4)::integer=substring(code,1,4)::integer+1)");
        jdbc.update("UPDATE flyway_schema_history SET checksum=? WHERE version='10'", LEGACY_CHECKSUM);
    }

    private int checksum() {
        return Objects.requireNonNull(jdbc.queryForObject(
                "SELECT checksum FROM flyway_schema_history WHERE version='10'", Integer.class));
    }

    private void recoverVersion10() throws Exception {
        jdbc.execute(Objects.requireNonNull(Files.readString(Path.of("../scripts/recover-v10-checksum.sql"))));
    }
}
