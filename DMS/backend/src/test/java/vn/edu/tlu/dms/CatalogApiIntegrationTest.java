package vn.edu.tlu.dms;

import java.util.Objects;
import org.springframework.lang.NonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tlu.dms.account.service.IdentityAccountService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CatalogApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean IdentityAccountService identities;
    private String suffix;
    private String adminSubject;
    private String studentSubject;
    private Long studentId;

    @BeforeEach
    void fixture() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        adminSubject = UUID.randomUUID().toString();
        studentSubject = UUID.randomUUID().toString();
        user(Role.ADMIN, adminSubject);
        studentId = user(Role.STUDENT, studentSubject);
        jdbc.update("INSERT INTO student_profile(user_id,student_code,gender) VALUES (?,?,'M')", studentId, suffix);
    }

    @Test
    void catalogCrudUsesOneSaveEndpointAndPreservesOmittedFields() throws Exception {
        Long id = create("schools", "school-" + suffix, "School " + suffix, Map.of());
        mvc.perform(post("/api/catalogs/schools/save-or-update").with(admin())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(mapper.writeValueAsBytes(Map.of("id", id, "name", "Renamed " + suffix)))))
                .andExpect(status().isOk()).andExpect(jsonPath("code").value("school-" + suffix))
                .andExpect(jsonPath("active").value(true));
        mvc.perform(get("/api/catalogs/schools/{id}", id).with(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("name").value("Renamed " + suffix));
        mvc.perform(delete("/api/catalogs/schools/{id}", id).with(admin())).andExpect(status().isOk());
        mvc.perform(get("/api/catalogs/schools/{id}", id).with(admin())).andExpect(status().isNotFound());
    }

    @Test
    void studentCanLookupButCannotWriteCatalogs() throws Exception {
        create("schools", "read-" + suffix, "Read " + suffix, Map.of());
        mvc.perform(post("/api/catalogs/schools/search-by-page").with(student())
                        .contentType(MediaType.APPLICATION_JSON_VALUE).content("{\"page\":0,\"size\":10}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/catalogs/schools/save-or-update").with(student())
                        .contentType(MediaType.APPLICATION_JSON_VALUE).content("{\"code\":\"forbidden\",\"name\":\"Forbidden\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void catalogFiltersApplyBeforePagingAndCountIncludingInactiveHistory() throws Exception {
        create("schools", "on-" + suffix, suffix + " active", Map.of());
        create("schools", "off-" + suffix, suffix + " inactive", Map.of("active", false));
        mvc.perform(post("/api/catalogs/schools/search-by-page").with(admin())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(mapper.writeValueAsBytes(Map.of("q", suffix, "active", false, "page", 0, "size", 1)))))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1))
                .andExpect(jsonPath("content.length()").value(1)).andExpect(jsonPath("content[0].active").value(false));
        mvc.perform(post("/api/catalogs/schools/search-by-page").with(admin())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(mapper.writeValueAsBytes(Map.of("q", suffix, "page", 0, "size", 1)))))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(2));
    }

    @Test
    void duplicateNamesAreScopedToTheirSchoolOrParent() throws Exception {
        Long schoolA = create("schools", "a-" + suffix, "A " + suffix, Map.of());
        Long schoolB = create("schools", "b-" + suffix, "B " + suffix, Map.of());
        String name = "Faculty " + suffix;
        create("faculties", "fa-" + suffix, name, Map.of("schoolId", schoolA));
        create("faculties", "fb-" + suffix, name, Map.of("schoolId", schoolB));
        mvc.perform(post("/api/catalogs/faculties/search-by-page").with(admin())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(mapper.writeValueAsBytes(Map.of("q", suffix, "schoolId", schoolA)))))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1))
                .andExpect(jsonPath("content[0].schoolName").value("A " + suffix));
        Long parentA = create("hometowns", "pa-" + suffix, "Province A " + suffix, Map.of());
        Long parentB = create("hometowns", "pb-" + suffix, "Province B " + suffix, Map.of());
        create("hometowns", "wa-" + suffix, "Ward " + suffix, Map.of("parentId", parentA));
        create("hometowns", "wb-" + suffix, "Ward " + suffix, Map.of("parentId", parentB));
    }

    @Test
    void usedCatalogCannotBeDeletedOrReassignedToAnotherSchool() throws Exception {
        Long schoolA = create("schools", "sa-" + suffix, "School A " + suffix, Map.of());
        Long schoolB = create("schools", "sb-" + suffix, "School B " + suffix, Map.of());
        Long faculty = create("faculties", "f-" + suffix, "Faculty " + suffix, Map.of("schoolId", schoolA));
        jdbc.update("UPDATE student_profile SET school_id=?,faculty_id=? WHERE user_id=?", schoolA, faculty, studentId);
        mvc.perform(post("/api/catalogs/faculties/save-or-update").with(admin())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(mapper.writeValueAsBytes(Map.of("id", faculty, "schoolId", schoolB)))))
                .andExpect(status().isConflict()).andExpect(jsonPath("errorCode").value(ErrorCode.RESOURCE_IN_USE));
        mvc.perform(delete("/api/catalogs/faculties/{id}", faculty).with(admin()))
                .andExpect(status().isConflict()).andExpect(jsonPath("errorCode").value(ErrorCode.RESOURCE_IN_USE));
    }

    @Test
    void hierarchyRejectsCyclesAndInactiveParentSelections() throws Exception {
        Long parent = create("hometowns", "p-" + suffix, "Parent " + suffix, Map.of());
        Long child = create("hometowns", "c-" + suffix, "Child " + suffix, Map.of("parentId", parent));
        mvc.perform(post("/api/catalogs/hometowns/save-or-update").with(admin())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(mapper.writeValueAsBytes(Map.of("id", parent, "parentId", child)))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/catalogs/hometowns/save-or-update").with(admin())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(mapper.writeValueAsBytes(Map.of("id", parent, "active", false)))))
                .andExpect(status().isConflict());
    }

    @Test
    void immutableCodesMissingIdsAndUnknownKindsAreRejected() throws Exception {
        Long id = create("schools", "keep-" + suffix, "Keep " + suffix, Map.of());
        mvc.perform(post("/api/catalogs/schools/save-or-update").with(admin())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(mapper.writeValueAsBytes(Map.of("id", id, "code", "changed")))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/catalogs/schools/save-or-update").with(admin())
                        .contentType(MediaType.APPLICATION_JSON_VALUE).content("{\"id\":9223372036854775807,\"name\":\"Missing\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/catalogs/not-a-catalog/search-by-page").with(admin())
                        .contentType(MediaType.APPLICATION_JSON_VALUE).content("{}"))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT code FROM school WHERE id=?", String.class, id)).isEqualTo("keep-" + suffix);
    }

    @Test
    void creatingFloorWithoutBuildingOrFloorNumberReturnsValidationError() throws Exception {
        for (String body : List.of(
                "{\"floor\":0,\"name\":\"Ground floor\"}",
                "{\"buildingId\":1,\"name\":\"Ground floor\"}")) {
            mvc.perform(post("/api/floors/save-or-update").with(admin())
                            .contentType(MediaType.APPLICATION_JSON_VALUE).content(Objects.requireNonNull(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("errorCode").value(ErrorCode.VALIDATION_FAILED));
        }
    }

    @Test
    void floorZeroIsSelectableAndUsedFloorsCannotBeDeleted() throws Exception {
        Long unit = jdbc.queryForObject("SELECT id FROM management_unit WHERE code='K1'", Long.class);
        Long building = jdbc.queryForObject("INSERT INTO building(management_unit_id,code,name,deposit_amount) VALUES (?,?,'Test building',1000) RETURNING id", Long.class, unit, suffix);
        String response = mvc.perform(post("/api/floors/save-or-update").with(admin())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(mapper.writeValueAsBytes(Map.of("buildingId", building, "floor", 0, "name", "Ground floor")))))
                .andExpect(status().isOk()).andExpect(jsonPath("floor").value(0))
                .andReturn().getResponse().getContentAsString();
        Long floor = mapper.readTree(response).get("id").longValue();
        mvc.perform(post("/api/floors/search-by-page").with(admin()).contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(mapper.writeValueAsBytes(Map.of("buildingId", building, "active", true)))))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1));
        Long type = jdbc.queryForObject("INSERT INTO room_type(name,bed_count,price_main,price_summer) VALUES (?,1,0,0) RETURNING id", Long.class, suffix);
        mvc.perform(post("/api/rooms/save-or-update").with(admin()).contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(mapper.writeValueAsBytes(Map.of("buildingId", building, "floor", 0, "code", "G01", "roomTypeId", type, "gender", "M")))))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/floors/{id}", floor).with(admin())).andExpect(status().isConflict());
    }

    @Test
    void newAssignmentsRejectInactiveOrUnconfiguredFloors() throws Exception {
        Long unit = jdbc.queryForObject("SELECT id FROM management_unit WHERE code='K1'", Long.class);
        Long building = jdbc.queryForObject("INSERT INTO building(management_unit_id,code,name,deposit_amount) VALUES (?,?,'Test building',1000) RETURNING id", Long.class, unit, suffix);
        jdbc.update("INSERT INTO building_floor(building_id,floor,name,active) VALUES (?,1,'Closed floor',false)", building);
        Long type = jdbc.queryForObject("INSERT INTO room_type(name,bed_count,price_main,price_summer) VALUES (?,1,0,0) RETURNING id", Long.class, suffix);
        for (int floor : List.of(1, 2)) {
            mvc.perform(post("/api/rooms/save-or-update").with(admin()).contentType(MediaType.APPLICATION_JSON_VALUE)
                            .content(Objects.requireNonNull(mapper.writeValueAsBytes(Map.of("buildingId", building, "floor", floor, "code", "P" + floor, "roomTypeId", type, "gender", "F")))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("details.fieldErrors[0].field").value("floor"));
        }
    }

    private Long create(String kind, String code, String name, Map<String, Object> extra) throws Exception {
        var payload = new java.util.LinkedHashMap<String, Object>(extra);
        payload.put("code", code);
        payload.put("name", name);
        String response = mvc.perform(post("/api/catalogs/{kind}/save-or-update", kind).with(admin())
                        .contentType(MediaType.APPLICATION_JSON_VALUE).content(Objects.requireNonNull(mapper.writeValueAsBytes(payload))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("id").longValue();
    }

    private Long user(String role, String subject) {
        return jdbc.queryForObject("INSERT INTO app_user(username,role,full_name,must_change_password,keycloak_user_id) VALUES (?,?,?,false,?) RETURNING id",
                Long.class, role + suffix, role, role, subject);
    }

    @NonNull

    private RequestPostProcessor admin() { return identity(adminSubject, Role.ADMIN); }
    @NonNull
    private RequestPostProcessor student() { return identity(studentSubject, Role.STUDENT); }
    @NonNull
    private RequestPostProcessor identity(String subject, String role) {
        return Objects.requireNonNull(jwt().jwt(builder -> builder.subject(subject)
                .claim("realm_access", Map.of("roles", List.of(role))).claim("dms_security_version", 0L)));
    }
}
