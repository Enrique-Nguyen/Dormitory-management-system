package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.tlu.dms.account.domain.AppUser;
import vn.edu.tlu.dms.account.service.IdentityAccountService;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.StudentImport;
import vn.edu.tlu.dms.common.exception.BusinessException;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** FR-ACC-05: file thật, PostgreSQL thật, phân quyền và transaction; chỉ thay máy chủ định danh. */
@SpringBootTest(properties = "dms.jobs.enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class StudentImportIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean IdentityAccountService identities;

    private String subject;
    private Long adminId;

    @BeforeEach
    void setUp() {
        subject = UUID.randomUUID().toString();
        adminId =
                jdbc.queryForObject(
                        "INSERT INTO app_user(username,full_name,role,keycloak_user_id)"
                                + " VALUES(?,'Admin import','ADMIN',?) RETURNING id",
                        Long.class,
                        "import-" + subject,
                        subject);
        doAnswer(
                        invocation -> {
                            AppUser user = invocation.getArgument(0);
                            user.setKeycloakUserId(UUID.randomUUID().toString());
                            return null;
                        })
                .when(identities)
                .provisionAccount(any(), any());
    }

    private RequestPostProcessor token(String role) {
        return jwt().jwt(
                        builder ->
                                builder.subject(subject)
                                        .claim("realm_access", Map.of("roles", List.of(role)))
                                        .claim("dms_security_version", 0))
                .authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_" + role));
    }

    private MockMultipartFile file(String[]... rows) throws Exception {
        try (var workbook = new XSSFWorkbook();
                var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Sinh viên");
            var header = sheet.createRow(0);
            for (int c = 0; c < StudentImport.HEADERS.size(); c++) {
                header.createCell(c).setCellValue(StudentImport.HEADERS.get(c));
            }
            for (int r = 0; r < rows.length; r++) {
                var row = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            workbook.write(out);
            return new MockMultipartFile(
                    "file", "students.xlsx", "application/octet-stream", out.toByteArray());
        }
    }

    private String[] valid(String code) {
        return new String[] {
            code, "Nguyễn Sinh Viên", "Nam", "20/10/2005", "CNTT", "K65", "0912345678"
        };
    }

    private ResultActions upload(MockMultipartFile file, boolean dryRun) throws Exception {
        return mvc.perform(
                multipart("/api/admin/student-imports")
                        .file(file)
                        .param("dryRun", Boolean.toString(dryRun))
                        .with(token("ADMIN")));
    }

    @Test
    void previewValidatesWithoutCreatingAccountsOrAudit() throws Exception {
        upload(file(valid("9900100001")), true)
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("validRows").value(1))
                .andExpect(jsonPath("createdRows").value(0))
                .andExpect(jsonPath("rows").isEmpty())
                .andExpect(jsonPath("createdAccounts").isEmpty());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM app_user WHERE username='9900100001'",
                                Long.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM audit_log WHERE actor_id=?",
                                Long.class,
                                adminId))
                .isZero();
        verifyNoInteractions(identities);
    }

    @Test
    void writesOnlyValidRowsSkipsExistingAndAllDuplicateOccurrences() throws Exception {
        jdbc.update(
                "INSERT INTO app_user(username,full_name,role)"
                        + " VALUES('9900100002','Existing','ADMIN')");
        String[] invalidPhone = valid("9900100003");
        invalidPhone[6] = "123";
        var source =
                file(
                        valid("9900100001"),
                        valid("9900100002"),
                        invalidPhone,
                        valid("9900100004"),
                        valid("9900100004"));
        var response =
                upload(source, false)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("totalRows").value(5))
                        .andExpect(jsonPath("createdRows").value(1))
                        .andExpect(jsonPath("existingRows").value(1))
                        .andExpect(jsonPath("errorRows").value(3))
                        .andExpect(jsonPath("rows[1].errors[0].code").value("PATTERN"))
                        .andExpect(jsonPath("rows[2].errors[0].code").value("DUPLICATE_IN_FILE"))
                        .andReturn();
        JsonNode result = mapper.readTree(response.getResponse().getContentAsString());
        String password = result.path("createdAccounts").get(0).path("temporaryPassword").asText();
        assertThat(password).hasSizeBetween(8, 64);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT keycloak_user_id FROM app_user WHERE username='9900100001'",
                                String.class))
                .isNotBlank();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT password_hash FROM app_user WHERE username='9900100001'",
                                String.class))
                .isNull();
        assertThat(
                        jdbc.queryForList(
                                        "SELECT old_value::text,new_value::text FROM audit_log"
                                                + " WHERE actor_id=?",
                                        adminId)
                                .toString())
                .doesNotContain(password, "temporaryPassword");
        upload(source, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("createdRows").value(0))
                .andExpect(jsonPath("existingRows").value(2))
                .andExpect(jsonPath("createdAccounts").isEmpty());
    }

    @Test
    void templateHasCanonicalColumnsAndNoStudentData() throws Exception {
        var response =
                mvc.perform(get("/api/admin/student-imports/template").with(token("ADMIN")))
                        .andExpect(status().isOk())
                        .andExpect(header().string("Cache-Control", "no-store"))
                        .andReturn();
        try (var workbook =
                new XSSFWorkbook(
                        new java.io.ByteArrayInputStream(
                                response.getResponse().getContentAsByteArray()))) {
            assertThat(workbook.getSheetAt(0).getLastRowNum()).isZero();
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("Mã SV");
        }
    }

    @Test
    void anonymousAndStaffCannotReadTemplateOrImport() throws Exception {
        mvc.perform(get("/api/admin/student-imports/template"))
                .andExpect(status().isUnauthorized());
        Long unit =
                jdbc.queryForObject("SELECT id FROM management_unit WHERE code='K1'", Long.class);
        jdbc.update(
                "UPDATE app_user SET role='STAFF',management_unit_id=? WHERE id=?", unit, adminId);
        mvc.perform(get("/api/admin/student-imports/template").with(token("STAFF")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        multipart("/api/admin/student-imports")
                                .file(file(valid("9900100001")))
                                .with(token("STAFF")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(identities);
    }

    @Test
    void rejectsRenamedBinaryMissingFileAndOversize() throws Exception {
        upload(
                        new MockMultipartFile(
                                "file", "fake.xlsx", "application/octet-stream", new byte[] {1, 2}),
                        true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errorCode").value("IMPORT_FILE_INVALID"));
        mvc.perform(multipart("/api/admin/student-imports").with(token("ADMIN")))
                .andExpect(status().isBadRequest());
        upload(
                        new MockMultipartFile(
                                "file",
                                "big.xlsx",
                                "application/octet-stream",
                                new byte[(int) StudentImport.MAX_BYTES + 1]),
                        true)
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("errorCode").value("FILE_TOO_LARGE"));
    }

    @Test
    void compressedArchiveCannotBypassUncompressedLimit() throws Exception {
        var out = new ByteArrayOutputStream();
        try (var zip = new java.util.zip.ZipOutputStream(out)) {
            zip.putNextEntry(new java.util.zip.ZipEntry("xl/worksheets/sheet1.xml"));
            byte[] buffer = new byte[8192];
            for (long size = 0;
                    size <= StudentImport.MAX_UNCOMPRESSED_BYTES;
                    size += buffer.length) {
                zip.write(buffer);
            }
            zip.closeEntry();
        }
        assertThat(out.size()).isLessThan((int) StudentImport.MAX_BYTES);
        upload(new MockMultipartFile("file", "compressed.xlsx", null, out.toByteArray()), true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errorCode").value("IMPORT_FILE_INVALID"));
        verifyNoInteractions(identities);
    }

    @Test
    void rejectsMissingAndDuplicateHeaders() throws Exception {
        var original = file(valid("9900100001"));
        for (boolean duplicate : List.of(false, true)) {
            try (var workbook = new XSSFWorkbook(original.getInputStream());
                    var out = new ByteArrayOutputStream()) {
                workbook.getSheetAt(0)
                        .getRow(0)
                        .getCell(0)
                        .setCellValue(duplicate ? "Họ tên" : "Mã học sinh");
                workbook.write(out);
                upload(new MockMultipartFile("file", "bad.xlsx", null, out.toByteArray()), true)
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("errorCode").value("IMPORT_FILE_INVALID"));
            }
        }
    }

    @Test
    void rejectsFormulasAndInvalidCalendarDates() throws Exception {
        String[] badDate = valid("9900100001");
        badDate[3] = "31/02/2005";
        upload(file(badDate), true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("errorRows").value(1))
                .andExpect(jsonPath("rows[0].errors[0].column").value("dateOfBirth"));
        var original = file(valid("9900100001"));
        try (var workbook = new XSSFWorkbook(original.getInputStream());
                var out = new ByteArrayOutputStream()) {
            workbook.getSheetAt(0).getRow(1).getCell(0).setCellFormula("1+1");
            workbook.write(out);
            upload(new MockMultipartFile("file", "formula.xlsx", null, out.toByteArray()), true)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("errorRows").value(1));
        }
    }

    @Test
    void acceptsReorderedTrimmedHeadersOptionalPhoneAndNativeDateCells() throws Exception {
        try (var workbook = new XSSFWorkbook();
                var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Import");
            var header = sheet.createRow(0);
            var row = sheet.createRow(1);
            String[] source = valid("0000100001");
            for (int c = 0; c < 6; c++) {
                header.createCell(c)
                        .setCellValue(
                                " "
                                        + StudentImport.HEADERS
                                                .get(5 - c)
                                                .toUpperCase(java.util.Locale.ROOT)
                                        + " ");
                row.createCell(c).setCellValue(source[5 - c]);
            }
            row.getCell(2).setCellValue(LocalDate.of(2005, 10, 20));
            var style = workbook.createCellStyle();
            style.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));
            row.getCell(2).setCellStyle(style);
            workbook.write(out);
            upload(new MockMultipartFile("file", "reorder.xlsx", null, out.toByteArray()), false)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("createdRows").value(1))
                    .andExpect(jsonPath("createdAccounts[0].username").value("0000100001"));
        }
    }

    @Test
    void rejectsMoreThanFiveThousandRowsBeforeAnyProvisioning() throws Exception {
        var original = file(valid("9900100001"));
        try (var workbook = new XSSFWorkbook(original.getInputStream());
                var out = new ByteArrayOutputStream()) {
            for (int index = 2; index <= 5001; index++) {
                workbook.getSheetAt(0).createRow(index).createCell(0).setCellValue("9900199999");
            }
            workbook.write(out);
            upload(new MockMultipartFile("file", "too-many.xlsx", null, out.toByteArray()), false)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("errorCode").value("IMPORT_FILE_INVALID"));
            verifyNoInteractions(identities);
        }
    }

    @Test
    void ignoresFormattedEmptyRowsBeyondTheDataLimitAndKeepsExcelRowNumbers() throws Exception {
        var original = file(valid("9900100001"));
        try (var workbook = new XSSFWorkbook(original.getInputStream());
                var out = new ByteArrayOutputStream()) {
            var sheet = workbook.getSheetAt(0);
            var empty = sheet.createRow(10000).createCell(0);
            var style = workbook.createCellStyle();
            style.setDataFormat(workbook.createDataFormat().getFormat("@"));
            empty.setCellStyle(style);
            sheet.getRow(0).createCell(1000).setCellStyle(style);
            sheet.createRow(10001).createCell(0).setCellValue("BAD");
            workbook.write(out);
            upload(new MockMultipartFile("file", "formatted.xlsx", null, out.toByteArray()), true)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("totalRows").value(2))
                    .andExpect(jsonPath("validRows").value(1))
                    .andExpect(jsonPath("errorRows").value(1))
                    .andExpect(jsonPath("rows[0].rowNumber").value(10002));
        }
    }

    @Test
    void acceptsUnicodeSpacesAndDecomposedVietnameseFromCopiedExcelHeaders() throws Exception {
        var original = file(valid("9900100001"));
        try (var workbook = new XSSFWorkbook(original.getInputStream());
                var out = new ByteArrayOutputStream()) {
            var sheet = workbook.getSheetAt(0);
            for (var cell : sheet.getRow(0)) {
                cell.setCellValue(
                        java.text.Normalizer.normalize(
                                "\uFEFF"
                                        + cell.getStringCellValue().replace(' ', '\u00A0')
                                        + "\u00A0",
                                java.text.Normalizer.Form.NFD));
            }
            sheet.getRow(1)
                    .getCell(2)
                    .setCellValue(
                            java.text.Normalizer.normalize("Nữ", java.text.Normalizer.Form.NFD));
            workbook.write(out);
            upload(new MockMultipartFile("file", "copied.xlsx", null, out.toByteArray()), false)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("createdRows").value(1));
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT gender FROM student_profile WHERE"
                                        + " student_code='9900100001'",
                                    String.class))
                    .isEqualTo("F");
        }
    }

    @Test
    void acceptsExactlyFiveThousandNonemptyRowsWithoutCountingStyledRows() throws Exception {
        try (var workbook = new XSSFWorkbook();
                var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Sinh viên");
            var header = sheet.createRow(0);
            for (int column = 0; column < StudentImport.HEADERS.size(); column++) {
                header.createCell(column).setCellValue(StudentImport.HEADERS.get(column));
            }
            for (int index = 1; index <= StudentImport.MAX_ROWS; index++) {
                var row = sheet.createRow(index);
                String[] values =
                        valid("990011" + String.format(java.util.Locale.ROOT, "%04d", index));
                for (int column = 0; column < values.length; column++) {
                    row.createCell(column).setCellValue(values[column]);
                }
            }
            sheet.createRow(10000).createCell(0).setBlank();
            workbook.write(out);
            upload(new MockMultipartFile("file", "full.xlsx", null, out.toByteArray()), true)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("totalRows").value(5000))
                    .andExpect(jsonPath("validRows").value(5000))
                    .andExpect(jsonPath("createdRows").value(0));
            verifyNoInteractions(identities);
        }
    }

    @Test
    void duplicateHeadersAfterUnicodeNormalizationAreStillRejected() throws Exception {
        var original = file(valid("9900100001"));
        try (var workbook = new XSSFWorkbook(original.getInputStream());
                var out = new ByteArrayOutputStream()) {
            workbook.getSheetAt(0).getRow(0).createCell(7).setCellValue("Ma\u0303\u00A0SV");
            workbook.write(out);
            upload(new MockMultipartFile("file", "duplicate.xlsx", null, out.toByteArray()), false)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("errorCode").value("IMPORT_FILE_INVALID"));
            verifyNoInteractions(identities);
        }
    }

    @Test
    @Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void identityFailureRollsBackEveryValidStudentAndItsAudit() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        doAnswer(
                        invocation -> {
                            if (calls.incrementAndGet() == 2) {
                                throw new BusinessException(
                                        ErrorCode.IDENTITY_UNAVAILABLE,
                                        HttpStatus.SERVICE_UNAVAILABLE);
                            }
                            ((AppUser) invocation.getArgument(0))
                                    .setKeycloakUserId(UUID.randomUUID().toString());
                            return null;
                        })
                .when(identities)
                .provisionAccount(any(), any());
        upload(file(valid("9900190001"), valid("9900190002")), false)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("errorCode").value("IDENTITY_UNAVAILABLE"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM app_user WHERE username IN"
                                        + " ('9900190001','9900190002')",
                                Long.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM audit_log WHERE actor_id=?",
                                Long.class,
                                adminId))
                .isZero();
    }
}
