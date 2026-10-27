package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.domain.AppUser;
import vn.edu.tlu.dms.account.service.IdentityAccountService;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.Role;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Kiểm thử API trên PostgreSQL thật: mapping, ràng buộc, phạm vi và luồng trạng thái. */
@SpringBootTest(properties = {"dms.upload-dir=${java.io.tmpdir}/dms-upload-integration"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CrudApiIntegrationTest {

    /** Khóa bộ lọc search-by-page phải gửi đi dưới dạng số nguyên dài. */
    private static final List<String> LONG_FILTERS =
            List.of("page", "size", "managementUnitId", "actorId", "buildingId", "roomTypeId");

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
                            multipart("/api/maintenance-requests/save-or-update")
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
    void validatesPagingAndCommaSeparatedSortInSearchBody() throws Exception {
        mvc.perform(searchByPage("/api/rooms", "sort", "code,desc").with(asUser(staffId, "STAFF")))
                .andExpect(status().isOk());
        mvc.perform(searchByPage("/api/rooms", "size", "101").with(asUser(staffId, "STAFF")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        mvc.perform(
                        searchByPage("/api/rooms", "sort", "passwordHash,desc")
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
                                post("/api/rooms/save-or-update")
                                        .with(asUser(staffId, "STAFF"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"buildingId":%d,"roomTypeId":%d,"floor":2,"code":"A102","gender":"M"}
                                                """
                                                        .formatted(firstBuildingId, roomTypeId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.beds.length()").value(2))
                        .andExpect(jsonPath("$.freeBeds").value(2))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long id = mapper.readTree(response).get("id").asLong();
        mvc.perform(
                        post("/api/rooms/save-or-update")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(id, "{\"code\":\"A103\"}")))
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
        // Lọc theo roomTypeId của fixture (duy nhất mỗi lần chạy) để phạm vi đơn vị là yếu tố duy
        // nhất quyết định số bản ghi, kể cả khi CSDL local đã có phòng mẫu trong cùng đơn vị.
        mvc.perform(
                        searchByPage("/api/rooms", "roomTypeId", roomTypeId)
                                .with(asUser(staffId, "STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/rooms/" + otherRoomId).with(asUser(staffId, "STAFF")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
        jdbc.update("UPDATE app_user SET cross_unit_access = true WHERE id = ?", staffId);
        entityManager.clear();
        mvc.perform(
                        searchByPage("/api/rooms", "roomTypeId", roomTypeId)
                                .with(asUser(staffId, "STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(
                        post("/api/rooms/save-or-update")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(otherRoomId, "{\"code\":\"INVALID\"}")))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAdminAccessAndUntrustedStatusFields() throws Exception {
        mvc.perform(searchByPage("/api/admin/users").with(asUser(staffId, "STAFF")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/rooms/save-or-update")
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
                        post("/api/rooms/save-or-update")
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
    void catalogLifecycleUsesAdminPermissionsAndSaveKeepsUnsentFields() throws Exception {
        String created =
                mvc.perform(
                                post("/api/admin/management-units/save-or-update")
                                        .with(asUser(adminId, "ADMIN"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"code\":\"U"
                                                        + suffix
                                                        + "\",\"name\":\"Đơn vị mới\"}"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long id = mapper.readTree(created).get("id").asLong();
        mvc.perform(
                        post("/api/admin/management-units/save-or-update")
                                .with(asUser(adminId, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(id, "{\"name\":\"Đơn vị cập nhật\"}")))
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
                        post("/api/semesters/save-or-update")
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
                        post("/api/registration-periods/save-or-update")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"semesterId":%d,"kind":"REGULAR","name":"Đợt","openAt":"2040-08-01T08:00:00+07:00",
                                        "closeAt":"2040-08-31T08:00:00+07:00","unitIds":[%d],"quotas":[{"buildingId":%d,"quota":10}]}
                                        """
                                                .formatted(
                                                        semesterId, firstUnitId, firstBuildingId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quotas[0].quota").value(10));
        mvc.perform(
                        post("/api/registration-periods/save-or-update")
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
                                multipart("/api/maintenance-requests/save-or-update")
                                        .file(data)
                                        .with(asUser(staffId, "STAFF")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long id = mapper.readTree(created).get("id").asLong();
        // Tìm theo đúng mã ticket vừa sinh để số bản ghi không phụ thuộc ticket mẫu của đơn vị
        // trong CSDL local.
        mvc.perform(
                        searchByPage(
                                        "/api/maintenance-requests",
                                        "q",
                                        mapper.readTree(created).get("code").asText())
                                .with(asUser(staffId, "STAFF")))
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
        String title = "Thông báo " + suffix;
        Long id =
                jdbc.queryForObject(
                        """
                        INSERT INTO notification(scope,user_id,title,body,created_by) VALUES ('USER',?,?,?,?) RETURNING id
                        """,
                        Long.class,
                        studentId,
                        title,
                        "Nội dung",
                        staffId);
        // Đếm theo hiệu trước/sau vì hộp thư sinh viên còn nhận tin mẫu (phạm vi ALL, FLOOR) từ
        // CSDL local; đối tượng kiểm tra là đúng một tin chưa đọc của test bị ẩn khi đọc lặp lại.
        long unreadBefore = unreadNotifications();
        assertThat(unreadBefore).isGreaterThanOrEqualTo(1);
        for (int repeat = 0; repeat < 2; repeat++) {
            mvc.perform(
                            post("/api/notifications/" + id + "/read")
                                    .with(asUser(studentId, "STUDENT")))
                    .andExpect(status().isNoContent());
        }
        assertThat(unreadNotifications()).isEqualTo(unreadBefore - 1);
        mvc.perform(
                        searchByPage("/api/notifications", "q", title)
                                .with(asUser(studentId, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].isRead").value(true));
        mvc.perform(
                        post("/api/notifications/save-or-update")
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
        // Lọc theo mã hợp đồng/ hóa đơn của fixture để phép đếm chỉ tính dữ liệu test tạo, không
        // cộng thêm hợp đồng, hóa đơn mẫu đang có trong đơn vị ở CSDL local.
        mvc.perform(
                        searchByPage("/api/contracts", "q", "HD-" + suffix)
                                .with(asUser(staffId, "STAFF")))
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
        mvc.perform(
                        searchByPage("/api/invoices", "q", "HP-" + suffix)
                                .with(asUser(staffId, "STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(searchByPage("/api/me/invoices").with(asUser(studentId, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        // API tổng hợp của ADMIN xem toàn hệ thống; /me vẫn chỉ là dữ liệu của chính ADMIN.
        mvc.perform(
                        searchByPage("/api/contracts", "q", "HD-" + suffix)
                                .with(asUser(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(
                        searchByPage("/api/invoices", "q", "HP-" + suffix)
                                .with(asUser(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(searchByPage("/api/me/contracts").with(asUser(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(searchByPage("/api/me/invoices").with(asUser(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
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
                                post("/api/contracts/save-or-update")
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
        // Tìm đúng mã hợp đồng API vừa sinh để phép đếm không dính hợp đồng mẫu trong đơn vị.
        mvc.perform(
                        searchByPage(
                                        "/api/contracts",
                                        "q",
                                        mapper.readTree(result).get("code").asText())
                                .with(asUser(staffId, "STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(
                        post("/api/contracts/save-or-update")
                                .with(asUser(otherStaffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(id, "{\"endDate\":\"2041-02-01\"}")))
                .andExpect(status().isNotFound());
        mvc.perform(
                        post("/api/contracts/save-or-update")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        withId(
                                                id,
                                                "{\"endDate\":\"2041-02-01\",\"status\":\"ACTIVE\"}")))
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
                                post("/api/admin/students/save-or-update")
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
                        searchByPage("/api/admin/students", "q", "NEW" + suffix)
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
                                post("/api/utility-readings/save-or-update")
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
                                post("/api/invoices/save-or-update")
                                        .with(asUser(staffId, "STAFF"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(invoice))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long invoiceId = mapper.readTree(result).get("id").asLong();
        mvc.perform(
                        post("/api/invoices/save-or-update")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(invoiceId, "{\"totalAmount\":30000}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(30000));
        // Dùng chính mã hóa đơn API sinh ra (thay vì tiền tố "DN-") để không đếm kèm hóa đơn điện
        // nước mẫu của dữ liệu local.
        mvc.perform(
                        searchByPage(
                                        "/api/invoices",
                                        "q",
                                        mapper.readTree(result).get("code").asText())
                                .with(asUser(staffId, "STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(delete("/api/invoices/" + invoiceId).with(asUser(staffId, "STAFF")))
                .andExpect(status().isNoContent());
    }

    // ------------------------------------------------------------------
    // Hồi quy cho các lỗi từ code review nhánh develop-quyennnv
    // ------------------------------------------------------------------

    @Test
    void floorNotificationCanBeRetitledWithoutChangingScope() throws Exception {
        String body =
                """
                {"scope":"FLOOR","buildingId":%d,"floor":1,"title":"%s","body":"Nội dung"}
                """;
        String created =
                mvc.perform(
                                post("/api/notifications/save-or-update")
                                        .with(asUser(staffId, "STAFF"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body.formatted(firstBuildingId, "Cũ")))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long id = mapper.readTree(created).get("id").asLong();
        mvc.perform(
                        post("/api/notifications/save-or-update")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(id, body.formatted(firstBuildingId, "Mới"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Mới"));
    }

    @Test
    void maintenancePatchReturnsTheUpdatedTicket() throws Exception {
        Long ticketId =
                jdbc.queryForObject(
                        """
                        INSERT INTO maintenance_request(code,requester_id,building_id,floor,category,severity,description)
                        VALUES (?,?,?,1,'ELECTRIC','LOW','Đèn hỏng') RETURNING id
                        """,
                        Long.class,
                        "TK-" + suffix,
                        studentId,
                        firstBuildingId);
        mvc.perform(
                        multipart("/api/maintenance-requests/save-or-update")
                                .file(
                                        new MockMultipartFile(
                                                "data",
                                                "data.json",
                                                MediaType.APPLICATION_JSON_VALUE,
                                                withId(
                                                                ticketId,
                                                                """
                                                                {"category":"WATER","severity":"HIGH","description":"Vòi nước hỏng"}
                                                                """)
                                                        .getBytes(StandardCharsets.UTF_8)))
                                .with(asUser(studentId, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.category").value("WATER"));
    }

    @Test
    void maintenanceEditHidesForeignTicketsAndRejectsNonOpenOnes() throws Exception {
        Long otherStudentId =
                jdbc.queryForObject(
                        "INSERT INTO app_user(username,role,full_name,must_change_password,"
                                + "keycloak_user_id) VALUES(?,'STUDENT','Sinh viên khác',false,?)"
                                + " RETURNING id",
                        Long.class,
                        "other" + suffix,
                        UUID.randomUUID().toString());
        Long ticketId =
                jdbc.queryForObject(
                        """
                        INSERT INTO maintenance_request(code,requester_id,building_id,floor,category,severity,description)
                        VALUES (?,?,?,1,'ELECTRIC','LOW','Đèn hỏng') RETURNING id
                        """,
                        Long.class,
                        "TK-" + suffix,
                        studentId,
                        firstBuildingId);
        String body =
                """
                {"category":"WATER","severity":"HIGH","description":"Vòi nước hỏng"}
                """;
        mvc.perform(
                        ticketEdit(ticketId, body)
                                .with(asUser(otherStudentId, "STUDENT")))
                .andExpect(status().isNotFound());
        mvc.perform(
                        ticketEdit(ticketId + 100000, body)
                                .with(asUser(studentId, "STUDENT")))
                .andExpect(status().isNotFound());
        jdbc.update(
                "UPDATE maintenance_request SET status='ASSIGNED', assigned_to=? WHERE id=?",
                technicianId,
                ticketId);
        mvc.perform(
                        ticketEdit(ticketId, body)
                                .with(asUser(studentId, "STUDENT")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void savingRoomWithUnknownRoomTypeIsNotFoundAndNotReportedAsInUse() throws Exception {
        mvc.perform(
                        post("/api/rooms/save-or-update")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"buildingId":%d,"roomTypeId":%d,"floor":1,"code":"Z999","gender":"M"}
                                        """
                                                .formatted(firstBuildingId, roomTypeId + 100000)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(org.hamcrest.Matchers.not("RESOURCE_IN_USE")));
    }

    @Test
    void shrinkingRoomTypeRetiresBedsWithHistoryAndGrowingReactivatesThem() throws Exception {
        Long fourBeds =
                jdbc.queryForObject(
                        "INSERT INTO room_type(name,bed_count,price_main,price_summer) VALUES"
                                + " (?,4,1000000,500000) RETURNING id",
                        Long.class,
                        "Bốn " + suffix);
        String response =
                mvc.perform(
                                post("/api/rooms/save-or-update")
                                        .with(asUser(staffId, "STAFF"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"buildingId":%d,"roomTypeId":%d,"floor":2,"code":"A104","gender":"M"}
                                                """
                                                        .formatted(firstBuildingId, fourBeds)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.beds.length()").value(4))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long roomId = mapper.readTree(response).get("id").asLong();
        Long bed04 =
                jdbc.queryForObject(
                        "SELECT id FROM bed WHERE room_id=? AND code='04'", Long.class, roomId);
        Long applicationId = createDraftApplication(createOpenPeriod());
        Long contractId =
                jdbc.queryForObject(
                        """
                        INSERT INTO dormitory_contract(code,student_id,application_id,start_date,end_date,activation_deadline,status)
                        VALUES (?,?,?,current_date,current_date+30,now()+interval '7 days','CANCELLED') RETURNING id
                        """,
                        Long.class,
                        "HD-" + suffix,
                        studentId,
                        applicationId);
        jdbc.update(
                """
                INSERT INTO room_assignment_history(student_id,contract_id,bed_id,kind,started_at,ended_at,end_reason,price_main,price_summer,created_by)
                VALUES(?,?,?,'HOLD',now()-interval '2 days',now()-interval '1 day','HOLD_CANCELLED',1000000,500000,?)
                """,
                studentId,
                contractId,
                bed04,
                staffId);
        mvc.perform(
                        post("/api/rooms/save-or-update")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(roomId, "{\"roomTypeId\":" + roomTypeId + "}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeBeds").value(2));
        assertThat(jdbc.queryForObject("SELECT status FROM bed WHERE id=?", String.class, bed04))
                .isEqualTo("RETIRED");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM bed WHERE room_id=?", Long.class, roomId))
                .isEqualTo(3L);
        mvc.perform(
                        post("/api/rooms/save-or-update")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(roomId, "{\"roomTypeId\":" + fourBeds + "}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeBeds").value(4));
        assertThat(jdbc.queryForObject("SELECT status FROM bed WHERE id=?", String.class, bed04))
                .isEqualTo("ACTIVE");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM bed WHERE room_id=?", Long.class, roomId))
                .isEqualTo(4L);
    }

    @Test
    void approvedApplicationWithLiveContractCannotBeCancelled() throws Exception {
        Long periodId = createOpenPeriod();
        Long id = createDraftApplication(periodId);
        for (String action : List.of("submit")) {
            mvc.perform(
                            post("/api/applications/" + id + "/" + action)
                                    .with(asUser(studentId, "STUDENT")))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/api/applications/" + id + "/approve").with(asUser(staffId, "STAFF")))
                .andExpect(status().isOk());
        Long contractId =
                jdbc.queryForObject(
                        """
                        INSERT INTO dormitory_contract(code,student_id,application_id,start_date,end_date,activation_deadline)
                        VALUES (?,?,?,current_date,current_date+30,now()+interval '7 days') RETURNING id
                        """,
                        Long.class,
                        "HD-" + suffix,
                        studentId,
                        id);
        mvc.perform(post("/api/applications/" + id + "/cancel").with(asUser(studentId, "STUDENT")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_HAS_CONTRACT"));
        mvc.perform(get("/api/applications/" + id).with(asUser(studentId, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedActions").isEmpty());
        jdbc.update("UPDATE dormitory_contract SET status='CANCELLED' WHERE id=?", contractId);
        mvc.perform(post("/api/applications/" + id + "/cancel").with(asUser(studentId, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void sharedRegistrationPeriodCannotBeRewrittenBySingleUnitStaff() throws Exception {
        Long semesterId = createSemester();
        Long periodId =
                jdbc.queryForObject(
                        """
                        INSERT INTO registration_period(semester_id,kind,name,open_at,close_at,created_by)
                        VALUES(?,'REGULAR','Đợt chung',now()+interval '10 days',now()+interval '20 days',?) RETURNING id
                        """,
                        Long.class,
                        semesterId,
                        staffId);
        for (Long unit : List.of(firstUnitId, otherUnitId)) {
            jdbc.update(
                    "INSERT INTO registration_period_unit(period_id,management_unit_id) VALUES"
                        + " (?,?)",
                    periodId,
                    unit);
        }
        mvc.perform(
                        post("/api/registration-periods/save-or-update")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(periodId, "{\"unitIds\":[" + firstUnitId + "]}")))
                .andExpect(status().isNotFound());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM registration_period_unit WHERE period_id=?",
                                Long.class,
                                periodId))
                .isEqualTo(2);
    }

    @Test
    void studentPatchKeepsFieldsThatAreNotSent() throws Exception {
        String result =
                mvc.perform(
                                post("/api/admin/students/save-or-update")
                                        .with(asUser(adminId, "ADMIN"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                mapper.writeValueAsString(
                                                        Map.of(
                                                                "studentCode", "PT" + suffix,
                                                                "fullName", "Tên cũ",
                                                                "gender", "M",
                                                                "faculty", "CNTT",
                                                                "phone", "0900000000"))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long id = mapper.readTree(result).get("userId").asLong();
        mvc.perform(
                        post("/api/admin/students/save-or-update")
                                .with(asUser(adminId, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(id, "{\"fullName\":\"Tên mới\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Tên mới"))
                .andExpect(jsonPath("$.faculty").value("CNTT"))
                .andExpect(jsonPath("$.phone").value("0900000000"))
                .andExpect(jsonPath("$.studentCode").value("PT" + suffix));
    }

    @Test
    void readingCannotBeInsertedBeforeAnExistingLaterMonth() throws Exception {
        for (String month : List.of("2026-10-01", "2026-09-01")) {
            mvc.perform(
                            post("/api/utility-readings/save-or-update")
                                    .with(asUser(staffId, "STAFF"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            mapper.writeValueAsString(
                                                    Map.of(
                                                            "roomId",
                                                            firstRoomId,
                                                            "meterType",
                                                            "ELECTRIC",
                                                            "billingMonth",
                                                            month,
                                                            "startReading",
                                                            100,
                                                            "endReading",
                                                            120))))
                    .andExpect(
                            month.equals("2026-10-01") ? status().isOk() : status().isConflict());
        }
    }

    @Test
    void frameworkErrorsAreNotReportedAsServerErrors() throws Exception {
        mvc.perform(get("/api/applications/1/submit").with(asUser(studentId, "STUDENT")))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
        mvc.perform(get("/api/does-not-exist").with(asUser(studentId, "STUDENT")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void duplicateBedCodeIsMappedInsteadOfConcurrentUpdate() throws Exception {
        Long roomId = createRoomWithBeds("A301");
        mvc.perform(
                        post("/api/beds/save-or-update")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of("roomId", roomId, "code", "01"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BED_CODE_EXISTS"));
    }

    @Test
    void changingRoomTypeSynchronizesBedsAndRoomTypeCapacityIsGuarded() throws Exception {
        Long roomId = createRoomWithBeds("A302");
        Long biggerType =
                jdbc.queryForObject(
                        "INSERT INTO room_type(name,bed_count,price_main,price_summer) VALUES"
                                + " (?,3,1000000,500000) RETURNING id",
                        Long.class,
                        "Lớn " + suffix);
        mvc.perform(
                        post("/api/rooms/save-or-update")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(roomId, "{\"roomTypeId\":" + biggerType + "}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.beds.length()").value(3));
        mvc.perform(
                        post("/api/room-types/save-or-update")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(biggerType, "{\"bedCount\":5}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_IN_USE"));
    }

    @Test
    void likeWildcardsInSearchAreTreatedAsPlainText() throws Exception {
        mvc.perform(searchByPage("/api/students", "q", "%").with(asUser(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private Long createRoomWithBeds(String code) throws Exception {
        String response =
                mvc.perform(
                                post("/api/rooms/save-or-update")
                                        .with(asUser(staffId, "STAFF"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"buildingId":%d,"roomTypeId":%d,"floor":3,"code":"%s","gender":"M"}
                                                """
                                                        .formatted(
                                                                firstBuildingId, roomTypeId, code)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return mapper.readTree(response).get("id").asLong();
    }

    @Test
    void adminWithoutUnitCanManageRoomsInEveryUnit() throws Exception {
        // ADMIN không cần giả vai STAFF hoặc gắn đơn vị để dùng CRUD nghiệp vụ.
        mvc.perform(get("/api/rooms/" + otherRoomId).with(asUser(adminId, "ADMIN")))
                .andExpect(status().isOk());
        String created =
                mvc.perform(
                                post("/api/rooms/save-or-update")
                                        .with(asUser(adminId, "ADMIN"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"buildingId":%d,"roomTypeId":%d,"floor":2,"code":"B102","gender":"M"}
                                                """
                                                        .formatted(otherBuildingId, roomTypeId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.beds.length()").value(2))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long roomId = mapper.readTree(created).get("id").asLong();
        mvc.perform(
                        searchByPage("/api/rooms", "buildingId", otherBuildingId.toString())
                                .with(asUser(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(
                        post("/api/rooms/save-or-update")
                                .with(asUser(adminId, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(roomId, "{\"code\":\"B103\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("B103"));
        mvc.perform(
                        post("/api/rooms/save-or-update")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(roomId, "{\"code\":\"Không có quyền\"}")))
                .andExpect(status().isNotFound());
        mvc.perform(searchByPage("/api/rooms").with(asUser(studentId, "STUDENT")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/rooms/" + roomId).with(asUser(adminId, "ADMIN")))
                .andExpect(status().isNoContent());
        entityManager.flush();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT actor_id FROM audit_log WHERE action='ROOM_DELETE' AND"
                                        + " entity_id=?",
                                Long.class,
                                roomId))
                .isEqualTo(adminId);
    }

    @Test
    void adminCanManageForeignRegistrationPeriodWhileAggregateStaffCannotWriteIt()
            throws Exception {
        Long semesterId = createSemester();
        String created =
                mvc.perform(
                                post("/api/registration-periods/save-or-update")
                                        .with(asUser(adminId, "ADMIN"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"semesterId":%d,"kind":"REGULAR","name":"Đợt đơn vị khác",
                                                "openAt":"2040-08-01T08:00:00+07:00","closeAt":"2040-08-31T08:00:00+07:00",
                                                "unitIds":[%d],"quotas":[{"buildingId":%d,"quota":10}]}
                                                """
                                                        .formatted(
                                                                semesterId,
                                                                otherUnitId,
                                                                otherBuildingId)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long periodId = mapper.readTree(created).get("id").asLong();
        jdbc.update("UPDATE app_user SET cross_unit_access=true WHERE id=?", staffId);
        entityManager.clear();
        mvc.perform(get("/api/registration-periods/" + periodId).with(asUser(staffId, "STAFF")))
                .andExpect(status().isOk());
        mvc.perform(
                        post("/api/registration-periods/save-or-update")
                                .with(asUser(staffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(periodId, "{\"name\":\"Sửa ngoài phạm vi\"}")))
                .andExpect(status().isNotFound());
        mvc.perform(
                        post("/api/registration-periods/save-or-update")
                                .with(asUser(adminId, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(periodId, "{\"name\":\"ADMIN cập nhật\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ADMIN cập nhật"));
        mvc.perform(delete("/api/registration-periods/" + periodId).with(asUser(adminId, "ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCreatesAndProcessesApplicationOnBehalfWithoutChangingAuditActor() throws Exception {
        Long periodId = createOpenPeriod();
        jdbc.update(
                "INSERT INTO registration_period_unit(period_id,management_unit_id) VALUES(?,?)",
                periodId,
                otherUnitId);
        String created =
                mvc.perform(
                                post("/api/applications/save-or-update")
                                        .with(asUser(adminId, "ADMIN"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                mapper.writeValueAsString(
                                                        Map.of(
                                                                "periodId", periodId,
                                                                "preferredUnitId", otherUnitId,
                                                                "studentId", studentId))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.student.id").value(studentId))
                        .andExpect(jsonPath("$.timeline[0].actorId").value(adminId))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long applicationId = mapper.readTree(created).get("id").asLong();
        mvc.perform(
                        post("/api/applications/save-or-update")
                                .with(asUser(adminId, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        withId(
                                                applicationId,
                                                "{\"note\":\"ADMIN sửa đơn hộ sinh viên\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value("ADMIN sửa đơn hộ sinh viên"));
        mvc.perform(
                        post("/api/applications/" + applicationId + "/submit")
                                .with(asUser(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedActions").value(hasItem("approve")));
        mvc.perform(
                        post("/api/applications/" + applicationId + "/approve")
                                .with(asUser(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        // Có mọi quyền không có nghĩa là được bỏ qua trạng thái đã xử lý.
        mvc.perform(
                        post("/api/applications/" + applicationId + "/approve")
                                .with(asUser(adminId, "ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATE_TRANSITION"));
        mvc.perform(
                        post("/api/applications/" + applicationId + "/cancel")
                                .with(asUser(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        entityManager.flush();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM status_history WHERE"
                                    + " entity_type='APPLICATION' AND entity_id=? AND actor_id=?",
                                Long.class,
                                applicationId,
                                adminId))
                .isEqualTo(4);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM status_history WHERE"
                                    + " entity_type='APPLICATION' AND entity_id=? AND actor_id<>?",
                                Long.class,
                                applicationId,
                                adminId))
                .isZero();
    }

    @Test
    void studentCannotForgeApplicantAndAdminMustChooseAStudent() throws Exception {
        Long periodId = createOpenPeriod();
        mvc.perform(
                        post("/api/applications/save-or-update")
                                .with(asUser(studentId, "STUDENT"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "periodId", periodId,
                                                        "preferredUnitId", firstUnitId,
                                                        "studentId", adminId))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        mvc.perform(
                        post("/api/applications/save-or-update")
                                .with(asUser(adminId, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "periodId", periodId,
                                                        "preferredUnitId", firstUnitId))))
                .andExpect(status().isBadRequest());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM room_application WHERE period_id=?",
                                Long.class,
                                periodId))
                .isZero();
    }

    @Test
    void adminCanWorkTicketAssignedToAnotherTechnicianButCannotRestartClosedTicket()
            throws Exception {
        Long otherTechnicianId = createUser("TECHNICIAN", otherUnitId);
        MockMultipartFile data =
                new MockMultipartFile(
                        "data",
                        "data.json",
                        "application/json",
                        """
                        {"category":"ELECTRIC","severity":"MEDIUM","description":"Đèn hỏng ở đơn vị khác","buildingId":%d,"floor":1}
                        """
                                .formatted(otherBuildingId)
                                .getBytes(StandardCharsets.UTF_8));
        String created =
                mvc.perform(
                                multipart("/api/maintenance-requests/save-or-update")
                                        .file(data)
                                        .with(asUser(otherStaffId, "STAFF")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long ticketId = mapper.readTree(created).get("id").asLong();
        mvc.perform(
                        post("/api/maintenance-requests/" + ticketId + "/assign")
                                .with(asUser(otherStaffId, "STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"technicianId\":" + otherTechnicianId + "}"))
                .andExpect(status().isOk());
        mvc.perform(
                        post("/api/maintenance-requests/" + ticketId + "/start")
                                .with(asUser(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTo").value(otherTechnicianId))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mvc.perform(
                        post("/api/maintenance-requests/" + ticketId + "/resolve")
                                .with(asUser(adminId, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"note\":\"ADMIN xác nhận đã sửa xong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
        mvc.perform(
                        post("/api/maintenance-requests/" + ticketId + "/accept")
                                .with(asUser(adminId, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"ADMIN nghiệm thu thay người yêu cầu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        mvc.perform(
                        post("/api/maintenance-requests/" + ticketId + "/start")
                                .with(asUser(adminId, "ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void adminManagesForeignUtilityInvoiceAndStudentCannotUpdateIt() throws Exception {
        String created =
                mvc.perform(
                                post("/api/invoices/save-or-update")
                                        .with(asUser(adminId, "ADMIN"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                mapper.writeValueAsString(
                                                        Map.of(
                                                                "type",
                                                                "UTILITY",
                                                                "roomId",
                                                                otherRoomId,
                                                                "billingMonth",
                                                                "2026-10-01",
                                                                "totalAmount",
                                                                20000,
                                                                "dueDate",
                                                                LocalDate.now()
                                                                        .plusDays(7)
                                                                        .toString()))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long invoiceId = mapper.readTree(created).get("id").asLong();
        mvc.perform(get("/api/invoices/" + invoiceId).with(asUser(adminId, "ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/invoices/" + invoiceId).with(asUser(staffId, "STAFF")))
                .andExpect(status().isNotFound());
        mvc.perform(
                        post("/api/invoices/save-or-update")
                                .with(asUser(studentId, "STUDENT"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(invoiceId, "{\"totalAmount\":1}")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/invoices/save-or-update")
                                .with(asUser(adminId, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(invoiceId, "{\"totalAmount\":30000}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(30000));
        mvc.perform(delete("/api/invoices/" + invoiceId).with(asUser(adminId, "ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCanEditAnotherPublishersNotificationWhileStaffCannot() throws Exception {
        String created =
                mvc.perform(
                                post("/api/notifications/save-or-update")
                                        .with(asUser(otherStaffId, "STAFF"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                mapper.writeValueAsString(
                                                        Map.of(
                                                                "scope",
                                                                "UNIT",
                                                                "unitId",
                                                                otherUnitId,
                                                                "title",
                                                                "Thông báo khác " + suffix,
                                                                "body",
                                                                "Nội dung ban đầu"))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long notificationId = mapper.readTree(created).get("id").asLong();
        mvc.perform(
                        get("/api/notifications/published/" + notificationId)
                                .with(asUser(staffId, "STAFF")))
                .andExpect(status().isNotFound());
        mvc.perform(
                        searchByPage(
                                        "/api/notifications/published",
                                        "q",
                                        "Thông báo khác " + suffix)
                                .with(asUser(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(
                        post("/api/notifications/save-or-update")
                                .with(asUser(adminId, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        withId(
                                                notificationId,
                                                mapper.writeValueAsString(
                                                        Map.of(
                                                                "scope", "UNIT",
                                                                "unitId", otherUnitId,
                                                                "title", "ADMIN cập nhật",
                                                                "body", "Nội dung mới")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("ADMIN cập nhật"));
        mvc.perform(delete("/api/notifications/" + notificationId).with(asUser(adminId, "ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void meExposesRoleSpecificCrudPermissions() throws Exception {
        mvc.perform(get("/api/auth/me").with(asUser(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions").value(hasItem("ROOM_CREATE")))
                .andExpect(jsonPath("$.permissions").value(hasItem("USER_CREATE")));
        mvc.perform(get("/api/auth/me").with(asUser(staffId, "STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions").value(hasItem("ROOM_CREATE")))
                .andExpect(jsonPath("$.permissions").value(not(hasItem("USER_CREATE"))))
                .andExpect(jsonPath("$.permissions").value(hasItem("CONTRACT_EXTENSION_UPDATE")))
                .andExpect(
                        jsonPath("$.permissions").value(not(hasItem("CONTRACT_EXTENSION_CREATE"))))
                .andExpect(jsonPath("$.permissions").value(hasItem("ROOM_TRANSFER_UPDATE")))
                .andExpect(jsonPath("$.permissions").value(not(hasItem("ROOM_TRANSFER_DELETE"))))
                .andExpect(jsonPath("$.permissions").value(hasItem("SUMMER_REGISTRATION_READ")))
                .andExpect(
                        jsonPath("$.permissions")
                                .value(not(hasItem("SUMMER_REGISTRATION_UPDATE"))));
        mvc.perform(get("/api/auth/me").with(asUser(technicianId, "TECHNICIAN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions").value(hasItem("MAINTENANCE_UPDATE")))
                .andExpect(jsonPath("$.permissions").value(not(hasItem("ROOM_CREATE"))));
        mvc.perform(get("/api/auth/me").with(asUser(studentId, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions").value(hasItem("ROOM_APPLICATION_CREATE")))
                .andExpect(jsonPath("$.permissions").value(hasItem("ROOM_APPLICATION_DELETE")))
                .andExpect(jsonPath("$.permissions").value(not(hasItem("INVOICE_UPDATE"))));
    }

    /** Các điều kiện tìm tài khoản phải giao nhau; không để tìm theo tên bỏ qua đơn vị/khóa. */
    @Test
    void accountFiltersCombineRoleUnitLockedAndSearchWithAccuratePaging() throws Exception {
        String marker = "Filter " + suffix;
        jdbc.update(
                "UPDATE app_user SET full_name=? WHERE id IN (?, ?, ?)",
                marker,
                staffId,
                otherStaffId,
                technicianId);
        jdbc.update("UPDATE app_user SET locked=true WHERE id=?", otherStaffId);
        Long lockedFirstUnitStaff =
                jdbc.queryForObject(
                        "INSERT INTO"
                            + " app_user(username,role,full_name,management_unit_id,locked,keycloak_user_id)"
                            + " VALUES(?,'STAFF',?,?,true,?) RETURNING id",
                        Long.class,
                        "locked-filter-" + suffix,
                        marker,
                        firstUnitId,
                        UUID.randomUUID().toString());

        mvc.perform(
                        searchByPage(
                                        "/api/admin/users",
                                        "q",
                                        marker,
                                        "role",
                                        "STAFF",
                                        "size",
                                        "1",
                                        "page",
                                        "1",
                                        "sort",
                                        "id,desc")
                                .with(asUser(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(otherStaffId));
        mvc.perform(
                        searchByPage(
                                        "/api/admin/users",
                                        "q",
                                        marker,
                                        "role",
                                        "STAFF",
                                        "managementUnitId",
                                        firstUnitId.toString(),
                                        "locked",
                                        "true")
                                .with(asUser(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(lockedFirstUnitStaff));
        mvc.perform(
                        searchByPage(
                                        "/api/admin/users",
                                        "q",
                                        marker,
                                        "role",
                                        "STAFF",
                                        "managementUnitId",
                                        firstUnitId.toString(),
                                        "locked",
                                        "false")
                                .with(asUser(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(staffId));
    }

    /** Fixture ở hai biên thời gian và các bản ghi gần giống phải cho đúng count lẫn từng trang. */
    @Test
    void auditFiltersCombineActorActionTypeSearchAndInclusiveTimeBoundaries() throws Exception {
        String marker = "Audit " + suffix;
        String action = "FILTER_" + suffix;
        jdbc.update("UPDATE app_user SET full_name=? WHERE id=?", marker, staffId);
        Long startBoundary = createAuditFixture(staffId, action, "USER", "2045-01-01T00:00:00Z");
        Long endBoundary = createAuditFixture(staffId, action, "USER", "2045-01-02T00:00:00Z");
        createAuditFixture(otherStaffId, action, "USER", "2045-01-01T12:00:00Z");
        createAuditFixture(staffId, action, "ROOM", "2045-01-01T12:00:00Z");
        createAuditFixture(staffId, "OTHER_ACTION", "USER", "2045-01-01T12:00:00Z");
        createAuditFixture(staffId, action, "USER", "2044-12-31T23:59:59Z");
        createAuditFixture(staffId, action, "USER", "2045-01-02T00:00:01Z");

        for (int page = 0; page < 2; page++) {
            mvc.perform(
                            searchByPage(
                                            "/api/admin/audit-logs",
                                            "q",
                                            marker,
                                            "actorId",
                                            staffId.toString(),
                                            "action",
                                            action,
                                            "entityType",
                                            "USER",
                                            "from",
                                            "2045-01-01T00:00:00Z",
                                            "to",
                                            "2045-01-02T00:00:00Z",
                                            "page",
                                            Integer.toString(page),
                                            "size",
                                            "1")
                                    .with(asUser(adminId, "ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(
                            jsonPath("$.content[0].id")
                                    .value(page == 0 ? endBoundary : startBoundary));
        }
        // Chuỗi tìm kiếm được bind như dữ liệu; không biến dấu nháy thành một phần câu SQL.
        mvc.perform(
                        searchByPage("/api/admin/audit-logs", "action", "' OR 1=1 --")
                                .with(asUser(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    /** Đầu vào bộ lọc sai phải báo 400; quyền tổng hợp của Staff vẫn không mở API quản trị. */
    @Test
    void managementFiltersRejectInvalidIdentifiersRangesAndNonAdminAccess() throws Exception {
        mvc.perform(
                        searchByPage("/api/admin/users", "managementUnitId", "0")
                                .with(asUser(adminId, "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        mvc.perform(
                        searchByPage("/api/admin/users", "locked", "invalid")
                                .with(asUser(adminId, "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        mvc.perform(
                        searchByPage("/api/admin/audit-logs", "actorId", "-1")
                                .with(asUser(adminId, "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        mvc.perform(
                        searchByPage(
                                        "/api/admin/audit-logs",
                                        "from",
                                        "2045-01-02T00:00:00Z",
                                        "to",
                                        "2045-01-01T00:00:00Z")
                                .with(asUser(adminId, "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        jdbc.update("UPDATE app_user SET cross_unit_access=true WHERE id=?", staffId);
        mvc.perform(searchByPage("/api/admin/users").with(asUser(staffId, "STAFF")))
                .andExpect(status().isForbidden());
        mvc.perform(searchByPage("/api/admin/audit-logs").with(asUser(staffId, "STAFF")))
                .andExpect(status().isForbidden());
    }

    /** Annotation OR chỉ mở cổng chung; Service vẫn kiểm tra CREATE và UPDATE riêng theo id. */
    @Test
    void saveWithCreateOnlyPermissionCannotUpdateExistingRow() throws Exception {
        grantStaffPermissions(
                Permission.PROFILE_READ, Permission.ROOM_READ, Permission.ROOM_CREATE);
        mvc.perform(
                        post("/api/rooms/save-or-update")
                                .with(asUser(staffId, Role.STAFF))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(firstRoomId, "{\"code\":\"FORBIDDEN\"}")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/rooms/save-or-update")
                                .with(asUser(staffId, Role.STAFF))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(newRoomPayload("CREATE-ONLY")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CREATE-ONLY"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT code FROM room WHERE id=?", String.class, firstRoomId))
                .isEqualTo("A101");
    }

    /** Có quyền sửa không đồng nghĩa được tạo mới khi dùng chung save-or-update. */
    @Test
    void saveWithUpdateOnlyPermissionCannotCreateNewRow() throws Exception {
        grantStaffPermissions(
                Permission.PROFILE_READ, Permission.ROOM_READ, Permission.ROOM_UPDATE);
        Long before = jdbc.queryForObject("SELECT count(*) FROM room", Long.class);
        mvc.perform(
                        post("/api/rooms/save-or-update")
                                .with(asUser(staffId, Role.STAFF))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(newRoomPayload("NO-CREATE")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/rooms/save-or-update")
                                .with(asUser(staffId, Role.STAFF))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(firstRoomId, "{\"code\":\"UPDATED\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("UPDATED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM room", Long.class)).isEqualTo(before);
    }

    /** Id sai không được rơi xuống nhánh tạo; id hợp lệ nhưng không tồn tại trả 404. */
    @Test
    void saveRejectsInvalidIdentifierAndNeverInsertsForMissingEntity() throws Exception {
        Long before = jdbc.queryForObject("SELECT count(*) FROM room", Long.class);
        for (String badId : List.of("0", "-1", "1.5", "\"not-an-id\"")) {
            mvc.perform(
                            post("/api/rooms/save-or-update")
                                    .with(asUser(adminId, Role.ADMIN))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"id\":" + badId + ",\"code\":\"INVALID\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        }
        mvc.perform(
                        post("/api/rooms/save-or-update")
                                .with(asUser(adminId, Role.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(Long.MAX_VALUE, "{\"code\":\"MISSING\"}")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM room", Long.class)).isEqualTo(before);
    }

    /** Tạo mới kiểm tra trường bắt buộc, cập nhật giữ trường vắng mặt và chặn mass assignment. */
    @Test
    void saveValidatesCreateAndPartialUpdateWithoutAcceptingSecurityFields() throws Exception {
        mvc.perform(
                        post("/api/rooms/save-or-update")
                                .with(asUser(adminId, Role.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"INCOMPLETE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        mvc.perform(
                        post("/api/rooms/save-or-update")
                                .with(asUser(adminId, Role.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(firstRoomId, "{\"code\":\"PARTIAL\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buildingId").value(firstBuildingId))
                .andExpect(jsonPath("$.code").value("PARTIAL"));
        for (String forbiddenField :
                List.of("status", "createdBy", "ownerId", "permissions", "unknownField")) {
            ObjectNode payload = (ObjectNode) mapper.readTree(newRoomPayload("UNTRUSTED"));
            payload.put(forbiddenField, "CLIENT-CONTROLLED");
            mvc.perform(
                            post("/api/rooms/save-or-update")
                                    .with(asUser(adminId, Role.ADMIN))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        }
    }

    /** Body tìm kiếm được kiểm tra kiểu JSON và không cho client tự chọn chủ sở hữu/phạm vi. */
    @Test
    void searchBodyRejectsMalformedPagingTypesAndClientScopeOverrides() throws Exception {
        for (String payload :
                List.of(
                        "{\"page\":-1}",
                        "{\"size\":0}",
                        "{\"size\":101}",
                        "{\"page\":\"invalid\"}",
                        "{\"page\":0.5}",
                        "{\"page\":null}",
                        "{\"locked\":\"invalid\"}",
                        "{\"sort\":[\"passwordHash,desc\"]}",
                        "{\"sort\":[null]}",
                        "{\"ownerId\":1}",
                        "{\"mine\":false}",
                        "{\"currentUserId\":1}")) {
            mvc.perform(
                            post("/api/rooms/search-by-page")
                                    .with(asUser(adminId, Role.ADMIN))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        }
    }

    /** Danh mục nhỏ cũng trả cùng cấu trúc phân trang, không trả mảng làm FE xử lý hai kiểu. */
    @Test
    void catalogsReturnConsistentPageEnvelopeAndAccurateCounts() throws Exception {
        for (String base :
                List.of(
                        "/api/management-units",
                        "/api/buildings",
                        "/api/room-types",
                        "/api/priority-categories",
                        "/api/admin/configs",
                        "/api/semesters")) {
            mvc.perform(searchByPage(base, "size", 1).with(asUser(adminId, Role.ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(1))
                    .andExpect(jsonPath("$.totalElements").isNumber())
                    .andExpect(jsonPath("$.totalPages").isNumber());
        }
        mvc.perform(
                        searchByPage("/api/management-units", "q", "K1", "size", 1)
                                .with(asUser(adminId, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].code").value("K1"));
    }

    /** Giữ bộ lọc loại đồng hồ và hỗ trợ cập nhật một phần bằng API ghi duy nhất. */
    @Test
    void utilityRateSaveAndSearchRetainMeterTypeFilterAndPartialValues() throws Exception {
        String result =
                mvc.perform(
                                post("/api/utility-rates/save-or-update")
                                        .with(asUser(adminId, Role.ADMIN))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"meterType\":\"ELECTRIC\",\"unitPrice\":3500,\"effectiveFrom\":\"2095-01-01\"}"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long rateId = mapper.readTree(result).path("id").asLong();
        mvc.perform(
                        post("/api/utility-rates/save-or-update")
                                .with(asUser(adminId, Role.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"meterType\":\"WATER\",\"unitPrice\":18000,\"effectiveFrom\":\"2095-01-01\"}"))
                .andExpect(status().isOk());
        mvc.perform(
                        post("/api/utility-rates/save-or-update")
                                .with(asUser(adminId, Role.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(rateId, "{\"unitPrice\":3700}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meterType").value("ELECTRIC"))
                .andExpect(jsonPath("$.effectiveFrom").value("2095-01-01"))
                .andExpect(jsonPath("$.unitPrice").value(3700));
        mvc.perform(
                        searchByPage(
                                        "/api/utility-rates",
                                        "meterType",
                                        "ELECTRIC",
                                        "size",
                                        1,
                                        "sort",
                                        "effectiveFrom,desc")
                                .with(asUser(adminId, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(rateId))
                .andExpect(jsonPath("$.content[0].meterType").value("ELECTRIC"));
    }

    /** Hồ sơ chỉ ghi người hiện tại; cấu hình tìm bằng key, không tạo key tùy ý. */
    @Test
    void profileAndConfigurationSaveKeepTheirRestrictedIdentifiers() throws Exception {
        mvc.perform(
                        post("/api/me/profile/save-or-update")
                                .with(asUser(adminId, Role.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"phone\":\"0901234567\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adminId))
                .andExpect(jsonPath("$.phone").value("0901234567"));
        mvc.perform(
                        post("/api/me/profile/save-or-update")
                                .with(asUser(adminId, Role.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"STAFF\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/admin/configs/save-or-update")
                                .with(asUser(adminId, Role.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"key\":\"invoice.due_days\",\"value\":\"9\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("invoice.due_days"))
                .andExpect(jsonPath("$.value").value("9"));
        mvc.perform(
                        post("/api/admin/configs/save-or-update")
                                .with(asUser(adminId, Role.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"key\":\"not-a-registered-key\",\"value\":\"9\"}"))
                .andExpect(status().isNotFound());
    }

    /** Alias cập nhật sinh viên của Staff không được vô tình mở nhánh tạo tài khoản. */
    @Test
    void staffStudentSaveAliasCannotCreateAccounts() throws Exception {
        for (String base : List.of("/api/students", "/api/admin/students")) {
            mvc.perform(
                            post(base + "/save-or-update")
                                    .with(asUser(staffId, Role.STAFF))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"studentCode\":\"NO-CREATE\",\"fullName\":\"Không"
                                                    + " tạo\",\"gender\":\"M\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    /** Tạo và đổi mã giường đi cùng API nhưng không làm mất phòng đang sở hữu giường. */
    @Test
    void bedSaveCreatesThenUpdatesSameBedWithoutChangingItsRoom() throws Exception {
        String created =
                mvc.perform(
                                post("/api/beds/save-or-update")
                                        .with(asUser(staffId, Role.STAFF))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                mapper.writeValueAsString(
                                                        Map.of(
                                                                "roomId",
                                                                firstRoomId,
                                                                "code",
                                                                "G1"))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value("G1"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long bedId = mapper.readTree(created).path("id").asLong();
        mvc.perform(
                        post("/api/beds/save-or-update")
                                .with(asUser(staffId, Role.STAFF))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withId(bedId, "{\"code\":\"G2\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bedId))
                .andExpect(jsonPath("$.code").value("G2"));
        assertThat(jdbc.queryForObject("SELECT room_id FROM bed WHERE id=?", Long.class, bedId))
                .isEqualTo(firstRoomId);
    }

    /** Sửa multipart dùng id trong phần data và giữ ảnh hiện có khi không chọn ảnh mới. */
    @Test
    void maintenanceSaveUpdatesMultipartDataAndKeepsExistingPhotos() throws Exception {
        MockMultipartFile data =
                new MockMultipartFile(
                        "data",
                        "data.json",
                        "application/json",
                        mapper.writeValueAsBytes(
                                Map.of(
                                        "category",
                                        "ELECTRIC",
                                        "severity",
                                        "LOW",
                                        "description",
                                        "Nội dung ban đầu",
                                        "buildingId",
                                        firstBuildingId,
                                        "floor",
                                        1)));
        MockMultipartFile photo =
                new MockMultipartFile(
                        "photos",
                        "valid.png",
                        "image/png",
                        new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
        String created =
                mvc.perform(
                                multipart("/api/maintenance-requests/save-or-update")
                                        .file(data)
                                        .file(photo)
                                        .with(asUser(staffId, Role.STAFF)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.photos.length()").value(1))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long ticketId = mapper.readTree(created).path("id").asLong();
        Long photoId = mapper.readTree(created).path("photos").get(0).path("id").asLong();
        MockMultipartFile update =
                new MockMultipartFile(
                        "data",
                        "data.json",
                        "application/json",
                        mapper.writeValueAsBytes(
                                Map.of(
                                        "id",
                                        ticketId,
                                        "category",
                                        "ELECTRIC",
                                        "severity",
                                        "MEDIUM",
                                        "description",
                                        "Nội dung chỉnh sửa")));
        mvc.perform(
                        multipart("/api/maintenance-requests/save-or-update")
                                .file(update)
                                .with(asUser(staffId, Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId))
                .andExpect(jsonPath("$.description").value("Nội dung chỉnh sửa"))
                .andExpect(jsonPath("$.severity").value("MEDIUM"))
                .andExpect(jsonPath("$.photos.length()").value(1))
                .andExpect(jsonPath("$.photos[0].id").value(photoId));
    }

    @Test
    void staffCannotChangeStudentIdentityButCanUpdateContactInsideOwnUnit() throws Exception {
        createDraftApplication(createOpenPeriod());
        for (var change : Map.of("studentCode", "NEW" + suffix,
                "fullName", "Tên không được phép đổi", "gender", "F").entrySet()) {
            mvc.perform(post("/api/students/save-or-update").with(asUser(staffId, Role.STAFF))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(Map.of("id", studentId, change.getKey(), change.getValue()))))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("errorCode").value("FORBIDDEN"));
        }
        mvc.perform(post("/api/students/save-or-update").with(asUser(staffId, Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("id", studentId, "phone", "0912345678"))))
                .andExpect(status().isOk()).andExpect(jsonPath("phone").value("0912345678"));
        assertThat(jdbc.queryForObject("SELECT gender FROM student_profile WHERE user_id=?", String.class, studentId)).isEqualTo("M");
    }

    @Test
    void auditDetailsAreAdminOnlyAndShowSystemActorAndBeforeAfterValues() throws Exception {
        Long logId = jdbc.queryForObject("INSERT INTO audit_log(action,entity_type,old_value,new_value)"
                + " VALUES('TEST_AUDIT','CONFIG','{\"value\":1}'::jsonb,'{\"value\":2}'::jsonb) RETURNING id", Long.class);
        mvc.perform(get("/api/admin/audit-logs/{id}", logId).with(asUser(adminId, Role.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("fullName").value("SYSTEM"))
                .andExpect(jsonPath("oldValue").value("{\"value\": 1}"))
                .andExpect(jsonPath("newValue").value("{\"value\": 2}"));
        mvc.perform(get("/api/admin/audit-logs/{id}", logId).with(asUser(staffId, Role.STAFF)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("requestId").isNotEmpty());
        mvc.perform(get("/api/admin/audit-logs/{id}", Long.MAX_VALUE).with(asUser(adminId, Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    /** Mọi màn và lookup dùng cùng envelope, tổng ổn định khi đổi trang và tìm không có kết quả. */
    @Test
    void allCrudListsAndLookupsSharePagedSearchContract() throws Exception {
        List<String> adminLists =
                List.of(
                        "/api/management-units",
                        "/api/buildings",
                        "/api/room-types",
                        "/api/semesters",
                        "/api/priority-categories",
                        "/api/admin/users",
                        "/api/students",
                        "/api/admin/students",
                        "/api/rooms",
                        "/api/registration-periods",
                        "/api/applications",
                        "/api/contracts",
                        "/api/extension-requests",
                        "/api/transfer-requests",
                        "/api/summer-choices",
                        "/api/storage-records",
                        "/api/utility-readings",
                        "/api/utility-rates",
                        "/api/invoices",
                        "/api/deposits",
                        "/api/maintenance-requests",
                        "/api/notifications/published",
                        "/api/notifications",
                        "/api/admin/configs",
                        "/api/admin/audit-logs",
                        "/api/technicians");
        var endpoints = new LinkedHashMap<String, RequestPostProcessor>();
        for (String base : adminLists) {
            endpoints.put(base, asUser(adminId, Role.ADMIN));
        }
        endpoints.put("/api/me/registration-periods/open", asUser(studentId, Role.STUDENT));
        endpoints.put("/api/me/room", asUser(studentId, Role.STUDENT));
        for (var endpoint : endpoints.entrySet()) {
            String base = endpoint.getKey();
            var first =
                    mapper.readTree(
                            mvc.perform(searchByPage(base, "size", 1).with(endpoint.getValue()))
                                    .andExpect(status().isOk())
                                    .andExpect(jsonPath("$.content").isArray())
                                    .andExpect(jsonPath("$.page").value(0))
                                    .andExpect(jsonPath("$.size").value(1))
                                    .andExpect(jsonPath("$.totalElements").isNumber())
                                    .andExpect(jsonPath("$.totalPages").isNumber())
                                    .andReturn()
                                    .getResponse()
                                    .getContentAsString());
            long total = first.path("totalElements").asLong();
            assertThat(first.path("content").size()).as(base).isEqualTo(total == 0 ? 0 : 1);
            assertThat(first.path("totalPages").asLong()).as(base).isEqualTo(total);
            mvc.perform(searchByPage(base, "size", 1, "page", total + 1).with(endpoint.getValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(total));
            mvc.perform(searchByPage(base, "q", "NO-MATCH-" + suffix).with(endpoint.getValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0));
        }
    }

    /** Sắp xếp phải thực thi đúng cột native/SQL; ID mất không được biến thành tạo yêu cầu mới. */
    @Test
    void nativeAndSqlSortsAreAppliedAndMissingStaySaveDoesNotInsert() throws Exception {
        String marker = "ORDER" + suffix;
        Long periodId = createOpenPeriod();
        Long applicationId = createDraftApplication(periodId);
        Long earlierContract =
                jdbc.queryForObject(
                        "INSERT INTO dormitory_contract(code,student_id,application_id,start_date,"
                                + "end_date,status,activation_deadline) VALUES(?,?,?,"
                                + "'2040-09-01','2041-01-31','CANCELLED',now()) RETURNING id",
                        Long.class,
                        marker + "Z",
                        studentId,
                        applicationId);
        Long laterContract =
                jdbc.queryForObject(
                        "INSERT INTO dormitory_contract(code,student_id,application_id,start_date,"
                                + "end_date,status,activation_deadline) VALUES(?,?,?,"
                                + "'2040-10-01','2041-02-28','CANCELLED',now()) RETURNING id",
                        Long.class,
                        marker + "A",
                        studentId,
                        applicationId);
        jdbc.update(
                "UPDATE app_user SET full_name=? WHERE id IN (?,?)",
                marker + " Z",
                studentId,
                technicianId);
        jdbc.update(
                "UPDATE student_profile SET student_code=? WHERE user_id=?",
                marker + "Z",
                studentId);
        var additionalIds = new LinkedHashMap<String, Long>();
        for (String role : List.of(Role.STUDENT, Role.TECHNICIAN)) {
            Long id =
                    jdbc.queryForObject(
                            "INSERT INTO"
                                + " app_user(username,role,full_name,management_unit_id,must_change_password,keycloak_user_id)"
                                + " VALUES(?,?,?,?,false,?) RETURNING id",
                            Long.class,
                            marker + role,
                            role,
                            marker + " A",
                            Role.TECHNICIAN.equals(role) ? firstUnitId : null,
                            UUID.randomUUID().toString());
            additionalIds.put(role, id);
            if (Role.STUDENT.equals(role)) {
                jdbc.update(
                        "INSERT INTO student_profile(user_id,student_code,gender) VALUES(?,?,'M')",
                        id,
                        marker + "A");
            }
        }
        Long laterAudit =
                createAuditFixture(adminId, marker + "Z", "SORT_CASE", "2045-01-02T00:00:00Z");
        Long earlierAudit =
                createAuditFixture(adminId, marker + "A", "SORT_CASE", "2045-01-01T00:00:00Z");
        record SortCase(String base, String field, Long ascendingFirst, Long descendingFirst) {}
        for (SortCase sortCase :
                List.of(
                        new SortCase("/api/contracts", "code", laterContract, earlierContract),
                        new SortCase("/api/contracts", "startDate", earlierContract, laterContract),
                        new SortCase(
                                "/api/students",
                                "studentCode",
                                additionalIds.get(Role.STUDENT),
                                studentId),
                        new SortCase(
                                "/api/technicians",
                                "fullName",
                                additionalIds.get(Role.TECHNICIAN),
                                technicianId),
                        new SortCase(
                                "/api/admin/audit-logs", "createdAt", earlierAudit, laterAudit))) {
            for (String direction : List.of("asc", "desc")) {
                Long expected =
                        "asc".equals(direction)
                                ? sortCase.ascendingFirst()
                                : sortCase.descendingFirst();
                mvc.perform(
                                searchByPage(
                                                sortCase.base(),
                                                "q",
                                                marker,
                                                "size",
                                                1,
                                                "sort",
                                                sortCase.field() + "," + direction)
                                        .with(asUser(adminId, Role.ADMIN)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.totalElements").value(2))
                        .andExpect(jsonPath("$.content[0].id").value(expected));
            }
            mvc.perform(
                            searchByPage(sortCase.base(), "sort", "id desc; SELECT pg_sleep(1),asc")
                                    .with(asUser(adminId, Role.ADMIN)))
                    .andExpect(status().isBadRequest());
        }
        for (String resource : List.of("extension-requests", "transfer-requests")) {
            String table =
                    "extension-requests".equals(resource)
                            ? "contract_extension_request"
                            : "room_transfer_request";
            Long before = jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
            mvc.perform(
                            post("/api/" + resource + "/save-or-update")
                                    .with(asUser(adminId, Role.ADMIN))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            withId(
                                                    Long.MAX_VALUE,
                                                    "{\"reason\":\"Không tạo ngầm\"}")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class))
                    .isEqualTo(before);
        }
    }

    /** Không âm thầm bỏ qua trường sai nhánh rồi tạo/sửa tài khoản hoặc gọi Keycloak. */
    @Test
    void userSaveRejectsWrongBranchFieldsBeforeDatabaseAndIdentitySideEffects() throws Exception {
        Long userCount = jdbc.queryForObject("SELECT count(*) FROM app_user", Long.class);
        Long auditCount = jdbc.queryForObject("SELECT count(*) FROM audit_log", Long.class);
        var before =
                jdbc.queryForMap(
                        "SELECT username,full_name,security_version FROM app_user WHERE id=?",
                        staffId);
        mvc.perform(
                        post("/api/admin/users/save-or-update")
                                .with(asUser(adminId, Role.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        withId(
                                                staffId,
                                                "{\"username\":\"changed-"
                                                        + suffix
                                                        + "\",\"fullName\":\"Không ghi thay đổi"
                                                        + " này\"}")))
                .andExpect(status().isBadRequest());
        Map<String, String> studentFields =
                Map.of(
                        "studentCode", "BAD" + suffix,
                        "gender", "M",
                        "dateOfBirth", "2003-01-01",
                        "faculty", "CNTT",
                        "cohort", "65");
        for (var field : studentFields.entrySet()) {
            var input = mapper.createObjectNode();
            input.put("username", "strict-" + suffix);
            input.put("fullName", "Không tạo tài khoản sai nhánh");
            input.put("role", Role.STAFF);
            input.put("managementUnitId", firstUnitId);
            input.put(field.getKey(), field.getValue());
            mvc.perform(
                            post("/api/admin/users/save-or-update")
                                    .with(asUser(adminId, Role.ADMIN))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(input)))
                    .andExpect(status().isBadRequest());
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user", Long.class))
                .isEqualTo(userCount);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_log", Long.class))
                .isEqualTo(auditCount);
        assertThat(
                        jdbc.queryForMap(
                                "SELECT username,full_name,security_version FROM app_user WHERE"
                                        + " id=?",
                                staffId))
                .isEqualTo(before);
        verifyNoInteractions(identities);
    }

    /**
     * ADMIN đọc toàn hệ thống ở API chung, nhưng /me không được trả hồ sơ/cọc/kho của người khác.
     */
    @Test
    void adminMeAliasesNeverExpandToOtherOwnersContractsAndStayRequests() throws Exception {
        String marker = "OWNER" + suffix;
        jdbc.update("UPDATE app_user SET full_name=? WHERE id=?", marker, studentId);
        Long periodId = createOpenPeriod();
        Long applicationId = createDraftApplication(periodId);
        Long contractId =
                jdbc.queryForObject(
                        "INSERT INTO"
                            + " dormitory_contract(code,student_id,application_id,start_date,end_date,status,activation_deadline)"
                            + " VALUES(?,?,?,'2040-09-01','2041-01-31','PENDING',now()) RETURNING"
                            + " id",
                        Long.class,
                        marker,
                        studentId,
                        applicationId);
        Long extensionId =
                jdbc.queryForObject(
                        "INSERT INTO"
                            + " contract_extension_request(contract_id,old_end_date,new_end_date,reason)"
                            + " VALUES(?,'2041-01-31','2041-03-01','Gia hạn kiểm thử') RETURNING"
                            + " id",
                        Long.class,
                        contractId);
        Long transferId =
                jdbc.queryForObject(
                        "INSERT INTO room_transfer_request(contract_id,reason) VALUES(?,'Chuyển"
                                + " kiểm thử') RETURNING id",
                        Long.class,
                        contractId);
        Long summerSemester =
                jdbc.queryForObject(
                        "INSERT INTO semester(academic_year,type,name,start_date,end_date)"
                                + " VALUES('2040-2041','SUMMER','Hè kiểm"
                                + " thử','2041-06-01','2041-08-31') RETURNING id",
                        Long.class);
        Long summerId =
                jdbc.queryForObject(
                        "INSERT INTO summer_choice(contract_id,semester_id,choice)"
                                + " VALUES(?,?,'STAY') RETURNING id",
                        Long.class,
                        contractId,
                        summerSemester);
        Long bedId =
                jdbc.queryForObject(
                        "INSERT INTO bed(room_id,code,status) VALUES(?,'OWNER-1','ACTIVE')"
                                + " RETURNING id",
                        Long.class,
                        firstRoomId);
        Long storageId =
                jdbc.queryForObject(
                        "INSERT INTO storage_record(contract_id,summer_choice_id,previous_bed_id,"
                                + " package_count,description,location,deposited_at,received_by)"
                                + " VALUES(?,?,?,1,'Đồ kiểm thử','Kho kiểm thử','2041-06-01',?)"
                                + " RETURNING id",
                        Long.class,
                        contractId,
                        summerId,
                        bedId,
                        staffId);
        Map<String, Long> resources =
                Map.of(
                        "contracts", contractId,
                        "extension-requests", extensionId,
                        "transfer-requests", transferId,
                        "summer-choices", summerId,
                        "deposits", contractId,
                        "storage-records", storageId);
        for (var resource : resources.entrySet()) {
            mvc.perform(
                            searchByPage("/api/" + resource.getKey(), "q", marker)
                                    .with(asUser(adminId, Role.ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(resource.getValue()));
            mvc.perform(
                            searchByPage("/api/me/" + resource.getKey(), "q", marker)
                                    .with(asUser(adminId, Role.ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.content.length()").value(0));
            mvc.perform(
                            searchByPage("/api/me/" + resource.getKey(), "q", marker)
                                    .with(asUser(studentId, Role.STUDENT)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(resource.getValue()));
        }
    }

    /** Phần tử quan hệ null trả lỗi đầu vào; cả danh sách null vẫn giữ nghĩa cập nhật từng phần. */
    @Test
    void nullRelationArrayElementsReturnBadRequestWithoutWritingRows() throws Exception {
        Long periodId = createOpenPeriod();
        Long semesterId =
                jdbc.queryForObject(
                        "SELECT semester_id FROM registration_period WHERE id=?",
                        Long.class,
                        periodId);
        Long periodsBefore =
                jdbc.queryForObject("SELECT count(*) FROM registration_period", Long.class);
        Long applicationsBefore =
                jdbc.queryForObject("SELECT count(*) FROM room_application", Long.class);
        for (String field : List.of("unitIds", "quotas")) {
            ObjectNode input = mapper.createObjectNode();
            input.put("semesterId", semesterId);
            input.put("kind", "REGULAR");
            input.put("name", "Đợt kiểm tra phần tử null");
            input.put("openAt", "2090-08-01T00:00:00+07:00");
            input.put("closeAt", "2090-08-15T00:00:00+07:00");
            input.putArray("unitIds").add(firstUnitId);
            input.putArray("quotas");
            input.putArray(field).addNull();
            mvc.perform(
                            post("/api/registration-periods/save-or-update")
                                    .with(asUser(adminId, Role.ADMIN))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(input)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        }
        ObjectNode application = mapper.createObjectNode();
        application.put("periodId", periodId);
        application.put("preferredUnitId", firstUnitId);
        application.putArray("priorityCategoryIds").addNull();
        mvc.perform(
                        post("/api/applications/save-or-update")
                                .with(asUser(studentId, Role.STUDENT))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(application)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM registration_period", Long.class))
                .isEqualTo(periodsBefore);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM room_application", Long.class))
                .isEqualTo(applicationsBefore);
    }

    /** Sửa thông báo từng phần giữ nội dung/phạm vi; ID mất không biến thành lỗi SQL 500. */
    @Test
    void notificationSaveRetainsOmittedContentAndRecipientScope() throws Exception {
        String created =
                mvc.perform(
                                post("/api/notifications/save-or-update")
                                        .with(asUser(staffId, Role.STAFF))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                mapper.writeValueAsString(
                                                        Map.of(
                                                                "scope",
                                                                "FLOOR",
                                                                "buildingId",
                                                                firstBuildingId,
                                                                "floor",
                                                                1,
                                                                "title",
                                                                "Thông báo ban đầu",
                                                                "body",
                                                                "Nội dung cần giữ"))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long id = mapper.readTree(created).get("id").asLong();
        mvc.perform(
                        post("/api/notifications/save-or-update")
                                .with(asUser(staffId, Role.STAFF))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "id",
                                                        id,
                                                        "title",
                                                        "Thông báo đã sửa",
                                                        "floor",
                                                        1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Thông báo đã sửa"))
                .andExpect(jsonPath("$.body").value("Nội dung cần giữ"))
                .andExpect(jsonPath("$.scope").value("FLOOR"))
                .andExpect(jsonPath("$.buildingId").value(firstBuildingId))
                .andExpect(jsonPath("$.floor").value(1));
        mvc.perform(
                        post("/api/notifications/save-or-update")
                                .with(asUser(staffId, Role.STAFF))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "id",
                                                        id,
                                                        "title",
                                                        "Đổi tầng không hợp lệ",
                                                        "floor",
                                                        2))))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/notifications/save-or-update")
                                .with(asUser(adminId, Role.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"id\":9223372036854775807,\"title\":\"Không tạo"
                                                + " ngầm\"}"))
                .andExpect(status().isNotFound());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM audit_log WHERE action='NOTIFICATION_CREATE'"
                                        + " AND entity_id=?",
                                Long.class,
                                id))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT title FROM notification WHERE id=?", String.class, id))
                .isEqualTo("Thông báo đã sửa");
    }

    /** Thiếu chỉ số cuối khi tạo phải báo lỗi; khi sửa không tự thay trường bị bỏ qua bằng 0. */
    @Test
    void readingSaveRequiresCreateValuesAndKeepsOmittedUpdateValues() throws Exception {
        var create = mapper.createObjectNode();
        create.put("roomId", firstRoomId);
        create.put("meterType", "ELECTRIC");
        create.put("billingMonth", "2026-10-01");
        create.put("startReading", 100);
        mvc.perform(
                        post("/api/utility-readings/save-or-update")
                                .with(asUser(staffId, Role.STAFF))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(create)))
                .andExpect(status().isBadRequest());
        create.put("endReading", 120);
        String response =
                mvc.perform(
                                post("/api/utility-readings/save-or-update")
                                        .with(asUser(staffId, Role.STAFF))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(mapper.writeValueAsString(create)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long id = mapper.readTree(response).get("id").asLong();
        mvc.perform(
                        post("/api/utility-readings/save-or-update")
                                .with(asUser(staffId, Role.STAFF))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of("id", id, "startReading", 105))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(firstRoomId))
                .andExpect(jsonPath("$.meterType").value("ELECTRIC"))
                .andExpect(jsonPath("$.endReading").value(120))
                .andExpect(jsonPath("$.startReading").value(105))
                .andExpect(jsonPath("$.consumption").value(15));
        mvc.perform(
                        post("/api/utility-readings/save-or-update")
                                .with(asUser(staffId, Role.STAFF))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of("id", id, "roomId", otherRoomId))))
                .andExpect(status().isBadRequest());
    }

    private void grantStaffPermissions(String... permissions) throws Exception {
        jdbc.update(
                "UPDATE role_permission_matrix SET permissions=?::jsonb,version=version+1 WHERE"
                        + " role=?",
                mapper.writeValueAsString(List.of(permissions)),
                Role.STAFF);
        entityManager.clear();
    }

    private String newRoomPayload(String code) throws Exception {
        return mapper.writeValueAsString(
                Map.of(
                        "buildingId",
                        firstBuildingId,
                        "roomTypeId",
                        roomTypeId,
                        "floor",
                        2,
                        "code",
                        code,
                        "gender",
                        "M"));
    }

    private Long createAuditFixture(Long actorId, String action, String type, String createdAt) {
        return jdbc.queryForObject(
                "INSERT INTO audit_log(actor_id,action,entity_type,entity_id,created_at)"
                        + " VALUES(?,?,?,?,?) RETURNING id",
                Long.class,
                actorId,
                action,
                type,
                firstRoomId,
                OffsetDateTime.parse(createdAt));
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
                                post("/api/applications/save-or-update")
                                        .with(asUser(studentId, "STUDENT"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"periodId\":"
                                                        + periodId
                                                        + ",\"preferredUnitId\":"
                                                        + firstUnitId
                                                        + "}"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return mapper.readTree(response).get("id").asLong();
    }

    /** Số bản tin chưa đọc mà API unread-count trả về cho sinh viên của fixture. */
    private long unreadNotifications() throws Exception {
        String response =
                mvc.perform(
                                get("/api/notifications/unread-count")
                                        .with(asUser(studentId, "STUDENT")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return mapper.readTree(response).get("count").asLong();
    }

    /** Gửi bộ lọc dưới dạng JSON có kiểu; dữ liệu sai vẫn để API kiểm tra và trả 400. */
    private MockHttpServletRequestBuilder searchByPage(String base, Object... filters)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("page", 0);
        body.put("size", 20);
        for (int index = 0; index < filters.length; index += 2) {
            String key = (String) filters[index];
            Object value = filters[index + 1];
            if ("sort".equals(key)) {
                value = List.of(value.toString());
            } else if (LONG_FILTERS.contains(key)) {
                value = Long.valueOf(value.toString());
            } else if ("locked".equals(key)
                    && List.of("true", "false").contains(value.toString())) {
                value = Boolean.valueOf(value.toString());
            }
            body.put(key, value);
        }
        return post(base + "/search-by-page")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body));
    }

    /** Giữ nguyên payload cập nhật từng phần; id chỉ chọn bản ghi, không cho phép tạo ngầm. */
    private MockMultipartHttpServletRequestBuilder ticketEdit(Long ticketId, String payload)
            throws Exception {
        return multipart("/api/maintenance-requests/save-or-update")
                .file(
                        new MockMultipartFile(
                                "data",
                                "data.json",
                                MediaType.APPLICATION_JSON_VALUE,
                                withId(ticketId, payload).getBytes(StandardCharsets.UTF_8)));
    }

    private String withId(Long targetId, String payload) throws Exception {
        ObjectNode body = (ObjectNode) mapper.readTree(payload);
        body.put("id", targetId);
        return mapper.writeValueAsString(body);
    }
}
