package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.domain.AppUser;
import vn.edu.tlu.dms.account.service.IdentityAccountService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Kiểm thử API trên PostgreSQL thật: mapping, ràng buộc, phạm vi và luồng trạng thái. */
@SpringBootTest(properties = {"dms.upload-dir=${java.io.tmpdir}/dms-upload-integration"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CrudApiIntegrationTest {

    @Autowired MockMvc mvc;

    @Autowired JdbcTemplate jdbc;

    @Autowired ObjectMapper mapper;

    @Autowired EntityManager entityManager;

    @MockitoBean JwtDecoder decoder;

    @MockitoBean IdentityAccountService identities;

    private Long adminId;

    private Long staffId;

    private Long otherStaffId;

    private Long studentId;

    private Long technicianId;

    private Long firstUnitId;

    private Long otherUnitId;

    private Long firstBuildingId;

    private Long otherBuildingId;

    private Long roomTypeId;

    private Long firstRoomId;

    private Long otherRoomId;

    private String suffix;

    @BeforeEach
    void createFixtureData() {
        doAnswer(
                        invocation -> {
                            AppUser user = invocation.getArgument(0);
                            user.setKeycloakUserId(UUID.randomUUID().toString());
                            user.setPasswordHash(null);
                            return null;
                        })
                .when(identities)
                .provisionAccount(any(AppUser.class), anyString());
        suffix = UUID.randomUUID().toString().substring(0, 6);
        firstUnitId =
                jdbc.queryForObject("SELECT id FROM management_unit WHERE code = 'K1'", Long.class);
        otherUnitId =
                jdbc.queryForObject(
                        "SELECT id FROM management_unit WHERE code = 'N23'", Long.class);
        adminId = createUser("ADMIN", null);
        staffId = createUser("STAFF", firstUnitId);
        otherStaffId = createUser("STAFF", otherUnitId);
        studentId = createUser("STUDENT", null);
        technicianId = createUser("TECHNICIAN", firstUnitId);
        jdbc.update(
                "INSERT INTO student_profile(user_id,student_code,gender) VALUES (?,?,'M')",
                studentId,
                "SV" + suffix);
        firstBuildingId = createBuilding(firstUnitId, "A" + suffix);
        otherBuildingId = createBuilding(otherUnitId, "B" + suffix);
        roomTypeId =
                jdbc.queryForObject(
                        """
                        INSERT INTO room_type(name,bed_count,price_main,price_summer) VALUES (?,2,1000000,500000) RETURNING id
                        """,
                        Long.class,
                        "Loại " + suffix);
        firstRoomId = createRoom(firstBuildingId, "A101");
        otherRoomId = createRoom(otherBuildingId, "B101");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void photoFailureRollsBackDatabaseRowsAndPreviouslyWrittenFiles() throws Exception {
        try {
            MockMultipartFile data =
                    new MockMultipartFile(
                            "data",
                            "data.json",
                            "application/json",
                            ("""
                            {"category":"ELECTRIC","severity":"LOW","description":"Ảnh kiểm thử","buildingId":%d,"floor":1}
                            """
                                            .formatted(firstBuildingId))
                                    .getBytes(StandardCharsets.UTF_8));
            MockMultipartFile validPhoto =
                    new MockMultipartFile(
                            "photos",
                            "valid.png",
                            "image/png",
                            new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
            MockMultipartFile invalidPhoto =
                    new MockMultipartFile(
                            "photos",
                            "invalid.png",
                            "image/png",
                            "invalid content".getBytes(StandardCharsets.UTF_8));
            mvc.perform(
                            multipart("/api/maintenance-requests")
                                    .file(data)
                                    .file(validPhoto)
                                    .file(invalidPhoto)
                                    .with(asUser(staffId, "STAFF")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("PHOTO_INVALID"));
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT count(*) FROM maintenance_request WHERE requester_id ="
                                            + " ?",
                                    Long.class,
                                    staffId))
                    .isZero();
            var uploadDirectory =
                    Path.of(System.getProperty("java.io.tmpdir"), "dms-upload-integration");
            if (Files.exists(uploadDirectory)) {
                try (var paths = Files.list(uploadDirectory)) {
                    assertThat(paths.count()).isZero();
                }
            }
        } finally {
            // Case này cần commit thật để kiểm tra rollback của Service, nên dọn đúng các ID
            // fixture đã tạo.
            jdbc.update("DELETE FROM room WHERE id IN (?, ?)", firstRoomId, otherRoomId);
            jdbc.update(
                    "DELETE FROM building WHERE id IN (?, ?)", firstBuildingId, otherBuildingId);
            jdbc.update("DELETE FROM room_type WHERE id = ?", roomTypeId);
            jdbc.update("DELETE FROM student_profile WHERE user_id = ?", studentId);
            jdbc.update(
                    "DELETE FROM app_user WHERE id IN (?, ?, ?, ?, ?)",
                    adminId,
                    staffId,
                    otherStaffId,
                    studentId,
                    technicianId);
        }
    }

    @Test
    void validatesPagingAndCommaSeparatedSortParameters() throws Exception {
        mvc.perform(get("/api/rooms").param("sort", "code,desc").with(asUser(staffId, "STAFF")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/rooms").param("size", "101").with(asUser(staffId, "STAFF")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        mvc.perform(
                        get("/api/rooms")
                                .param("sort", "passwordHash,desc")
                                .with(asUser(staffId, "STAFF")))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/rooms/" + firstRoomId + "/status")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"MAINTENANCE\",\"reason\":\"x    \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void roomCrudCreatesBedsAndSupportsPartialUpdates() throws Exception {
        String response =
                mvc.perform(
                                post("/api/rooms")
                                        .with(asUser(staffId, "STAFF"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"buildingId":%d,"roomTypeId":%d,"floor":2,"code":"A102","gender":"M"}
                                                """
                                                        .formatted(firstBuildingId, roomTypeId)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.beds.length()").value(2))
                        .andExpect(jsonPath("$.freeBeds").value(2))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long id = mapper.readTree(response).get("id").asLong();
        mvc.perform(
                        patch("/api/rooms/" + id)
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"A103\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A103"));
        mvc.perform(delete("/api/rooms/" + id).with(asUser(staffId, "STAFF")))
                .andExpect(status().isNoContent());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM bed WHERE room_id = ?", Long.class, id))
                .isZero();
    }

    @Test
    void listsAndDetailsRespectUnitScopeAndAggregatePermissionDoesNotAllowWrites()
            throws Exception {
        mvc.perform(get("/api/rooms").with(asUser(staffId, "STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/rooms/" + otherRoomId).with(asUser(staffId, "STAFF")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
        jdbc.update("UPDATE app_user SET cross_unit_access = true WHERE id = ?", staffId);
        entityManager.clear();
        mvc.perform(get("/api/rooms").with(asUser(staffId, "STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(
                        patch("/api/rooms/" + otherRoomId)
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"INVALID\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAdminAccessAndUntrustedStatusFields() throws Exception {
        mvc.perform(get("/api/admin/users").with(asUser(staffId, "STAFF")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/rooms")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"buildingId":%d,"roomTypeId":%d,"floor":1,"code":"BAD","gender":"M","status":"CLOSED"}
                                        """
                                                .formatted(firstBuildingId, roomTypeId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void schemaConstraintsMapToStableApiErrors() throws Exception {
        mvc.perform(
                        post("/api/rooms")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"buildingId":%d,"roomTypeId":%d,"floor":1,"code":"A101","gender":"M"}
                                        """
                                                .formatted(firstBuildingId, roomTypeId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ROOM_CODE_EXISTS"));
    }

    @Test
    void catalogLifecycleUsesAdminPermissionsAndPatchKeepsUnsentFields() throws Exception {
        String created =
                mvc.perform(
                                post("/api/admin/management-units")
                                        .with(asUser(adminId, "ADMIN"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"code\":\"U"
                                                        + suffix
                                                        + "\",\"name\":\"Đơn vị mới\"}"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long id = mapper.readTree(created).get("id").asLong();
        mvc.perform(
                        patch("/api/admin/management-units/" + id)
                                .with(asUser(adminId, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Đơn vị cập nhật\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("U" + suffix));
        mvc.perform(delete("/api/admin/management-units/" + id).with(asUser(adminId, "ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void semesterOverlapIsRejectedByPostgresConstraint() throws Exception {
        jdbc.update(
                """
                INSERT INTO semester(academic_year,type,name,start_date,end_date)
                VALUES ('2040-2041','HK1','HK1','2040-09-01','2041-01-31')
                """);
        mvc.perform(
                        post("/api/semesters")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"academicYear":"2040-2041","type":"HK2","name":"Trùng","startDate":"2041-01-01","endDate":"2041-06-01"}
                                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SEMESTER_OVERLAP"));
    }

    @Test
    void applicationWorkflowRecordsHistoryAndRejectsRepeatedActions() throws Exception {
        Long periodId = createOpenPeriod();
        Long id = createDraftApplication(periodId);
        mvc.perform(post("/api/applications/" + id + "/submit").with(asUser(studentId, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
        mvc.perform(
                        post("/api/applications/" + id + "/approve")
                                .with(asUser(otherStaffId, "STAFF")))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/applications/" + id + "/approve").with(asUser(staffId, "STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.timeline.length()").value(3));
        mvc.perform(post("/api/applications/" + id + "/approve").with(asUser(staffId, "STAFF")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void registrationPeriodUsesCompositeMappingsAndRejectsCrossUnitWrite() throws Exception {
        Long semesterId = createSemester();
        mvc.perform(
                        post("/api/registration-periods")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"semesterId":%d,"kind":"REGULAR","name":"Đợt","openAt":"2040-08-01T08:00:00+07:00",
                                        "closeAt":"2040-08-31T08:00:00+07:00","unitIds":[%d],"quotas":[{"buildingId":%d,"quota":10}]}
                                        """
                                                .formatted(
                                                        semesterId, firstUnitId, firstBuildingId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quotas[0].quota").value(10));
        mvc.perform(
                        post("/api/registration-periods")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"semesterId":%d,"kind":"REGULAR","name":"Sai đơn vị","openAt":"2040-08-01T08:00:00+07:00",
                                        "closeAt":"2040-08-31T08:00:00+07:00","unitIds":[%d],"quotas":[]}
                                        """
                                                .formatted(semesterId, otherUnitId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void maintenanceWorkflowChecksAssignedTechnicianAndRecordsProgress() throws Exception {
        MockMultipartFile data =
                new MockMultipartFile(
                        "data",
                        "data.json",
                        "application/json",
                        """
                        {"category":"ELECTRIC","severity":"MEDIUM","description":"Đèn hỏng","buildingId":%d,"floor":1}
                        """
                                .formatted(firstBuildingId)
                                .getBytes(StandardCharsets.UTF_8));
        String created =
                mvc.perform(
                                multipart("/api/maintenance-requests")
                                        .file(data)
                                        .with(asUser(staffId, "STAFF")))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long id = mapper.readTree(created).get("id").asLong();
        mvc.perform(get("/api/maintenance-requests").with(asUser(staffId, "STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(
                        post("/api/maintenance-requests/" + id + "/assign")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"technicianId\":" + technicianId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
        mvc.perform(
                        post("/api/maintenance-requests/" + id + "/start")
                                .with(asUser(technicianId, "TECHNICIAN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mvc.perform(
                        post("/api/maintenance-requests/" + id + "/resolve")
                                .with(asUser(technicianId, "TECHNICIAN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"note\":\"Đã thay bóng đèn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
        mvc.perform(
                        post("/api/maintenance-requests/" + id + "/accept")
                                .with(asUser(staffId, "STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void notificationReadsAreIdempotentAndStudentsCannotPublish() throws Exception {
        Long id =
                jdbc.queryForObject(
                        """
                        INSERT INTO notification(scope,user_id,title,body,created_by) VALUES ('USER',?,'Thông báo','Nội dung',?) RETURNING id
                        """,
                        Long.class,
                        studentId,
                        staffId);
        mvc.perform(get("/api/notifications/unread-count").with(asUser(studentId, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
        for (int repeat = 0; repeat < 2; repeat++) {
            mvc.perform(
                            post("/api/notifications/" + id + "/read")
                                    .with(asUser(studentId, "STUDENT")))
                    .andExpect(status().isNoContent());
        }
        mvc.perform(get("/api/notifications").with(asUser(studentId, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].isRead").value(true));
        mvc.perform(
                        post("/api/notifications")
                                .with(asUser(studentId, "STUDENT"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"scope\":\"ALL\",\"title\":\"Hi\",\"body\":\"Nội"
                                                + " dung\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void contractAndInvoiceQueriesRespectOwnershipAndPaymentCannotBeRepeated() throws Exception {
        Long periodId = createOpenPeriod();
        Long applicationId = createDraftApplication(periodId);
        Long bedId =
                jdbc.queryForObject(
                        "INSERT INTO bed(room_id,code,status)VALUES(?,'01','ACTIVE')RETURNING id",
                        Long.class,
                        firstRoomId);
        Long contractId =
                jdbc.queryForObject(
                        """
                        INSERT INTO dormitory_contract(code,student_id,application_id,start_date,end_date,status,activation_deadline)
                        VALUES(?,?,?,'2040-09-01','2041-01-31','PENDING',now()+interval '7 days')RETURNING id
                        """,
                        Long.class,
                        "HD-" + suffix,
                        studentId,
                        applicationId);
        jdbc.update(
                """
                INSERT INTO room_assignment_history(student_id,contract_id,bed_id,kind,price_main,price_summer,created_by)
                VALUES(?,?,?,'HOLD',1000000,500000,?)
                """,
                studentId,
                contractId,
                bedId,
                staffId);
        mvc.perform(get("/api/contracts").with(asUser(staffId, "STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/contracts/" + contractId).with(asUser(otherStaffId, "STAFF")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/rooms/" + firstRoomId).with(asUser(staffId, "STAFF")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ROOM_OCCUPIED"));
        Long invoiceId =
                jdbc.queryForObject(
                        """
                        INSERT INTO invoice(code,type,student_id,semester_id,total_amount,due_date,issued_by)
                        VALUES(?,'ROOM_FEE',?,?,1000000,current_date+7,?)RETURNING id
                        """,
                        Long.class,
                        "HP-" + suffix,
                        studentId,
                        jdbc.queryForObject(
                                "SELECT semester_id FROM registration_period WHERE id=?",
                                Long.class,
                                periodId),
                        staffId);
        mvc.perform(get("/api/invoices").with(asUser(staffId, "STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/me/invoices").with(asUser(studentId, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        for (int attempt = 1; attempt <= 2; attempt++) {
            var result =
                    mvc.perform(
                            post("/api/invoices/" + invoiceId + "/confirm-payment")
                                    .with(asUser(staffId, "STAFF"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"payerUserId\":"
                                                    + studentId
                                                    + ",\"paymentMethod\":\"CASH\"}"));
            if (attempt == 1) {
                result.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
            } else {
                result.andExpect(status().isConflict())
                        .andExpect(jsonPath("$.errorCode").value("INVOICE_ALREADY_PAID"));
            }
        }
    }

    @Test
    void pendingContractCrudIsScopedAndDoesNotCreateDetailedWorkflowData() throws Exception {
        Long periodId = createOpenPeriod();
        Long applicationId = createDraftApplication(periodId);
        jdbc.update("UPDATE room_application SET status='APPROVED' WHERE id=?", applicationId);
        entityManager.clear();
        String result =
                mvc.perform(
                                post("/api/contracts")
                                        .with(asUser(staffId, "STAFF"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                mapper.writeValueAsString(
                                                        Map.of(
                                                                "applicationId",
                                                                applicationId,
                                                                "startDate",
                                                                "2040-09-01",
                                                                "endDate",
                                                                "2041-01-31"))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("PENDING"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long id = mapper.readTree(result).get("id").asLong();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM room_assignment_history WHERE contract_id=?",
                                Long.class,
                                id))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM deposit_transaction WHERE contract_id=?",
                                Long.class,
                                id))
                .isZero();
        mvc.perform(get("/api/contracts").param("q", "HD-").with(asUser(staffId, "STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(
                        patch("/api/contracts/" + id)
                                .with(asUser(otherStaffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"endDate\":\"2041-02-01\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(
                        patch("/api/contracts/" + id)
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"endDate\":\"2041-02-01\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/contracts/" + id).with(asUser(staffId, "STAFF")))
                .andExpect(status().isNoContent());
    }

    @Test
    void studentCrudReturnsTemporaryPasswordAndBlocksDeleteWhenHistoryExists() throws Exception {
        String json =
                mapper.writeValueAsString(
                        Map.of(
                                "studentCode",
                                "NEW" + suffix,
                                "fullName",
                                "Sinh viên CRUD",
                                "gender",
                                "F",
                                "faculty",
                                "CNTT"));
        String result =
                mvc.perform(
                                post("/api/admin/students")
                                        .with(asUser(adminId, "ADMIN"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.temporaryPassword").isString())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long id = mapper.readTree(result).get("userId").asLong();
        mvc.perform(
                        get("/api/admin/students")
                                .param("q", "NEW" + suffix)
                                .with(asUser(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(delete("/api/admin/students/" + id).with(asUser(adminId, "ADMIN")))
                .andExpect(status().isNoContent());
        createDraftApplication(createOpenPeriod());
        mvc.perform(delete("/api/admin/students/" + studentId).with(asUser(adminId, "ADMIN")))
                .andExpect(status().isConflict());
    }

    @Test
    void readingCrudCalculatesConsumptionAndInvoiceRecordCrudRejectsForeignScope()
            throws Exception {
        String reading =
                mapper.writeValueAsString(
                        Map.of(
                                "roomId",
                                firstRoomId,
                                "meterType",
                                "ELECTRIC",
                                "billingMonth",
                                "2026-10-01",
                                "startReading",
                                100,
                                "endReading",
                                120));
        String created =
                mvc.perform(
                                post("/api/utility-readings")
                                        .with(asUser(staffId, "STAFF"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(reading))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.consumption").value(20))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long readingId = mapper.readTree(created).get("id").asLong();
        mvc.perform(
                        delete("/api/utility-readings/" + readingId)
                                .with(asUser(otherStaffId, "STAFF")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/utility-readings/" + readingId).with(asUser(staffId, "STAFF")))
                .andExpect(status().isNoContent());
        String invoice =
                mapper.writeValueAsString(
                        Map.of(
                                "type",
                                "UTILITY",
                                "roomId",
                                firstRoomId,
                                "billingMonth",
                                "2026-10-01",
                                "totalAmount",
                                20000,
                                "dueDate",
                                LocalDate.now().plusDays(7).toString()));
        String result =
                mvc.perform(
                                post("/api/invoices")
                                        .with(asUser(staffId, "STAFF"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(invoice))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long invoiceId = mapper.readTree(result).get("id").asLong();
        mvc.perform(
                        patch("/api/invoices/" + invoiceId)
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"totalAmount\":30000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(30000));
        mvc.perform(get("/api/invoices").param("q", "DN-").with(asUser(staffId, "STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(delete("/api/invoices/" + invoiceId).with(asUser(staffId, "STAFF")))
                .andExpect(status().isNoContent());
    }

    private RequestPostProcessor asUser(Long id, String role) {
        String identity =
                jdbc.queryForObject(
                        "SELECT keycloak_user_id FROM app_user WHERE id=?", String.class, id);
        Long version =
                jdbc.queryForObject(
                        "SELECT security_version FROM app_user WHERE id=?", Long.class, id);
        return jwt().jwt(
                        builder ->
                                builder.subject(identity)
                                        .claim("realm_access", Map.of("roles", List.of(role)))
                                        .claim("dms_security_version", version))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private Long createUser(String role, Long unitId) {
        return jdbc.queryForObject(
                "INSERT INTO"
                    + " app_user(username,role,full_name,management_unit_id,must_change_password,keycloak_user_id)"
                    + " VALUES(?,?,?,?,false,?) RETURNING id",
                Long.class,
                role + "-" + unitId + "-" + suffix,
                role,
                "Người dùng " + role,
                unitId,
                UUID.randomUUID().toString());
    }

    private Long createBuilding(Long unitId, String code) {
        return jdbc.queryForObject(
                "INSERT INTO"
                    + " building(management_unit_id,code,name,deposit_amount)VALUES(?,?,?,1000000)RETURNING"
                    + " id",
                Long.class,
                unitId,
                code,
                "Tòa " + code);
    }

    private Long createRoom(Long buildingId, String code) {
        return jdbc.queryForObject(
                "INSERT INTO"
                    + " room(building_id,room_type_id,floor,code,gender)VALUES(?,?,1,?,'M')RETURNING"
                    + " id",
                Long.class,
                buildingId,
                roomTypeId,
                code);
    }

    private Long createSemester() {
        return jdbc.queryForObject(
                """
                INSERT INTO semester(academic_year,type,name,start_date,end_date)
                VALUES('2040-2041','HK1','HK1','2040-09-01','2041-01-31')RETURNING id
                """,
                Long.class);
    }

    private Long createOpenPeriod() {
        Long semesterId = createSemester();
        Long periodId =
                jdbc.queryForObject(
                        """
                        INSERT INTO registration_period(semester_id,kind,name,open_at,close_at,created_by)
                        VALUES(?,'REGULAR','Đợt kiểm thử',now()-interval '1 day',now()+interval '7 days',?)RETURNING id
                        """,
                        Long.class,
                        semesterId,
                        staffId);
        jdbc.update(
                "INSERT INTO registration_period_unit(period_id,management_unit_id)VALUES(?,?)",
                periodId,
                firstUnitId);
        return periodId;
    }

    private Long createDraftApplication(Long periodId) throws Exception {
        String response =
                mvc.perform(
                                post("/api/applications")
                                        .with(asUser(studentId, "STUDENT"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"periodId\":"
                                                        + periodId
                                                        + ",\"preferredUnitId\":"
                                                        + firstUnitId
                                                        + "}"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return mapper.readTree(response).get("id").asLong();
    }
}
