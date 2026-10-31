package vn.edu.tlu.dms.account.service.impl;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import vn.edu.tlu.dms.account.api.AccountDtos.TemporaryPassword;
import vn.edu.tlu.dms.account.api.StudentManagementController.Save;
import vn.edu.tlu.dms.account.service.StudentImportService;
import vn.edu.tlu.dms.account.service.StudentManagementService;
import vn.edu.tlu.dms.common.constants.DmsConstants;
import vn.edu.tlu.dms.common.constants.DmsConstants.ApiField;
import vn.edu.tlu.dms.common.constants.DmsConstants.AuditAction;
import vn.edu.tlu.dms.common.constants.DmsConstants.EntityType;
import vn.edu.tlu.dms.common.constants.DmsConstants.ErrorCode;
import vn.edu.tlu.dms.common.constants.DmsConstants.Permission;
import vn.edu.tlu.dms.common.constants.DmsConstants.StudentImport;
import vn.edu.tlu.dms.common.exception.BusinessException;
import vn.edu.tlu.dms.common.security.CurrentUser;
import vn.edu.tlu.dms.common.security.ScopeGuard;
import vn.edu.tlu.dms.common.service.AuditService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipInputStream;

/** Đọc/validate trước khi mở transaction; không ghi mật khẩu vào CSDL, audit hoặc log. */
@Service
public class StudentImportServiceImpl implements StudentImportService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final JdbcTemplate jdbc;
    private final StudentManagementService students;
    private final ScopeGuard guard;
    private final CurrentUser current;
    private final AuditService audit;
    private final TransactionTemplate transaction;

    public StudentImportServiceImpl(
            JdbcTemplate jdbc,
            StudentManagementService students,
            ScopeGuard guard,
            CurrentUser current,
            AuditService audit,
            PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.students = students;
        this.guard = guard;
        this.current = current;
        this.audit = audit;
        this.transaction = new TransactionTemplate(manager);
    }

    @Override
    public Result importStudents(MultipartFile file, boolean dryRun) {
        requireAdmin();
        List<ParsedRow> rows = parse(file);
        if (dryRun) {
            return summarize(rows, existingCodes(rows), true);
        }
        return transaction.execute(
                status -> {
                    // Hai lô import không cùng nhận một mã là mới. Unique index vẫn bảo vệ với CRUD
                    // đơn lẻ.
                    jdbc.execute("SELECT pg_advisory_xact_lock(hashtext('dms.student-import'))");
                    Result result = summarize(rows, existingCodes(rows), false);
                    audit.record(
                            current.get().id(),
                            AuditAction.STUDENT_IMPORT,
                            EntityType.USER,
                            null,
                            null,
                            Map.of(
                                    "totalRows",
                                    result.totalRows(),
                                    "createdRows",
                                    result.createdRows(),
                                    "existingRows",
                                    result.existingRows(),
                                    "errorRows",
                                    result.errorRows()),
                            null);
                    return result;
                });
    }

    private void requireAdmin() {
        guard.requirePermission(Permission.STUDENT_CREATE);
        if (!current.get().isAdmin()) {
            throw BusinessException.forbidden();
        }
    }

    private List<ParsedRow> parse(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (file.getSize() > StudentImport.MAX_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE);
        }
        if (file.isEmpty() || name == null || !name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw invalidFile(List.of());
        }
        // Không tin size do caller cung cấp và không đọc stream không giới hạn.
        try (var stream = file.getInputStream()) {
            byte[] bytes = stream.readNBytes((int) StudentImport.MAX_BYTES + 1);
            if (bytes.length > StudentImport.MAX_BYTES) {
                throw new BusinessException(ErrorCode.FILE_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE);
            }
            checkArchive(bytes);
            try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                if (workbook.getNumberOfSheets() == 0) {
                    throw invalidFile(List.of());
                }
                var sheet = workbook.getSheetAt(0);
                var header = sheet.getRow(0);
                if (header == null) {
                    throw invalidFile(List.of());
                }
                var formatter = new DataFormatter(Locale.ROOT);
                Map<String, Integer> columns = new HashMap<>();
                for (Cell cell : header) {
                    String title =
                            normalizeText(text(cell, formatter))
                                    .replaceFirst("^\\uFEFF+", "")
                                    .strip()
                                    .toLowerCase(Locale.ROOT);
                    if (!title.isBlank()
                            && columns.putIfAbsent(title, cell.getColumnIndex()) != null) {
                        throw invalidFile(List.of());
                    }
                }
                if (columns.size() > StudentImport.MAX_HEADER_COLUMNS) {
                    throw invalidFile(List.of());
                }
                List<String> missing =
                        StudentImport.HEADERS.subList(0, 6).stream()
                                .filter(
                                        title ->
                                                !columns.containsKey(
                                                        title.toLowerCase(Locale.ROOT)))
                                .toList();
                if (!missing.isEmpty()) {
                    throw invalidFile(missing);
                }
                List<ParsedRow> result = new ArrayList<>();
                LocalDate today = LocalDate.now(ZoneId.of(DmsConstants.APPLICATION_TIME_ZONE));
                // Excel thường giữ dòng/ô trống đã định dạng sau vùng dữ liệu. Chỉ đếm dòng có dữ
                // liệu.
                for (var row : sheet) {
                    if (row.getRowNum() == 0) {
                        continue;
                    }
                    var values = new LinkedHashMap<String, String>();
                    List<CellError> errors = new ArrayList<>();
                    for (int col = 0; col < StudentImport.FIELDS.size(); col++) {
                        String field = StudentImport.FIELDS.get(col);
                        Integer position =
                                columns.get(
                                        StudentImport.HEADERS.get(col).toLowerCase(Locale.ROOT));
                        Cell cell = position == null ? null : row.getCell(position);
                        String value =
                                ApiField.STUDENT_CODE.equals(field)
                                        ? studentCode(cell, formatter)
                                        : text(cell, formatter);
                        values.put(field, normalizeText(value));
                        if (ApiField.STUDENT_CODE.equals(field)
                                && cell != null
                                && cell.getCellType() == CellType.NUMERIC) {
                            BigDecimal numeric =
                                    BigDecimal.valueOf(cell.getNumericCellValue())
                                            .stripTrailingZeros();
                            // Không đoán lại mã đã bị Excel làm tròn hoặc bỏ phần thập phân.
                            if (numeric.scale() > 0
                                    || numeric.toBigInteger().abs().toString().length()
                                            > StudentImport.MAX_NUMERIC_CODE_DIGITS) {
                                errors.add(new CellError(field, StudentImport.INVALID));
                            }
                        }
                        if (cell != null
                                && (cell.getCellType() == CellType.FORMULA
                                        || cell.getCellType() == CellType.ERROR
                                        || cell.getCellType() == CellType.BOOLEAN)) {
                            errors.add(new CellError(field, StudentImport.INVALID));
                        }
                    }
                    if (values.values().stream().allMatch(String::isBlank) && errors.isEmpty()) {
                        continue;
                    }
                    Cell birthday =
                            row.getCell(
                                    columns.get(
                                            StudentImport.HEADERS.get(3).toLowerCase(Locale.ROOT)));
                    LocalDate date =
                            parseDate(
                                    birthday,
                                    values.get(ApiField.DATE_OF_BIRTH),
                                    today,
                                    errors,
                                    workbook.isDate1904());
                    validate(values, errors);
                    String gender =
                            StudentImport.GENDERS.get(
                                    values.get(ApiField.GENDER).toUpperCase(Locale.ROOT));
                    if (gender == null && !values.get(ApiField.GENDER).isBlank()) {
                        errors.add(new CellError(ApiField.GENDER, StudentImport.INVALID));
                    }
                    result.add(
                            new ParsedRow(
                                    row.getRowNum() + 1,
                                    new Save(
                                            null,
                                            values.get(ApiField.STUDENT_CODE),
                                            values.get(ApiField.FULL_NAME),
                                            gender,
                                            date,
                                            values.get(ApiField.FACULTY),
                                            values.get(ApiField.COHORT),
                                            values.get(ApiField.PHONE),
                                            null,
                                            null),
                                    errors));
                    if (result.size() > StudentImport.MAX_ROWS) {
                        throw invalidFile(List.of());
                    }
                }
                if (result.isEmpty()) {
                    throw invalidFile(List.of());
                }
                Map<String, Long> counts = new HashMap<>();
                result.forEach(row -> counts.merge(row.data().studentCode(), 1L, Long::sum));
                result.stream()
                        .filter(
                                row ->
                                        !row.data().studentCode().isBlank()
                                                && counts.get(row.data().studentCode()) > 1)
                        .forEach(
                                row ->
                                        row.errors()
                                                .add(
                                                        new CellError(
                                                                ApiField.STUDENT_CODE,
                                                                StudentImport.DUPLICATE_IN_FILE)));
                return result;
            }
        } catch (BusinessException error) {
            throw error;
        } catch (IOException | RuntimeException error) {
            // Không lộ nội dung file, đường dẫn hoặc exception của thư viện cho client/log.
            throw invalidFile(List.of());
        }
    }

    private static String text(Cell cell, DataFormatter formatter) {
        return cell == null || cell.getCellType() == CellType.FORMULA
                ? ""
                : formatter.formatCellValue(cell);
    }

    /**
     * Chuẩn hóa chữ tiếng Việt và khoảng trắng do sao chép, không xóa ký tự điều khiển giữa dữ
     * liệu.
     */
    private static String normalizeText(String value) {
        StringBuilder normalized = new StringBuilder();
        value.codePoints()
                .forEach(
                        code ->
                                normalized.appendCodePoint(
                                        Character.isSpaceChar(code) ? ' ' : code));
        return Normalizer.normalize(normalized, Normalizer.Form.NFC).strip();
    }

    /** Không dùng số mũ/dấu phân nhóm của ô số làm mã; giữ format đệm 0 tường minh. */
    private static String studentCode(Cell cell, DataFormatter formatter) {
        String formatted = text(cell, formatter);
        if (cell != null
                && cell.getCellType() == CellType.NUMERIC
                && !formatted.matches("[0-9]+")) {
            return BigDecimal.valueOf(cell.getNumericCellValue())
                    .stripTrailingZeros()
                    .toPlainString();
        }
        return formatted;
    }

    /** Giới hạn giải nén trước khi POI dựng object, chặn file ZIP nhỏ nhưng bung ra quá lớn. */
    private static void checkArchive(byte[] bytes) throws IOException {
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            long total = 0;
            int entries = 0;
            byte[] buffer = new byte[8192];
            while (zip.getNextEntry() != null) {
                if (++entries > StudentImport.MAX_ZIP_ENTRIES) {
                    throw invalidFile(List.of());
                }
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    total += read;
                    if (total > StudentImport.MAX_UNCOMPRESSED_BYTES) {
                        throw invalidFile(List.of());
                    }
                }
            }
            if (entries == 0) {
                throw invalidFile(List.of());
            }
        }
    }

    private static LocalDate parseDate(
            Cell cell, String value, LocalDate today, List<CellError> errors, boolean date1904) {
        if (cell == null || cell.getCellType() == CellType.BLANK || value.isBlank()) {
            errors.add(new CellError(ApiField.DATE_OF_BIRTH, StudentImport.REQUIRED));
            return null;
        }
        try {
            LocalDate date;
            if (cell.getCellType() == CellType.NUMERIC
                    && DateUtil.isCellDateFormatted(cell)
                    && DateUtil.isValidExcelDate(cell.getNumericCellValue())) {
                date =
                        DateUtil.getLocalDateTime(cell.getNumericCellValue(), date1904)
                                .toLocalDate();
            } else {
                date = LocalDate.parse(value, DATE_FORMAT);
            }
            if (date.isAfter(today.minusYears(15)) || !date.isAfter(today.minusYears(61))) {
                throw new IllegalArgumentException();
            }
            return date;
        } catch (RuntimeException error) {
            errors.add(new CellError(ApiField.DATE_OF_BIRTH, StudentImport.INVALID));
            return null;
        }
    }

    private static void validate(Map<String, String> values, List<CellError> errors) {
        values.forEach(
                (field, value) -> {
                    if (value.codePoints().anyMatch(Character::isISOControl)) {
                        errors.add(new CellError(field, StudentImport.INVALID));
                    }
                });
        for (String field : StudentImport.FIELDS.subList(0, 6)) {
            if (!field.equals(ApiField.DATE_OF_BIRTH) && values.get(field).isBlank()) {
                errors.add(new CellError(field, StudentImport.REQUIRED));
            }
        }
        checkPattern(values, errors, ApiField.STUDENT_CODE, StudentImport.CODE_PATTERN);
        checkPattern(values, errors, ApiField.COHORT, StudentImport.COHORT_PATTERN);
        checkPattern(values, errors, ApiField.PHONE, StudentImport.PHONE_PATTERN);
        if (!values.get(ApiField.FULL_NAME).isBlank()
                && (values.get(ApiField.FULL_NAME).length() < 2
                        || values.get(ApiField.FULL_NAME).length() > 100)) {
            errors.add(new CellError(ApiField.FULL_NAME, StudentImport.INVALID));
        }
        if (values.get(ApiField.FACULTY).length() > 100) {
            errors.add(new CellError(ApiField.FACULTY, StudentImport.INVALID));
        }
    }

    private static void checkPattern(
            Map<String, String> values, List<CellError> errors, String field, String pattern) {
        if (!values.get(field).isBlank() && !values.get(field).matches(pattern)) {
            errors.add(new CellError(field, StudentImport.PATTERN));
        }
    }

    private Set<String> existingCodes(List<ParsedRow> rows) {
        List<String> codes = rows.stream().map(row -> row.data().studentCode()).distinct().toList();
        String placeholders = String.join(",", Collections.nCopies(codes.size(), "?"));
        List<Object> args = new ArrayList<>(codes);
        args.addAll(codes);
        return new HashSet<>(
                jdbc.queryForList(
                        "SELECT username FROM app_user WHERE username IN ("
                                + placeholders
                                + ") UNION SELECT student_code FROM student_profile WHERE"
                                + " student_code IN ("
                                + placeholders
                                + ")",
                        String.class,
                        args.toArray()));
    }

    private Result summarize(List<ParsedRow> rows, Set<String> existing, boolean dryRun) {
        List<RowResult> details = new ArrayList<>();
        List<CreatedAccount> created = new ArrayList<>();
        int valid = 0;
        int exists = 0;
        int errors = 0;
        for (ParsedRow row : rows) {
            String code = row.data().studentCode();
            if (!row.errors().isEmpty()) {
                errors++;
                details.add(
                        new RowResult(
                                row.number(),
                                code,
                                StudentImport.ERROR,
                                List.copyOf(row.errors())));
            } else if (existing.contains(code)) {
                exists++;
                details.add(new RowResult(row.number(), code, StudentImport.EXISTS, List.of()));
            } else {
                valid++;
                if (!dryRun) {
                    TemporaryPassword account =
                            (TemporaryPassword) students.saveOrUpdate(row.data());
                    created.add(
                            new CreatedAccount(
                                    account.userId(), code, account.temporaryPassword()));
                }
            }
        }
        return new Result(
                dryRun,
                rows.size(),
                valid,
                created.size(),
                exists,
                errors,
                List.copyOf(details),
                List.copyOf(created));
    }

    @Override
    public byte[] downloadTemplate() {
        requireAdmin();
        try (var workbook = new XSSFWorkbook();
                var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(StudentImport.SHEET_NAME);
            var header = sheet.createRow(0);
            var style = workbook.createCellStyle();
            var font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            var textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
            for (int i = 0; i < StudentImport.HEADERS.size(); i++) {
                var cell = header.createCell(i);
                cell.setCellValue(StudentImport.HEADERS.get(i));
                cell.setCellStyle(style);
                sheet.setColumnWidth(i, 24 * 256);
                sheet.setDefaultColumnStyle(i, textStyle);
            }
            sheet.createFreezePane(0, 1);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException error) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static BusinessException invalidFile(List<String> missing) {
        return new BusinessException(
                ErrorCode.IMPORT_FILE_INVALID,
                HttpStatus.BAD_REQUEST,
                Map.of(
                        ApiField.MISSING_COLUMNS,
                        missing,
                        ApiField.MAX_ROWS,
                        StudentImport.MAX_ROWS));
    }

    private record ParsedRow(int number, Save data, List<CellError> errors) {}
}
