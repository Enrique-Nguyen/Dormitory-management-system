package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CatalogMigrationIntegrationTest {
    @Autowired DataSource dataSource;

    @Test
    void sampleSeedMatchesNormalizedSchemaAndRemainsIdempotent() throws Exception {
        String schema = "catalog_seed_" + UUID.randomUUID().toString().replace("-", "");
        Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load().migrate();
        try (Connection connection = dataSource.getConnection(); Statement sql = connection.createStatement()) {
            connection.setSchema(schema);
            String seed = Files.readString(Path.of("../scripts/sample-data.sql"));
            sql.execute(seed);
            int rooms;
            try (ResultSet row = sql.executeQuery("SELECT count(*) FROM room")) {
                row.next();
                rooms = row.getInt(1);
                assertThat(rooms).isPositive();
            }
            sql.execute(seed);
            try (ResultSet row = sql.executeQuery("SELECT count(*) FROM room")) {
                row.next();
                assertThat(row.getInt(1)).isEqualTo(rooms);
            }
        } finally {
            try (Connection connection = dataSource.getConnection(); Statement sql = connection.createStatement()) {
                sql.executeUpdate("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    @Test
    void legacyLabelsAndLongAmenitiesSurviveNormalization() throws Exception {
        String schema = "catalog_migration_" + UUID.randomUUID().toString().replace("-", "");
        Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                .target("9").load().migrate();
        try {
            try (Connection connection = dataSource.getConnection(); Statement sql = connection.createStatement()) {
                connection.setSchema(schema);
                sql.executeUpdate("INSERT INTO app_user(username,role,full_name) VALUES ('legacy','STUDENT','Legacy student')");
                sql.executeUpdate("INSERT INTO student_profile(user_id,student_code,gender,faculty,cohort,hometown) SELECT id,'legacy','M',' Faculty preserved ','K65',' Hometown preserved ' FROM app_user WHERE username='legacy'");
                sql.executeUpdate("INSERT INTO room_type(name,bed_count,price_main,price_summer,amenities) VALUES ('Legacy type',2,0,0,repeat('A',450))");
                sql.executeUpdate("INSERT INTO building(management_unit_id,code,name,deposit_amount) SELECT id,'LEGACY','Legacy building',1000 FROM management_unit WHERE code='K1'");
                sql.executeUpdate("INSERT INTO room(building_id,room_type_id,floor,code,gender) SELECT b.id,t.id,0,'G01','M' FROM building b CROSS JOIN room_type t WHERE b.code='LEGACY' AND t.name='Legacy type'");
                sql.executeUpdate("INSERT INTO semester(academic_year,type,name,start_date,end_date) VALUES ('2026-2027','HK1','Legacy term','2026-09-01','2027-01-01')");
            }
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load().migrate();
            try (Connection connection = dataSource.getConnection(); Statement sql = connection.createStatement()) {
                connection.setSchema(schema);
                try (ResultSet row = sql.executeQuery("SELECT f.name,c.name,h.name FROM student_profile s JOIN faculty f ON f.id=s.faculty_id JOIN student_cohort c ON c.id=s.cohort_id JOIN hometown h ON h.id=s.hometown_id WHERE s.student_code='legacy'")) {
                    assertThat(row.next()).isTrue();
                    assertThat(row.getString(1)).isEqualTo(" Faculty preserved ");
                    assertThat(row.getString(2)).isEqualTo("K65");
                    assertThat(row.getString(3)).isEqualTo(" Hometown preserved ");
                }
                try (ResultSet row = sql.executeQuery("SELECT length(a.name) FROM room_type_amenity r JOIN amenity a ON a.id=r.amenity_id")) {
                    assertThat(row.next()).isTrue();
                    assertThat(row.getInt(1)).isEqualTo(450);
                }
                try (ResultSet row = sql.executeQuery("SELECT count(*) FROM room r JOIN building_floor f ON f.building_id=r.building_id AND f.floor=r.floor WHERE r.floor=0")) {
                    assertThat(row.next()).isTrue();
                    assertThat(row.getInt(1)).isEqualTo(1);
                }
            }
        } finally {
            try (Connection connection = dataSource.getConnection(); Statement sql = connection.createStatement()) {
                sql.executeUpdate("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }
}
