package vn.edu.tlu.dms.common.constants;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mã dùng chung của DMS. Giá trị chuỗi là hợp đồng với CSDL, Keycloak và frontend; đổi tên biến
 * Java được nhưng không tự ý đổi giá trị đã lưu hoặc đã công bố qua API. Cấu hình môi trường (URL,
 * mật khẩu, secret) vẫn đọc từ properties hoặc biến môi trường.
 */
public final class DmsConstants {

    public static final String PERMISSION_SEPARATOR = "_";

    /** Múi giờ nghiệp vụ dùng thống nhất khi JVM kết nối CSDL và xử lý ngày giờ. */
    public static final String APPLICATION_TIME_ZONE = "Asia/Ho_Chi_Minh";

    private DmsConstants() {}

    /** Mã lỗi công khai của API; gom mã không thay đổi giá trị đã dùng ở frontend/CSDL. */
    public static final class ErrorCode {
        public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
        public static final String ADMIN_PERMISSIONS_IMMUTABLE = "ADMIN_PERMISSIONS_IMMUTABLE";
        public static final String APPLICATION_EXISTS = "APPLICATION_EXISTS";
        public static final String APPLICATION_HAS_CONTRACT = "APPLICATION_HAS_CONTRACT";
        public static final String BED_CODE_EXISTS = "BED_CODE_EXISTS";
        public static final String BED_NOT_AVAILABLE = "BED_NOT_AVAILABLE";
        public static final String BED_OCCUPIED = "BED_OCCUPIED";
        public static final String BED_TAKEN = "BED_TAKEN";
        public static final String BOOTSTRAP_USERNAME_IN_USE = "BOOTSTRAP_USERNAME_IN_USE";
        public static final String CONCURRENT_UPDATE = "CONCURRENT_UPDATE";
        public static final String CONFIG_VALUE_INVALID = "CONFIG_VALUE_INVALID";
        public static final String CONTRACT_ACTIVATION_EXPIRED = "CONTRACT_ACTIVATION_EXPIRED";
        public static final String CONTRACT_AUTO_CANCELLED = "CONTRACT_AUTO_CANCELLED";
        public static final String CONTRACT_EXISTS = "CONTRACT_EXISTS";
        public static final String CONTRACT_NOT_ACTIVE = "CONTRACT_NOT_ACTIVE";
        public static final String CONTRACT_PERIOD_OVERLAP = "CONTRACT_PERIOD_OVERLAP";
        public static final String COMMON_AREA_NO_CHARGE = "COMMON_AREA_NO_CHARGE";
        public static final String COST_INVALID = "COST_INVALID";
        public static final String CHARGED_STUDENTS_INVALID = "CHARGED_STUDENTS_INVALID";
        public static final String CROSS_UNIT_ONLY_STAFF = "CROSS_UNIT_ONLY_STAFF";
        public static final String DEPOSIT_AMOUNT_MISMATCH = "DEPOSIT_AMOUNT_MISMATCH";
        public static final String DEPOSIT_INVALID = "DEPOSIT_INVALID";
        public static final String DUPLICATE_DEDUCTION = "DUPLICATE_DEDUCTION";
        public static final String DUPLICATE_DEPOSIT = "DUPLICATE_DEPOSIT";
        public static final String DUPLICATE_INVOICE = "DUPLICATE_INVOICE";
        public static final String DUPLICATE_REFUND = "DUPLICATE_REFUND";
        public static final String DUPLICATE_USERNAME = "DUPLICATE_USERNAME";
        public static final String DUPLICATE_VALUE = "DUPLICATE_VALUE";
        public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
        public static final String FORBIDDEN = "FORBIDDEN";
        public static final String GENDER_MISMATCH = "GENDER_MISMATCH";
        public static final String IDENTITY_ALREADY_LINKED = "IDENTITY_ALREADY_LINKED";
        public static final String IDENTITY_NOT_CONFIGURED = "IDENTITY_NOT_CONFIGURED";
        public static final String IDENTITY_NOT_LINKED = "IDENTITY_NOT_LINKED";
        public static final String IDENTITY_UNAVAILABLE = "IDENTITY_UNAVAILABLE";
        public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
        public static final String IMPORT_FILE_INVALID = "IMPORT_FILE_INVALID";
        public static final String INVALID_STATE_TRANSITION = "INVALID_STATE_TRANSITION";
        public static final String INVOICE_ALREADY_PAID = "INVOICE_ALREADY_PAID";
        public static final String LEADER_ALREADY_ASSIGNED = "LEADER_ALREADY_ASSIGNED";
        public static final String LEADER_NOT_STAYING = "LEADER_NOT_STAYING";
        public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
        public static final String NO_HOLD_ASSIGNMENT = "NO_HOLD_ASSIGNMENT";
        public static final String APPLICATION_NOT_APPROVED = "APPLICATION_NOT_APPROVED";
        public static final String MOVE_OUT_WARNINGS_NOT_ACKNOWLEDGED =
                "MOVE_OUT_WARNINGS_NOT_ACKNOWLEDGED";
        public static final String NO_OCCUPANT_TO_CHARGE = "NO_OCCUPANT_TO_CHARGE";
        public static final String NOT_FOUND = "NOT_FOUND";
        public static final String NOT_STAYING = "NOT_STAYING";
        public static final String PERIOD_NOT_OPEN = "PERIOD_NOT_OPEN";
        public static final String PERMISSION_MATRIX_STALE = "PERMISSION_MATRIX_STALE";
        public static final String PERMISSION_MATRIX_UNAVAILABLE = "PERMISSION_MATRIX_UNAVAILABLE";
        public static final String PHOTO_INVALID = "PHOTO_INVALID";
        public static final String PHOTO_UPLOAD_FAILED = "PHOTO_UPLOAD_FAILED";
        public static final String RATE_ALREADY_EFFECTIVE = "RATE_ALREADY_EFFECTIVE";
        public static final String RATE_EXISTS = "RATE_EXISTS";
        public static final String READING_HAS_FOLLOWING_MONTH = "READING_HAS_FOLLOWING_MONTH";
        public static final String READING_INVALID = "READING_INVALID";
        public static final String READING_LOCKED = "READING_LOCKED";
        public static final String REFUND_EXCEEDS_BALANCE = "REFUND_EXCEEDS_BALANCE";
        public static final String NO_TOPUP_REQUIRED = "NO_TOPUP_REQUIRED";
        public static final String REQUEST_PENDING_EXISTS = "REQUEST_PENDING_EXISTS";
        public static final String RESOURCE_IN_USE = "RESOURCE_IN_USE";
        public static final String ROOM_CODE_EXISTS = "ROOM_CODE_EXISTS";
        public static final String ROOM_OCCUPIED = "ROOM_OCCUPIED";
        public static final String SAME_BED = "SAME_BED";
        public static final String SEMESTER_OVERLAP = "SEMESTER_OVERLAP";
        public static final String SESSION_REVOKED = "SESSION_REVOKED";
        public static final String STORAGE_RECORD_EXISTS = "STORAGE_RECORD_EXISTS";
        public static final String STUDENT_ALREADY_ASSIGNED = "STUDENT_ALREADY_ASSIGNED";
        public static final String STUDENT_CODE_EXISTS = "STUDENT_CODE_EXISTS";
        public static final String STUDENT_HAS_ACTIVE_CONTRACT = "STUDENT_HAS_ACTIVE_CONTRACT";
        public static final String SUMMER_CHOICE_EXISTS = "SUMMER_CHOICE_EXISTS";
        public static final String SUMMER_CHOICE_NOT_APPROVED = "SUMMER_CHOICE_NOT_APPROVED";
        public static final String TECHNICIAN_OUT_OF_UNIT = "TECHNICIAN_OUT_OF_UNIT";
        public static final String TEMP_LEAVE_NOT_EXTENDED = "TEMP_LEAVE_NOT_EXTENDED";
        public static final String TICKET_ALREADY_CONCLUDED = "TICKET_ALREADY_CONCLUDED";
        public static final String TICKET_LOCATION_INVALID = "TICKET_LOCATION_INVALID";
        public static final String TOPUP_EXCEEDS_REQUIRED = "TOPUP_EXCEEDS_REQUIRED";
        public static final String TRANSFER_CROSS_UNIT = "TRANSFER_CROSS_UNIT";
        public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
        public static final String UNIT_NOT_IN_PERIOD = "UNIT_NOT_IN_PERIOD";
        public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
        public static final String USERNAME_EXISTS = "USERNAME_EXISTS";
        public static final String VALIDATION_FAILED = "VALIDATION_FAILED";

        private ErrorCode() {}
    }

    /** Tên field của hợp đồng validation, audit và ma trận quyền; không dùng làm tên cột SQL. */
    public static final class ApiField {
        public static final String STUDENT_CODE = "studentCode";
        public static final String FULL_NAME = "fullName";
        public static final String GENDER = "gender";
        public static final String DATE_OF_BIRTH = "dateOfBirth";
        public static final String FACULTY = "faculty";
        public static final String COHORT = "cohort";
        public static final String PHONE = "phone";
        public static final String ID = "id";
        public static final String FORM = "form";
        public static final String SORT = "sort";
        public static final String PAGE = "page";
        public static final String SIZE = "size";
        public static final String QUERY = "q";
        public static final String FIELD_ERRORS = "fieldErrors";
        public static final String FIELD = "field";
        public static final String MESSAGE = "message";
        public static final String ROLE = "role";
        public static final String VERSION = "version";
        public static final String PERMISSIONS = "permissions";
        public static final String REASON = "reason";
        public static final String END_REASON = "endReason";
        public static final String TARGET_BED_ID = "targetBedId";
        public static final String BED_ID = "bedId";
        public static final String BED_CODE = "bedCode";
        public static final String ROOM_CODE = "roomCode";
        public static final String ROOM_GENDER = "roomGender";
        public static final String COUNT = "count";
        public static final String CURRENT_STATUS = "currentStatus";

        public static final String SEMESTER_ID = "semesterId";
        public static final String BILLING_MONTH = "billingMonth";
        public static final String PRICE = "price";
        public static final String PAYER_NAME = "payerName";
        public static final String AMOUNT = "amount";
        public static final String MAINTENANCE_REQUEST_ID = "maintenanceRequestId";
        public static final String DUE_DATE = "dueDate";
        public static final String TYPE = "type";
        public static final String STATUS = "status";
        public static final String CONTRACT_ID = "contractId";
        public static final String CAUSE = "cause";
        public static final String REPAIR_COST = "repairCost";
        public static final String CHARGED_STUDENT_IDS = "chargedStudentIds";
        public static final String ASSET_CONDITION = "assetCondition";
        public static final String ASSET_NOTE = "assetNote";
        public static final String MOVE_OUT_DATE = "moveOutDate";
        public static final String ACKNOWLEDGED_WARNINGS = "acknowledgedWarnings";
        public static final String PACKAGE_COUNT = "packageCount";
        public static final String DESCRIPTION = "description";
        public static final String LOCATION = "location";
        public static final String DEPOSITED_AT = "depositedAt";
        public static final String RETURNED_AT = "returnedAt";
        public static final String PAPER_CONTRACT_SIGNED = "paperContractSigned";
        public static final String DEPOSIT_AMOUNT = "depositAmount";
        public static final String PAYMENT_METHOD = "paymentMethod";
        public static final String VERIFIED = "verified";
        public static final String NEW_END_DATE = "newEndDate";
        public static final String OLD_END_DATE = "oldEndDate";
        public static final String START_DATE = "startDate";
        public static final String END_DATE = "endDate";
        public static final String STUDENT_ID = "studentId";
        public static final String CONTRACT_CODE = "contractCode";
        public static final String CHOICE = "choice";
        public static final String DECIDED_BY = "decidedBy";
        public static final String STUDENT_NAME = "studentName";
        public static final String SEMESTER_NAME = "semesterName";
        public static final String CONTRACT_STATUS = "contractStatus";
        public static final String CONTRACT_START_DATE = "contractStartDate";
        public static final String CONTRACT_END_DATE = "contractEndDate";
        public static final String DECISION_REASON = "decisionReason";
        public static final String PRIORITY_SCORE = "priorityScore";
        public static final String ACTIVATED_AT = "activatedAt";
        public static final String APPLICATION_ID = "applicationId";
        public static final String NOTE = "note";

        public static final String MANAGEMENT_UNIT_ID = "managementUnitId";
        public static final String USERNAME = "username";
        public static final String HOMETOWN = "hometown";
        public static final String INPUT = "input";
        public static final String TOTAL_AMOUNT = "totalAmount";
        public static final String RATE = "rate";
        public static final String METER_TYPE = "meterType";
        public static final String UNIT_PRICE = "unitPrice";
        public static final String EFFECTIVE_FROM = "effectiveFrom";
        public static final String ROOM_ID = "roomId";
        public static final String START_READING = "startReading";
        public static final String OLD_METER_FINAL = "oldMeterFinal";
        public static final String END_READING = "endReading";
        public static final String ACTOR_ID = "actorId";
        public static final String TO = "to";
        public static final String KEY = "key";
        public static final String PHOTOS = "photos";
        public static final String ACTION = "action";
        public static final String SCOPE = "scope";
        public static final String CLOSE_AT = "closeAt";
        public static final String PERIOD = "period";
        public static final String NAME = "name";
        public static final String UNIT_IDS = "unitIds";
        public static final String QUOTAS = "quotas";
        public static final String PERIOD_ID = "periodId";
        public static final String PRIORITY_CATEGORY_IDS = "priorityCategoryIds";
        public static final String BUILDING_ID = "buildingId";
        public static final String ACADEMIC_YEAR = "academicYear";
        public static final String MISSING_COLUMNS = "missingColumns";
        public static final String MAX_ROWS = "maxRows";
        private ApiField() {}
    }

    /** Hợp đồng file import sinh viên; giới hạn kỹ thuật, không phải tham số nghiệp vụ. */
    public static final class StudentImport {
        public static final long MAX_BYTES = 5L * 1024 * 1024;
        public static final int MAX_ROWS = 5000;
        public static final long MAX_UNCOMPRESSED_BYTES = 50L * 1024 * 1024;
        public static final int MAX_ZIP_ENTRIES = 200;
        public static final String FILE_NAME = "mau-import-sinh-vien.xlsx";
        public static final String SHEET_NAME = "Sinh viên";
        public static final String REQUIRED = "REQUIRED";
        public static final String PATTERN = "PATTERN";
        public static final String DUPLICATE_IN_FILE = "DUPLICATE_IN_FILE";
        public static final String INVALID = "INVALID";
        public static final String ERROR = "ERROR";
        public static final String EXISTS = "EXISTS";
        public static final List<String> FIELDS =
                List.of(
                        ApiField.STUDENT_CODE,
                        ApiField.FULL_NAME,
                        ApiField.GENDER,
                        ApiField.DATE_OF_BIRTH,
                        ApiField.FACULTY,
                        ApiField.COHORT,
                        ApiField.PHONE);
        public static final List<String> HEADERS =
                List.of(
                        "Mã SV",
                        "Họ tên",
                        "Giới tính",
                        "Ngày sinh",
                        "Khoa",
                        "Khóa",
                        "Số điện thoại");
        public static final String CODE_PATTERN = "[0-9]{6,20}";
        public static final String PHONE_PATTERN = "0[0-9]{9}";
        public static final String COHORT_PATTERN = "K[0-9]{1,9}";
        public static final Map<String, String> GENDERS =
                Map.of("NAM", Gender.M, "M", Gender.M, "NỮ", Gender.F, "F", Gender.F);

        private StudentImport() {}
    }

    /** Giới hạn ảnh sự cố (SAD: tối đa 3 ảnh, mỗi ảnh ≤ 5 MB) và mã lý do trong details của PHOTO_INVALID. */
    public static final class PhotoUpload {
        public static final int MAX_COUNT = 3;
        public static final long MAX_BYTES = 5L * 1024 * 1024;
        public static final String REASON_COUNT = "COUNT";
        public static final String REASON_SIZE = "SIZE";
        public static final String REASON_TYPE = "TYPE";

        private PhotoUpload() {}
    }

    /** Thông báo validation dùng chung; giữ nguyên từng chuỗi đã trả về qua API. */
    public static final class ValidationMessage {
        public static final String ID_POSITIVE = "ID phải lớn hơn 0";
        public static final String ID_POSITIVE_INTEGER = "ID phải là số nguyên dương";
        public static final String REQUIRED = "Trường bắt buộc";
        public static final String NOTHING_TO_UPDATE = "Chưa có trường để cập nhật";
        public static final String NO_CONTENT_TO_UPDATE = "Chưa có nội dung để cập nhật";
        public static final String SORT_FIELD_INVALID = "Trường sắp xếp không hợp lệ";
        public static final String SORT_DIRECTION_INVALID = "Chỉ dùng asc hoặc desc";
        public static final String SORT_REQUIRED = "Trường sắp xếp không được rỗng";
        public static final String PAGE_RANGE = "page >= 0, size 1–100";

        public static final String INVOICE_CURRENT_SEMESTER =
                "Chỉ phát hành phí phòng trong học kỳ hiện tại";
        public static final String ASSIGNMENT_PRICE_REQUIRED =
                "Phân phòng chưa có đơn giá học kỳ hợp lệ";
        public static final String BILLING_MONTH_START = "Kỳ điện nước phải là ngày đầu tháng";
        public static final String ELECTRIC_RATE_REQUIRED =
                "Chưa có đơn giá điện có hiệu lực cho kỳ này";
        public static final String WATER_RATE_REQUIRED =
                "Chưa có đơn giá nước có hiệu lực cho kỳ này";
        public static final String PAYER_REQUIRED = "Cần người nộp hoặc họ tên người nộp";
        public static final String DEPOSIT_AMOUNT_POSITIVE = "Số tiền cọc phải lớn hơn 0";
        public static final String DEDUCTION_AMOUNT_POSITIVE = "Số tiền trừ cọc phải lớn hơn 0";
        public static final String TOPUP_AMOUNT_POSITIVE = "Số tiền bù cọc phải lớn hơn 0";
        public static final String REFUND_AMOUNT_POSITIVE = "Số tiền hoàn cọc phải lớn hơn 0";
        public static final String REFUND_FULL_BALANCE_REQUIRED = "Phải hoàn toàn bộ số dư cọc còn lại";
        public static final String TOPUP_DUE_DATE_REQUIRED = "Hạn nộp bù cọc không được để trống";
        public static final String MAINTENANCE_REQUEST_REQUIRED =
                "Mã ticket sửa chữa không được để trống";
        public static final String INVOICE_TYPE_INVALID = "Loại hóa đơn không hợp lệ";
        public static final String INVOICE_STATUS_INVALID = "Trạng thái hóa đơn không hợp lệ";
        public static final String CAUSE_INVALID = "Nguyên nhân phải là WEAR hoặc MISUSE";
        public static final String REPAIR_COST_POSITIVE = "Chi phí sửa chữa phải lớn hơn 0";
        public static final String WEAR_NOTE_REQUIRED = "Hao mòn tự nhiên cần ghi chú";
        public static final String CHARGED_STUDENT_REQUIRED =
                "Phải chọn ít nhất một sinh viên chịu trách nhiệm";
        public static final String CHARGED_NOTE_REQUIRED =
                "Bỏ bớt sinh viên chịu trách nhiệm cần ghi lý do";
        public static final String ASSET_CONDITION_INVALID = "Tình trạng tài sản phải là OK hoặc DAMAGED";
        public static final String ASSET_NOTE_REQUIRED = "Tài sản hư hỏng cần ghi chú";
        public static final String MOVE_OUT_DATE_REQUIRED = "Ngày trả phòng không được để trống";
        public static final String PACKAGE_COUNT_POSITIVE = "Số kiện phải lớn hơn 0";
        public static final String STORAGE_DESCRIPTION_REQUIRED = "Mô tả đồ gửi không được để trống";
        public static final String STORAGE_LOCATION_REQUIRED = "Vị trí kho không được để trống";
        public static final String DEPOSITED_DATE_REQUIRED = "Ngày gửi đồ không được để trống";
        public static final String RETURNED_DATE_REQUIRED = "Ngày nhận lại đồ không được để trống";
        public static final String BED_REQUIRED = "Cần chọn giường nhận lại đồ";
        public static final String PAPER_CONTRACT_REQUIRED =
                "Cần xác nhận đã ký giấy hợp đồng";
        public static final String DEPOSIT_AMOUNT_REQUIRED = "Số tiền cọc không được để trống";
        public static final String DEPOSIT_AMOUNT_MISMATCH =
                "Số tiền cọc không đúng với mức cọc của tòa";
        public static final String PAYMENT_METHOD_INVALID = "Hình thức thanh toán không hợp lệ";
        public static final String CONTRACT_ACTIVATION_EXPIRED =
                "Hợp đồng đã quá hạn kích hoạt";
        public static final String CONTRACT_PERIOD_OVERLAP =
                "Thời gian gia hạn chồng với hợp đồng khác của sinh viên";
        public static final String BED_REQUIRED_FOR_ASSIGN = "Cần chọn giường để phân";
        public static final String PRIORITY_VERIFIED_REQUIRED = "Cần xác nhận đúng hoặc không đúng";
        public static final String END_DATE_AFTER_START = "Ngày kết thúc phải sau ngày bắt đầu";
        public static final String EXTENSION_DATE_AFTER_CURRENT =
                "Ngày gia hạn phải sau ngày kết thúc hiện tại";
        public static final String REJECT_REASON_REQUIRED = "Cần lý do từ chối";
        public static final String RELEASE_HOLD_REASON_REQUIRED =
                "Cần lý do để hủy giữ chỗ (tối thiểu 5 ký tự)";
        public static final String STUDENT_IMMUTABLE = "Không đổi sinh viên của đơn đã tạo";
        public static final String STUDENT_REQUIRED_FOR_ADMIN =
                "ADMIN cần chọn sinh viên cần tạo đơn";
        public static final String STUDENT_ACCOUNT_REQUIRED =
                "Tài khoản được chọn phải là sinh viên";
        public static final String SUMMER_SEMESTER_REQUIRED = "Chọn học kỳ hè";
        public static final String SUMMER_CHOICE_EDIT = "Chỉ sửa lựa chọn";
        public static final String SUMMER_CHOICE_INVALID = "Lựa chọn không hợp lệ";
        public static final String RECORD_INVALID = "Chọn bản ghi hợp lệ";
        public static final String PENDING_TYPE_INVALID = "Loại yêu cầu không hợp lệ";
        public static final String SUMMER_SEMESTER_AFTER_CONTRACT =
                "Học kỳ hè phải kết thúc sau ngày hết hạn hợp đồng";

        public static final String NOT_BLANK = "Không để trống";
        public static final String UNIT_ID_POSITIVE = "ID đơn vị phải lớn hơn 0";
        public static final String USERNAME_IMMUTABLE =
                "Không được cập nhật tên đăng nhập qua API tài khoản";
        public static final String ADMIN_NO_UNIT = "Admin không gắn đơn vị";
        public static final String STUDENT_ROLE_FIXED = "Hồ sơ sinh viên giữ vai trò STUDENT";
        public static final String ROLE_IN_USE_IMMUTABLE =
                "Không đổi vai trò tài khoản đang sử dụng";
        public static final String UNIT_REQUIRED_FOR_ROLE = "Staff/KTV phải có đơn vị quản lý";
        public static final String ROLE_HAS_NO_UNIT = "Vai trò không có đơn vị";
        public static final String STUDENT_ONLY = "Chỉ áp dụng cho sinh viên";
        public static final String SELF_LOCK_FORBIDDEN = "Không tự khóa tài khoản đang dùng";
        public static final String STUDENT_FIELD_NOT_FOR_STAFF =
                "Trường hồ sơ sinh viên không áp dụng khi tạo tài khoản cán bộ";
        public static final String INVOICE_INPUT_REQUIRED = "Dữ liệu hóa đơn không được để trống";
        public static final String INVOICE_EDIT_UNISSUED_ONLY =
                "Chỉ sửa tổng tiền/hạn đóng của hồ sơ chưa phát hành";
        public static final String UPDATE_INPUT_REQUIRED = "Thiếu thông tin cập nhật";
        public static final String INVOICE_TYPE_CHOICE = "Chọn ROOM_FEE hoặc UTILITY";
        public static final String ROOM_FEE_TARGET = "Phí phòng chỉ gắn sinh viên/học kỳ";
        public static final String UTILITY_TARGET = "Điện nước chỉ gắn phòng/tháng";
        public static final String FIRST_DAY_OF_MONTH = "Dùng ngày đầu tháng";
        public static final String FIRST_DAY_OF_MONTH_VALID = "Nhập ngày đầu tháng hợp lệ";
        public static final String AMOUNT_NOT_NEGATIVE = "Số tiền không được âm";
        public static final String DUE_DATE_NOT_PAST = "Hạn đóng không được trước hôm nay";
        public static final String DUE_DATE_FROM_TODAY = "Hạn đóng phải từ hôm nay";
        public static final String NUMBER_REQUIRED = "Giá trị số bắt buộc";
        public static final String METER_TYPE_INVALID = "Loại đồng hồ không hợp lệ";
        public static final String RATE_INPUT_REQUIRED = "Thông tin đơn giá không được để trống";
        public static final String METER_TYPE_REQUIRED = "Phải chọn loại đồng hồ hợp lệ";
        public static final String UNIT_PRICE_POSITIVE = "Đơn giá phải lớn hơn 0";
        public static final String EFFECTIVE_FROM_REQUIRED = "Phải nhập ngày hiệu lực";
        public static final String READING_INPUT_REQUIRED = "Phải nhập thông tin chỉ số";
        public static final String READING_KEY_IMMUTABLE =
                "Không đổi phòng, loại đồng hồ hoặc kỳ của bản ghi";
        public static final String START_READING_REQUIRED = "Tháng đầu phải nhập chỉ số đầu";
        public static final String OLD_METER_FINAL_REQUIRED = "Thiếu chỉ số chốt đồng hồ cũ";
        public static final String ROOM_REQUIRED = "Phải chọn phòng hợp lệ";
        public static final String READING_MONTH_REQUIRED = "Phải chọn kỳ ghi chỉ số";
        public static final String END_READING_REQUIRED = "Phải nhập chỉ số cuối";
        public static final String READING_NOT_NEGATIVE = "Chỉ số không được âm";
        public static final String FIELD_NOT_WRITABLE = "Trường không được phép ghi";
        public static final String VALUE_FORMAT_INVALID = "Giá trị không đúng định dạng";
        public static final String TEXT_LENGTH_500 = "Nhập từ 1 đến 500 ký tự";
        public static final String ACTOR_ID_POSITIVE = "ID người thực hiện phải lớn hơn 0";
        public static final String TIME_RANGE_INVALID =
                "Mốc kết thúc phải bằng hoặc sau mốc bắt đầu";
        public static final String CONFIG_KEY_EXISTING = "Phải chọn khóa cấu hình đã tồn tại";
        public static final String CONTRACT_APPLICATION_IMMUTABLE =
                "Không đổi nguồn đơn của hợp đồng";
        public static final String DATE_FORMAT_ISO = "Ngày dùng yyyy-MM-dd";
        public static final String TRANSFER_NO_END_DATE =
                "Yêu cầu chuyển phòng không nhận ngày gia hạn";
        public static final String CONTRACT_IMMUTABLE = "Không đổi hợp đồng";
        public static final String DATE_INVALID = "Ngày không hợp lệ";
        public static final String PHOTO_UPDATE_UNSUPPORTED =
                "Không hỗ trợ cập nhật ảnh khi sửa yêu cầu";
        public static final String MAINTENANCE_LOCATION_IMMUTABLE =
                "Không đổi vị trí của yêu cầu đã tạo";
        public static final String REASON_MIN_5 = "Cần lý do hoặc ghi chú tối thiểu 5 ký tự";
        public static final String PROXY_ACCEPT_REASON_REQUIRED =
                "Nghiệm thu thay sinh viên cần lý do";
        public static final String ACTION_UNSUPPORTED = "Hành động không hỗ trợ";
        public static final String NOTIFICATION_SCOPE_IMMUTABLE =
                "Không đổi phạm vi thông báo đã đăng";
        public static final String NOTIFICATION_SCOPE_MISMATCH =
                "Các trường vị trí không khớp phạm vi thông báo";
        public static final String SCOPE_UNSUPPORTED = "Phạm vi không hỗ trợ";
        public static final String PERIOD_EXTEND_CLOSE_ONLY = "Chỉ được gia hạn ngày đóng";
        public static final String PERIOD_INPUT_REQUIRED = "Thiếu trường bắt buộc của đợt đăng ký";
        public static final String NAME_REQUIRED = "Không để trống tên";
        public static final String CLOSE_AFTER_OPEN = "Ngày đóng phải sau ngày mở";
        public static final String SUMMER_PERIOD_SEMESTER = "Đợt hè phải dùng học kỳ hè";
        public static final String UNIT_IDS_INVALID =
                "Danh sách đơn vị phải khác rỗng và không trùng";
        public static final String SUMMER_NO_QUOTA = "Đợt hè không có chỉ tiêu";
        public static final String QUOTA_BUILDING_INVALID =
                "Tòa phải thuộc đơn vị áp dụng và không được trùng";
        public static final String PERIOD_UNIT_REQUIRED = "Thiếu đợt hoặc đơn vị nguyện vọng";
        public static final String APPLICATION_PERIOD_IMMUTABLE = "Không đổi đợt của đơn đã tạo";
        public static final String PRIORITY_DUPLICATE = "Không khai trùng diện ưu tiên";
        public static final String END_REASON_INVALID = "Lý do đóng phân phòng không hợp lệ";
        public static final String ROOM_BUILDING_IMMUTABLE = "Không chuyển phòng sang tòa khác";
        public static final String BED_ROOM_IMMUTABLE = "Không chuyển giường sang phòng khác";
        public static final String PLACEMENT_SOURCE_EXACTLY_ONE =
                "Truyền đúng một trong applicationId, transferRequestId hoặc storageRecordId";
        public static final String ACADEMIC_YEAR_CONSECUTIVE = "Hai năm phải liền nhau";
        private ValidationMessage() {}
    }

    /** Thông báo kiểm tra đầu vào của ma trận quyền; template giữ nguyên hợp đồng lỗi hiện tại. */
    public static final class PermissionMessage {
        public static final String INVALID_VERSION = "Phiên bản quyền phải là số nguyên không âm";
        public static final String UNKNOWN_ROLE = "Vai trò không tồn tại trong hệ thống";
        public static final String REQUIRED_LIST = "Danh sách quyền không được để trống";
        public static final String UNKNOWN_PERMISSION = "Danh sách chứa mã quyền không hợp lệ";
        public static final String DUPLICATE_PERMISSION = "Danh sách chứa mã quyền trùng: %s";
        public static final String INELIGIBLE_PERMISSION = "Quyền không phù hợp với vai trò: %s";
        public static final String REQUIRED_PROFILE =
                "Phải giữ quyền xem hồ sơ cá nhân để truy cập và đăng xuất";
        public static final String REQUIRED_READ = "Phải cấp quyền %s trước khi cấp %s";
        public static final String UNKNOWN_SCREEN = "Mã quyền chưa đăng ký màn hình: %s";
        public static final String SERIALIZATION_FAILED =
                "Không thể chuyển danh sách quyền thành JSON";

        private PermissionMessage() {}
    }

    /**
     * Registry màn hình và nhãn phân quyền. Đây là danh mục cố định, không phải quyền đang cấp;
     * quyền động vẫn phải đọc từ role_permission_matrix, không lấy mặc định khi thiếu dữ liệu.
     */
    public static final class PermissionCatalog {

        public static final class Group {
            public static final String ACCOUNT_SYSTEM = "Tài khoản và hệ thống";
            public static final String PERSONAL = "Cá nhân";
            public static final String CATALOG = "Danh mục";
            public static final String RESIDENCY = "Phòng và lưu trú";
            public static final String REGISTRATION = "Đăng ký";
            public static final String FINANCE = "Tài chính";
            public static final String MAINTENANCE_NOTIFICATION = "Bảo trì và thông báo";

            private Group() {}
        }

        public static final Map<String, String> ROLE_LABELS =
                Map.of(
                        Role.ADMIN, "Quản trị viên",
                        Role.STAFF, "Cán bộ quản lý",
                        Role.STUDENT, "Sinh viên",
                        Role.TECHNICIAN, "Kỹ thuật viên");

        public static final List<String> ACTIONS =
                List.of(
                        Action.READ,
                        Action.CREATE,
                        Action.UPDATE,
                        Action.DELETE,
                        Action.LOCK,
                        Action.UNLOCK,
                        Action.RESET_PASSWORD,
                        Action.CROSS_UNIT_ACCESS,
                        Action.APPROVE);

        public static final Map<String, String> ACTION_LABELS =
                Map.ofEntries(
                        Map.entry(Action.READ, "Tìm kiếm / xem"),
                        Map.entry(Action.CREATE, "Thêm"),
                        Map.entry(Action.UPDATE, "Sửa / xử lý"),
                        Map.entry(Action.DELETE, "Xóa"),
                        Map.entry(Action.LOCK, "Khóa tài khoản"),
                        Map.entry(Action.UNLOCK, "Mở khóa tài khoản"),
                        Map.entry(Action.RESET_PASSWORD, "Đặt lại mật khẩu"),
                        Map.entry(Action.CROSS_UNIT_ACCESS, "Cấp quyền tổng hợp"),
                        Map.entry(Action.APPROVE, "Duyệt / từ chối"));

        /** Chỉ tạo ô cho mã có trong Permission.ALL; không tạo quyền theo dữ liệu client gửi. */
        public static final List<ScreenDefinition> SCREENS =
                List.of(
                        new ScreenDefinition(
                                Resource.USER, "Tài khoản nhân sự", Group.ACCOUNT_SYSTEM),
                        new ScreenDefinition(Resource.STUDENT, "Sinh viên", Group.ACCOUNT_SYSTEM),
                        new ScreenDefinition(
                                Resource.ROLE_PERMISSION,
                                "Phân quyền theo vai trò",
                                Group.ACCOUNT_SYSTEM),
                        new ScreenDefinition(
                                Resource.CONFIG, "Cấu hình hệ thống", Group.ACCOUNT_SYSTEM),
                        new ScreenDefinition(
                                Resource.AUDIT, "Nhật ký thao tác", Group.ACCOUNT_SYSTEM),
                        new ScreenDefinition(Resource.DASHBOARD, "Tổng quan", Group.ACCOUNT_SYSTEM),
                        new ScreenDefinition(Resource.PROFILE, "Hồ sơ cá nhân", Group.PERSONAL),
                        new ScreenDefinition(
                                Resource.MANAGEMENT_UNIT, "Đơn vị quản lý", Group.CATALOG),
                        new ScreenDefinition(Resource.BUILDING, "Tòa nhà", Group.CATALOG),
                        new ScreenDefinition(
                                Resource.PRIORITY_CATEGORY, "Diện ưu tiên", Group.CATALOG),
                        new ScreenDefinition(Resource.ROOM_TYPE, "Loại phòng", Group.CATALOG),
                        new ScreenDefinition(Resource.SEMESTER, "Học kỳ", Group.CATALOG),
                        new ScreenDefinition(Resource.ROOM, "Phòng", Group.RESIDENCY),
                        new ScreenDefinition(Resource.BED, "Giường", Group.RESIDENCY),
                        new ScreenDefinition(Resource.CONTRACT, "Hợp đồng", Group.RESIDENCY),
                        new ScreenDefinition(
                                Resource.CONTRACT_EXTENSION, "Gia hạn hợp đồng", Group.RESIDENCY),
                        new ScreenDefinition(
                                Resource.ROOM_TRANSFER, "Chuyển phòng", Group.RESIDENCY),
                        new ScreenDefinition(Resource.MOVE_OUT, "Trả phòng", Group.RESIDENCY),
                        new ScreenDefinition(Resource.STORAGE, "Gửi đồ", Group.RESIDENCY),
                        new ScreenDefinition(
                                Resource.REGISTRATION_PERIOD, "Đợt đăng ký", Group.REGISTRATION),
                        new ScreenDefinition(
                                Resource.ROOM_APPLICATION, "Đơn đăng ký phòng", Group.REGISTRATION),
                        new ScreenDefinition(
                                Resource.SUMMER_REGISTRATION, "Đăng ký hè", Group.REGISTRATION),
                        new ScreenDefinition(
                                Resource.UTILITY_RATE, "Đơn giá điện nước", Group.FINANCE),
                        new ScreenDefinition(
                                Resource.UTILITY_READING, "Chỉ số điện nước", Group.FINANCE),
                        new ScreenDefinition(Resource.INVOICE, "Hóa đơn", Group.FINANCE),
                        new ScreenDefinition(Resource.DEPOSIT, "Tiền cọc", Group.FINANCE),
                        new ScreenDefinition(
                                Resource.MAINTENANCE, "Bảo trì", Group.MAINTENANCE_NOTIFICATION),
                        new ScreenDefinition(
                                Resource.NOTIFICATION,
                                "Thông báo",
                                Group.MAINTENANCE_NOTIFICATION));

        public record ScreenDefinition(String code, String label, String group) {}

        private PermissionCatalog() {}
    }

    /** Vai trò nghiệp vụ; quyền động lưu trong CSDL, giới hạn mặc định khai báo tại ScopeGuard. */
    public static final class Role {
        public static final String ADMIN = "ADMIN";
        public static final String STAFF = "STAFF";
        public static final String TECHNICIAN = "TECHNICIAN";
        public static final String STUDENT = "STUDENT";

        public static final Set<String> ALL = Set.of(ADMIN, STAFF, TECHNICIAN, STUDENT);
        // API quản trị tài khoản không được chuyển hồ sơ sinh viên sang vai trò nhân sự.
        public static final String MANAGEMENT_ROLE_PATTERN = STAFF + "|" + TECHNICIAN + "|" + ADMIN;

        private Role() {}
    }

    /** Hành động phân quyền; READ bao gồm tìm kiếm và xem chi tiết. */
    public static final class Action {
        public static final String READ = "READ";
        public static final String CREATE = "CREATE";
        public static final String UPDATE = "UPDATE";
        public static final String DELETE = "DELETE";
        public static final String LOCK = "LOCK";
        public static final String UNLOCK = "UNLOCK";
        public static final String RESET_PASSWORD = "RESET_PASSWORD";
        public static final String CROSS_UNIT_ACCESS = "CROSS_UNIT_ACCESS";
        public static final String APPROVE = "APPROVE";

        public static final List<String> CRUD = List.of(READ, CREATE, UPDATE, DELETE);

        private Action() {}
    }

    /** Mã đối tượng/màn hình phân quyền; không phải tên bảng SQL. */
    public static final class Resource {
        public static final String USER = "USER";
        public static final String STUDENT = "STUDENT";
        public static final String MANAGEMENT_UNIT = "MANAGEMENT_UNIT";
        public static final String BUILDING = "BUILDING";
        public static final String PRIORITY_CATEGORY = "PRIORITY_CATEGORY";
        public static final String ROOM_TYPE = "ROOM_TYPE";
        public static final String SEMESTER = "SEMESTER";
        public static final String ROOM = "ROOM";
        public static final String BED = "BED";
        public static final String REGISTRATION_PERIOD = "REGISTRATION_PERIOD";
        public static final String ROOM_APPLICATION = "ROOM_APPLICATION";
        public static final String CONTRACT = "CONTRACT";
        public static final String CONTRACT_EXTENSION = "CONTRACT_EXTENSION";
        public static final String ROOM_TRANSFER = "ROOM_TRANSFER";
        public static final String MOVE_OUT = "MOVE_OUT";
        public static final String SUMMER_REGISTRATION = "SUMMER_REGISTRATION";
        public static final String STORAGE = "STORAGE";
        public static final String UTILITY_RATE = "UTILITY_RATE";
        public static final String UTILITY_READING = "UTILITY_READING";
        public static final String INVOICE = "INVOICE";
        public static final String DEPOSIT = "DEPOSIT";
        public static final String MAINTENANCE = "MAINTENANCE";
        public static final String NOTIFICATION = "NOTIFICATION";
        public static final String PROFILE = "PROFILE";
        public static final String CONFIG = "CONFIG";
        public static final String AUDIT = "AUDIT";
        public static final String DASHBOARD = "DASHBOARD";
        public static final String ROLE_PERMISSION = "ROLE_PERMISSION";

        private Resource() {}
    }

    /** Mã quyền đã đăng ký; hằng compile-time dùng được trực tiếp trong @RequirePrivilege. */
    public static final class Permission {
        public static final String USER_READ = Resource.USER + PERMISSION_SEPARATOR + Action.READ;
        public static final String USER_CREATE =
                Resource.USER + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String USER_UPDATE =
                Resource.USER + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String USER_DELETE =
                Resource.USER + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String STUDENT_READ =
                Resource.STUDENT + PERMISSION_SEPARATOR + Action.READ;
        public static final String STUDENT_CREATE =
                Resource.STUDENT + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String STUDENT_UPDATE =
                Resource.STUDENT + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String STUDENT_DELETE =
                Resource.STUDENT + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String MANAGEMENT_UNIT_READ =
                Resource.MANAGEMENT_UNIT + PERMISSION_SEPARATOR + Action.READ;
        public static final String MANAGEMENT_UNIT_CREATE =
                Resource.MANAGEMENT_UNIT + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String MANAGEMENT_UNIT_UPDATE =
                Resource.MANAGEMENT_UNIT + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String MANAGEMENT_UNIT_DELETE =
                Resource.MANAGEMENT_UNIT + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String BUILDING_READ =
                Resource.BUILDING + PERMISSION_SEPARATOR + Action.READ;
        public static final String BUILDING_CREATE =
                Resource.BUILDING + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String BUILDING_UPDATE =
                Resource.BUILDING + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String BUILDING_DELETE =
                Resource.BUILDING + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String PRIORITY_CATEGORY_READ =
                Resource.PRIORITY_CATEGORY + PERMISSION_SEPARATOR + Action.READ;
        public static final String PRIORITY_CATEGORY_CREATE =
                Resource.PRIORITY_CATEGORY + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String PRIORITY_CATEGORY_UPDATE =
                Resource.PRIORITY_CATEGORY + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String PRIORITY_CATEGORY_DELETE =
                Resource.PRIORITY_CATEGORY + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String ROOM_TYPE_READ =
                Resource.ROOM_TYPE + PERMISSION_SEPARATOR + Action.READ;
        public static final String ROOM_TYPE_CREATE =
                Resource.ROOM_TYPE + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String ROOM_TYPE_UPDATE =
                Resource.ROOM_TYPE + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String ROOM_TYPE_DELETE =
                Resource.ROOM_TYPE + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String SEMESTER_READ =
                Resource.SEMESTER + PERMISSION_SEPARATOR + Action.READ;
        public static final String SEMESTER_CREATE =
                Resource.SEMESTER + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String SEMESTER_UPDATE =
                Resource.SEMESTER + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String SEMESTER_DELETE =
                Resource.SEMESTER + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String ROOM_READ = Resource.ROOM + PERMISSION_SEPARATOR + Action.READ;
        public static final String ROOM_CREATE =
                Resource.ROOM + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String ROOM_UPDATE =
                Resource.ROOM + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String ROOM_DELETE =
                Resource.ROOM + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String BED_READ = Resource.BED + PERMISSION_SEPARATOR + Action.READ;
        public static final String BED_CREATE = Resource.BED + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String BED_UPDATE = Resource.BED + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String BED_DELETE = Resource.BED + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String REGISTRATION_PERIOD_READ =
                Resource.REGISTRATION_PERIOD + PERMISSION_SEPARATOR + Action.READ;
        public static final String REGISTRATION_PERIOD_CREATE =
                Resource.REGISTRATION_PERIOD + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String REGISTRATION_PERIOD_UPDATE =
                Resource.REGISTRATION_PERIOD + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String REGISTRATION_PERIOD_DELETE =
                Resource.REGISTRATION_PERIOD + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String ROOM_APPLICATION_READ =
                Resource.ROOM_APPLICATION + PERMISSION_SEPARATOR + Action.READ;
        public static final String ROOM_APPLICATION_CREATE =
                Resource.ROOM_APPLICATION + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String ROOM_APPLICATION_UPDATE =
                Resource.ROOM_APPLICATION + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String ROOM_APPLICATION_DELETE =
                Resource.ROOM_APPLICATION + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String CONTRACT_READ =
                Resource.CONTRACT + PERMISSION_SEPARATOR + Action.READ;
        public static final String CONTRACT_CREATE =
                Resource.CONTRACT + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String CONTRACT_UPDATE =
                Resource.CONTRACT + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String CONTRACT_DELETE =
                Resource.CONTRACT + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String CONTRACT_EXTENSION_READ =
                Resource.CONTRACT_EXTENSION + PERMISSION_SEPARATOR + Action.READ;
        public static final String CONTRACT_EXTENSION_CREATE =
                Resource.CONTRACT_EXTENSION + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String CONTRACT_EXTENSION_UPDATE =
                Resource.CONTRACT_EXTENSION + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String CONTRACT_EXTENSION_DELETE =
                Resource.CONTRACT_EXTENSION + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String ROOM_TRANSFER_READ =
                Resource.ROOM_TRANSFER + PERMISSION_SEPARATOR + Action.READ;
        public static final String ROOM_TRANSFER_CREATE =
                Resource.ROOM_TRANSFER + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String ROOM_TRANSFER_UPDATE =
                Resource.ROOM_TRANSFER + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String ROOM_TRANSFER_DELETE =
                Resource.ROOM_TRANSFER + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String ROOM_TRANSFER_APPROVE =
                Resource.ROOM_TRANSFER + PERMISSION_SEPARATOR + Action.APPROVE;
        public static final String MOVE_OUT_READ =
                Resource.MOVE_OUT + PERMISSION_SEPARATOR + Action.READ;
        public static final String MOVE_OUT_CREATE =
                Resource.MOVE_OUT + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String MOVE_OUT_UPDATE =
                Resource.MOVE_OUT + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String MOVE_OUT_DELETE =
                Resource.MOVE_OUT + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String SUMMER_REGISTRATION_READ =
                Resource.SUMMER_REGISTRATION + PERMISSION_SEPARATOR + Action.READ;
        public static final String SUMMER_REGISTRATION_CREATE =
                Resource.SUMMER_REGISTRATION + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String SUMMER_REGISTRATION_UPDATE =
                Resource.SUMMER_REGISTRATION + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String SUMMER_REGISTRATION_DELETE =
                Resource.SUMMER_REGISTRATION + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String STORAGE_READ =
                Resource.STORAGE + PERMISSION_SEPARATOR + Action.READ;
        public static final String STORAGE_CREATE =
                Resource.STORAGE + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String STORAGE_UPDATE =
                Resource.STORAGE + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String STORAGE_DELETE =
                Resource.STORAGE + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String UTILITY_RATE_READ =
                Resource.UTILITY_RATE + PERMISSION_SEPARATOR + Action.READ;
        public static final String UTILITY_RATE_CREATE =
                Resource.UTILITY_RATE + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String UTILITY_RATE_UPDATE =
                Resource.UTILITY_RATE + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String UTILITY_RATE_DELETE =
                Resource.UTILITY_RATE + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String UTILITY_READING_READ =
                Resource.UTILITY_READING + PERMISSION_SEPARATOR + Action.READ;
        public static final String UTILITY_READING_CREATE =
                Resource.UTILITY_READING + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String UTILITY_READING_UPDATE =
                Resource.UTILITY_READING + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String UTILITY_READING_DELETE =
                Resource.UTILITY_READING + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String INVOICE_READ =
                Resource.INVOICE + PERMISSION_SEPARATOR + Action.READ;
        public static final String INVOICE_CREATE =
                Resource.INVOICE + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String INVOICE_UPDATE =
                Resource.INVOICE + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String INVOICE_DELETE =
                Resource.INVOICE + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String DEPOSIT_READ =
                Resource.DEPOSIT + PERMISSION_SEPARATOR + Action.READ;
        public static final String DEPOSIT_CREATE =
                Resource.DEPOSIT + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String DEPOSIT_UPDATE =
                Resource.DEPOSIT + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String DEPOSIT_DELETE =
                Resource.DEPOSIT + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String MAINTENANCE_READ =
                Resource.MAINTENANCE + PERMISSION_SEPARATOR + Action.READ;
        public static final String MAINTENANCE_CREATE =
                Resource.MAINTENANCE + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String MAINTENANCE_UPDATE =
                Resource.MAINTENANCE + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String MAINTENANCE_DELETE =
                Resource.MAINTENANCE + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String NOTIFICATION_READ =
                Resource.NOTIFICATION + PERMISSION_SEPARATOR + Action.READ;
        public static final String NOTIFICATION_CREATE =
                Resource.NOTIFICATION + PERMISSION_SEPARATOR + Action.CREATE;
        public static final String NOTIFICATION_UPDATE =
                Resource.NOTIFICATION + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String NOTIFICATION_DELETE =
                Resource.NOTIFICATION + PERMISSION_SEPARATOR + Action.DELETE;
        public static final String PROFILE_READ =
                Resource.PROFILE + PERMISSION_SEPARATOR + Action.READ;
        public static final String PROFILE_UPDATE =
                Resource.PROFILE + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String CONFIG_READ =
                Resource.CONFIG + PERMISSION_SEPARATOR + Action.READ;
        public static final String CONFIG_UPDATE =
                Resource.CONFIG + PERMISSION_SEPARATOR + Action.UPDATE;
        public static final String AUDIT_READ = Resource.AUDIT + PERMISSION_SEPARATOR + Action.READ;
        public static final String DASHBOARD_READ =
                Resource.DASHBOARD + PERMISSION_SEPARATOR + Action.READ;
        public static final String STUDENT_RESET_PASSWORD =
                Resource.STUDENT + PERMISSION_SEPARATOR + Action.RESET_PASSWORD;
        public static final String USER_LOCK = Resource.USER + PERMISSION_SEPARATOR + Action.LOCK;
        public static final String USER_UNLOCK =
                Resource.USER + PERMISSION_SEPARATOR + Action.UNLOCK;
        public static final String USER_RESET_PASSWORD =
                Resource.USER + PERMISSION_SEPARATOR + Action.RESET_PASSWORD;
        public static final String USER_CROSS_UNIT_ACCESS =
                Resource.USER + PERMISSION_SEPARATOR + Action.CROSS_UNIT_ACCESS;
        public static final String ROLE_PERMISSION_READ =
                Resource.ROLE_PERMISSION + PERMISSION_SEPARATOR + Action.READ;
        public static final String ROLE_PERMISSION_UPDATE =
                Resource.ROLE_PERMISSION + PERMISSION_SEPARATOR + Action.UPDATE;

        // Danh mục đóng: ADMIN có mọi mã trong danh mục, mã lạ vẫn bị từ chối.
        public static final Set<String> ALL =
                Set.of(
                        USER_READ,
                        USER_CREATE,
                        USER_UPDATE,
                        USER_DELETE,
                        STUDENT_READ,
                        STUDENT_CREATE,
                        STUDENT_UPDATE,
                        STUDENT_DELETE,
                        MANAGEMENT_UNIT_READ,
                        MANAGEMENT_UNIT_CREATE,
                        MANAGEMENT_UNIT_UPDATE,
                        MANAGEMENT_UNIT_DELETE,
                        BUILDING_READ,
                        BUILDING_CREATE,
                        BUILDING_UPDATE,
                        BUILDING_DELETE,
                        PRIORITY_CATEGORY_READ,
                        PRIORITY_CATEGORY_CREATE,
                        PRIORITY_CATEGORY_UPDATE,
                        PRIORITY_CATEGORY_DELETE,
                        ROOM_TYPE_READ,
                        ROOM_TYPE_CREATE,
                        ROOM_TYPE_UPDATE,
                        ROOM_TYPE_DELETE,
                        SEMESTER_READ,
                        SEMESTER_CREATE,
                        SEMESTER_UPDATE,
                        SEMESTER_DELETE,
                        ROOM_READ,
                        ROOM_CREATE,
                        ROOM_UPDATE,
                        ROOM_DELETE,
                        BED_READ,
                        BED_CREATE,
                        BED_UPDATE,
                        BED_DELETE,
                        REGISTRATION_PERIOD_READ,
                        REGISTRATION_PERIOD_CREATE,
                        REGISTRATION_PERIOD_UPDATE,
                        REGISTRATION_PERIOD_DELETE,
                        ROOM_APPLICATION_READ,
                        ROOM_APPLICATION_CREATE,
                        ROOM_APPLICATION_UPDATE,
                        ROOM_APPLICATION_DELETE,
                        CONTRACT_READ,
                        CONTRACT_CREATE,
                        CONTRACT_UPDATE,
                        CONTRACT_DELETE,
                        CONTRACT_EXTENSION_READ,
                        CONTRACT_EXTENSION_CREATE,
                        CONTRACT_EXTENSION_UPDATE,
                        CONTRACT_EXTENSION_DELETE,
                        ROOM_TRANSFER_READ,
                        ROOM_TRANSFER_CREATE,
                        ROOM_TRANSFER_UPDATE,
                        ROOM_TRANSFER_DELETE,
                        ROOM_TRANSFER_APPROVE,
                        MOVE_OUT_READ,
                        MOVE_OUT_CREATE,
                        MOVE_OUT_UPDATE,
                        MOVE_OUT_DELETE,
                        SUMMER_REGISTRATION_READ,
                        SUMMER_REGISTRATION_CREATE,
                        SUMMER_REGISTRATION_UPDATE,
                        SUMMER_REGISTRATION_DELETE,
                        STORAGE_READ,
                        STORAGE_CREATE,
                        STORAGE_UPDATE,
                        STORAGE_DELETE,
                        UTILITY_RATE_READ,
                        UTILITY_RATE_CREATE,
                        UTILITY_RATE_UPDATE,
                        UTILITY_RATE_DELETE,
                        UTILITY_READING_READ,
                        UTILITY_READING_CREATE,
                        UTILITY_READING_UPDATE,
                        UTILITY_READING_DELETE,
                        INVOICE_READ,
                        INVOICE_CREATE,
                        INVOICE_UPDATE,
                        INVOICE_DELETE,
                        DEPOSIT_READ,
                        DEPOSIT_CREATE,
                        DEPOSIT_UPDATE,
                        DEPOSIT_DELETE,
                        MAINTENANCE_READ,
                        MAINTENANCE_CREATE,
                        MAINTENANCE_UPDATE,
                        MAINTENANCE_DELETE,
                        NOTIFICATION_READ,
                        NOTIFICATION_CREATE,
                        NOTIFICATION_UPDATE,
                        NOTIFICATION_DELETE,
                        PROFILE_READ,
                        PROFILE_UPDATE,
                        CONFIG_READ,
                        CONFIG_UPDATE,
                        AUDIT_READ,
                        DASHBOARD_READ,
                        STUDENT_RESET_PASSWORD,
                        USER_LOCK,
                        USER_UNLOCK,
                        USER_RESET_PASSWORD,
                        USER_CROSS_UNIT_ACCESS,
                        ROLE_PERMISSION_READ,
                        ROLE_PERMISSION_UPDATE);

        private Permission() {}
    }

    /** Lệnh nghiệp vụ trong allowedActions; khác mã quyền CRUD và giữ nguyên giá trị API. */
    public static final class BusinessAction {
        public static final String EDIT = "edit";
        public static final String DELETE = "delete";
        public static final String SUBMIT = "submit";
        public static final String APPROVE = "approve";
        public static final String REJECT = "reject";
        public static final String CANCEL = "cancel";
        public static final String ASSIGN = "assign";
        public static final String START = "start";
        public static final String ADD_NOTE = "add-note";
        public static final String RESOLVE = "resolve";
        public static final String ACCEPT = "accept";
        public static final String REOPEN = "reopen";
        public static final String CONCLUDE = "conclude";
        public static final String ACTIVATE = "activate";
        public static final String RELEASE_HOLD = "release-hold";
        public static final String VERIFY_PRIORITY = "verify-priority";
        public static final String CREATE_EXTENSION_REQUEST = "create-extension-request";
        public static final String CREATE_TRANSFER_REQUEST = "create-transfer-request";
        public static final String MOVE_OUT = "move-out";
        public static final String CREATE_STORAGE_RECORD = "create-storage-record";
        public static final String RETURN_STORAGE = "return-storage";

        public static final String CONFIRM_PAYMENT = "confirm-payment";
        public static final String DEPOSIT_TOPUP = "deposit-topup";
        public static final String DEPOSIT_REFUND = "deposit-refund";

        private BusinessAction() {}
    }

    /** Trạng thái phòng; MAINTENANCE là trạng thái phòng, không phải quyền bảo trì. */
    public static final class RoomStatus {
        public static final String ACTIVE = "ACTIVE";
        public static final String MAINTENANCE = "MAINTENANCE";
        public static final String CLOSED = "CLOSED";
        public static final String PATTERN = ACTIVE + "|" + MAINTENANCE + "|" + CLOSED;

        private RoomStatus() {}
    }

    /** Trạng thái giường; không dùng chung tập trạng thái của phòng. */
    public static final class BedOperationStatus {
        public static final String ACTIVE = "ACTIVE";
        public static final String BROKEN = "BROKEN";
        public static final String RETIRED = "RETIRED";
        public static final String PATTERN = ACTIVE + "|" + BROKEN + "|" + RETIRED;

        private BedOperationStatus() {}
    }

    /** Loại phân phòng: giữ chỗ (HOLD) hoặc đang ở (STAY); giá trị khớp CHECK của CSDL. */
    public static final class AssignmentKind {
        public static final String HOLD = "HOLD";
        public static final String STAY = "STAY";

        private AssignmentKind() {}
    }

    /** Lý do đóng phân phòng; giá trị khớp CHECK end_reason của room_assignment_history. */
    public static final class AssignmentEndReason {
        public static final String HOLD_CANCELLED = "HOLD_CANCELLED";
        public static final String HOLD_EXPIRED = "HOLD_EXPIRED";
        public static final String TRANSFER = "TRANSFER";
        public static final String MOVE_OUT = "MOVE_OUT";
        public static final String SUMMER_LEAVE = "SUMMER_LEAVE";

        /** Lý do được trả giường trực tiếp; TRANSFER chỉ do nghiệp vụ chuyển phòng ghi. */
        public static final Set<String> RELEASE =
                Set.of(HOLD_CANCELLED, HOLD_EXPIRED, MOVE_OUT, SUMMER_LEAVE);

        private AssignmentEndReason() {}
    }

    /** Nguồn yêu cầu chọn giường cho split view (API 4.4); tên khớp tham số truy vấn. */
    public static final class PlacementSource {
        public static final String APPLICATION = "applicationId";
        public static final String TRANSFER_REQUEST = "transferRequestId";
        public static final String STORAGE_RECORD = "storageRecordId";

        private PlacementSource() {}
    }

    /** Lý do hệ thống tự ghi khi thao tác không do người dùng nhập. */
    public static final class AuditReason {
        public static final String LEADER_AUTO_REMOVED =
                "Tự gỡ trưởng phòng do phân phòng của sinh viên đã đóng";

        private AuditReason() {}
    }

    /** Trạng thái đơn đăng ký; policy quyết định chuyển trạng thái hợp lệ. */
    public static final class ApplicationStatus {
        public static final String DRAFT = "DRAFT";
        public static final String SUBMITTED = "SUBMITTED";
        public static final String APPROVED = "APPROVED";
        public static final String ASSIGNED = "ASSIGNED";
        public static final String REJECTED = "REJECTED";
        public static final String CANCELLED = "CANCELLED";

        private ApplicationStatus() {}
    }

    /** Trạng thái phiếu bảo trì; không bỏ qua kiểm tra chủ sở hữu/đơn vị. */
    public static final class MaintenanceStatus {
        public static final String OPEN = "OPEN";
        public static final String ASSIGNED = "ASSIGNED";
        public static final String IN_PROGRESS = "IN_PROGRESS";
        public static final String RESOLVED = "RESOLVED";
        public static final String CLOSED = "CLOSED";
        public static final String REJECTED = "REJECTED";
        public static final String CANCELLED = "CANCELLED";

        private MaintenanceStatus() {}
    }

    /** Các trạng thái hợp đồng được khung CRUD sử dụng. */
    public static final class ContractStatus {
        public static final String PENDING = "PENDING";
        public static final String ACTIVE = "ACTIVE";
        public static final String CANCELLED = "CANCELLED";
        public static final String EXPIRED = "EXPIRED";
        public static final String TERMINATED = "TERMINATED";

        private ContractStatus() {}
    }

    /** Loại khoản trong sổ cọc, giữ nguyên các mã đã có trong CSDL. */
    public static final class DepositType {
        public static final String DEPOSIT = "DEPOSIT";
        public static final String DEDUCTION = "DEDUCTION";
        public static final String TOPUP = "TOPUP";
        public static final String REFUND = "REFUND";

        private DepositType() {}
    }

    /** Gia hạn/chuyển phòng chỉ được sửa/xóa khi yêu cầu còn chờ xử lý. */
    public static final class StayRequestStatus {
        public static final String PENDING = "PENDING";
        public static final String APPROVED = "APPROVED";
        public static final String REJECTED = "REJECTED";
        public static final String CANCELLED = "CANCELLED";

        private StayRequestStatus() {}
    }

    /** Trạng thái lựa chọn đăng ký hè được khung CRUD sử dụng. */
    public static final class SummerStatus {
        public static final String PENDING = "PENDING";
        public static final String APPROVED = "APPROVED";
        public static final String REJECTED = "REJECTED";

        private SummerStatus() {}
    }

    /** Trạng thái hóa đơn trước và sau khi xác nhận thanh toán. */
    public static final class InvoiceStatus {
        public static final String UNPAID = "UNPAID";
        public static final String PAID = "PAID";

        private InvoiceStatus() {}
    }

    /** Mã giới tính hiện có trong DTO và CSDL. */
    public static final class Gender {
        public static final String M = "M";
        public static final String F = "F";
        public static final String PATTERN = M + "|" + F;

        private Gender() {}
    }

    /** Loại học kỳ; SUMMER dùng cho học kỳ hè. */
    public static final class SemesterType {
        public static final String HK1 = "HK1";
        public static final String HK2 = "HK2";
        public static final String SUMMER = "SUMMER";
        public static final String PATTERN = HK1 + "|" + HK2 + "|" + SUMMER;

        private SemesterType() {}
    }

    /** Loại đợt đăng ký; độc lập với loại học kỳ. */
    public static final class RegistrationKind {
        public static final String REGULAR = "REGULAR";
        public static final String SUMMER = "SUMMER";
        public static final String PATTERN = REGULAR + "|" + SUMMER;

        private RegistrationKind() {}
    }

    /** Loại công tơ điện/nước; DTO dùng cùng tập mã. */
    public static final class MeterType {
        public static final String ELECTRIC = "ELECTRIC";
        public static final String WATER = "WATER";
        public static final String PATTERN = ELECTRIC + "|" + WATER;

        private MeterType() {}
    }

    /** Nhóm hỏng hóc; có thêm nhóm ngoài điện/nước. */
    public static final class MaintenanceCategory {
        public static final String ELECTRIC = "ELECTRIC";
        public static final String WATER = "WATER";
        public static final String FURNITURE = "FURNITURE";
        public static final String DOOR_LOCK = "DOOR_LOCK";
        public static final String OTHER = "OTHER";
        public static final String PATTERN =
                ELECTRIC + "|" + WATER + "|" + FURNITURE + "|" + DOOR_LOCK + "|" + OTHER;

        private MaintenanceCategory() {}
    }

    /** Mức độ nghiêm trọng của phiếu bảo trì. */
    public static final class MaintenanceSeverity {
        public static final String LOW = "LOW";
        public static final String MEDIUM = "MEDIUM";
        public static final String HIGH = "HIGH";
        public static final String PATTERN = LOW + "|" + MEDIUM + "|" + HIGH;

        private MaintenanceSeverity() {}
    }

    /** Phương thức xác nhận thanh toán; không thay đổi dữ liệu đã lưu. */
    public static final class PaymentMethod {
        public static final String CASH = "CASH";
        public static final String TRANSFER = "TRANSFER";
        public static final String PATTERN = CASH + "|" + TRANSFER;

        private PaymentMethod() {}
    }

    /** Phí phòng hoặc điện/nước; hai loại có trường tham chiếu khác nhau. */
    public static final class InvoiceType {
        public static final String ROOM_FEE = "ROOM_FEE";
        public static final String UTILITY = "UTILITY";

        public static final String ROOM_FEE_PREFIX = "HP";
        public static final String UTILITY_PREFIX = "DN";

        private InvoiceType() {}
    }

    /** Phạm vi nhận thông báo; USER chỉ do hệ thống tạo, không nhận từ form thủ công. */
    public static final class NotificationScope {
        public static final String ALL = "ALL";
        public static final String UNIT = "UNIT";
        public static final String BUILDING = "BUILDING";
        public static final String FLOOR = "FLOOR";
        public static final String ROOM = "ROOM";
        public static final String TRANSFER = "TRANSFER";
        public static final String USER = "USER";

        public static final String MANUAL_PATTERN =
                ALL + "|" + UNIT + "|" + BUILDING + "|" + FLOOR + "|" + ROOM;

        private NotificationScope() {}
    }

    /** Nguồn thông báo dùng để dựng liên kết tới nghiệp vụ. */
    public static final class NotificationSource {
        public static final String MANUAL = "MANUAL";
        public static final String SUMMER_PERIOD_OPENED = "SUMMER_PERIOD_OPENED";
        public static final String APPLICATION_APPROVED = "APPLICATION_APPROVED";
        public static final String BED_HELD = "BED_HELD";
        public static final String INVOICE_ISSUED = "INVOICE_ISSUED";
        public static final String TICKET_STATUS_CHANGED = "TICKET_STATUS_CHANGED";
        public static final String DEPOSIT_DEDUCTED = "DEPOSIT_DEDUCTED";
        public static final String CONTRACT_EXPIRING = "CONTRACT_EXPIRING";
        public static final String CONTRACT_MOVED_OUT = "CONTRACT_MOVED_OUT";
        public static final String STORAGE_RECORD_CHANGED = "STORAGE_RECORD_CHANGED";

        private NotificationSource() {}
    }

    /** Tên claim/attribute chung giữa bộ kiểm tra JWT và đồng bộ tài khoản Keycloak. */
    public static final class KeycloakClaim {
        public static final String REALM_ACCESS = "realm_access";
        public static final String ROLES = "roles";
        public static final String SECURITY_VERSION = "dms_security_version";
        public static final String AUTHORITY_PREFIX = "ROLE_";

        private KeycloakClaim() {}
    }

    /** Lựa chọn đăng ký hè; không thay đổi tập mã đang được khung CRUD chấp nhận. */
    public static final class SummerChoice {
        public static final String STAY = "STAY";
        public static final String TEMP_LEAVE = "TEMP_LEAVE";
        public static final String LEAVE = "LEAVE";
        public static final Set<String> ALL = Set.of(STAY, TEMP_LEAVE, LEAVE);

        private SummerChoice() {}
    }

    /** Khóa cấu hình nghiệp vụ lưu tại system_config; giá trị cấu hình vẫn lấy từ CSDL. */
    public static final class ConfigKey {
        public static final String INVOICE_DUE_DAYS = "invoice.due_days";
        public static final String CONTRACT_ACTIVATION_DAYS = "contract.activation_days";
        public static final String CONTRACT_EXPIRING_DAYS = "contract.expiring_days";
        public static final String MAINTENANCE_AUTOCLOSE_DAYS = "maintenance.autoclose_days";
        public static final String DEPOSIT_TOPUP_DUE_DAYS = "deposit.topup_due_days";

        private ConfigKey() {}
    }

    /**
     * Mã sự kiện audit giữ nguyên dữ liệu lịch sử. Dù một số mã trùng quyền thao tác, audit là hợp
     * đồng riêng; không đổi mã audit khi đổi cách đặt tên hoặc cấp quyền.
     */
    public static final class AuditAction {
        public static final String BED_CREATE = "BED_CREATE";
        public static final String BED_DELETE = "BED_DELETE";
        public static final String BED_OCCUPY = "BED_OCCUPY";
        public static final String BED_RELEASE = "BED_RELEASE";
        public static final String BED_STAY = "BED_STAY";
        public static final String BED_STATUS = "BED_STATUS";
        public static final String BED_UPDATE = "BED_UPDATE";
        public static final String CATALOG_PREFIX = "CATALOG_";
        public static final String CONFIG_UPDATE = "CONFIG_UPDATE";
        public static final String CRUD_CREATE = "CRUD_CREATE";
        public static final String CRUD_DELETE = "CRUD_DELETE";
        public static final String CRUD_UPDATE = "CRUD_UPDATE";
        public static final String MAINTENANCE_UPDATE = "MAINTENANCE_UPDATE";
        public static final String NOTIFICATION_CREATE = "NOTIFICATION_CREATE";
        public static final String NOTIFICATION_DELETE = "NOTIFICATION_DELETE";
        public static final String NOTIFICATION_UPDATE = "NOTIFICATION_UPDATE";
        public static final String RATE_CREATE = "RATE_CREATE";
        public static final String RATE_DELETE = "RATE_DELETE";
        public static final String RATE_UPDATE = "RATE_UPDATE";
        public static final String READING_CREATE = "READING_CREATE";
        public static final String READING_DELETE = "READING_DELETE";
        public static final String READING_UPDATE = "READING_UPDATE";
        public static final String ROOM_CREATE = "ROOM_CREATE";
        public static final String ROOM_DELETE = "ROOM_DELETE";
        public static final String ROOM_LEADER_CHANGE = "ROOM_LEADER_CHANGE";
        public static final String ROOM_STATUS = "ROOM_STATUS";
        public static final String ROOM_TRANSFER = "ROOM_TRANSFER";
        public static final String ROOM_UPDATE = "ROOM_UPDATE";
        public static final String STUDENT_CREATE = "STUDENT_CREATE";
        public static final String STUDENT_DELETE = "STUDENT_DELETE";
        public static final String STUDENT_UPDATE = "STUDENT_UPDATE";
        public static final String USER_CREATE = "USER_CREATE";
        public static final String USER_CROSS_UNIT_ACCESS = "USER_CROSS_UNIT_ACCESS";
        public static final String USER_LOCK = "USER_LOCK";
        public static final String USER_RESET_PASSWORD = "USER_RESET_PASSWORD";
        public static final String USER_UNLOCK = "USER_UNLOCK";
        public static final String USER_UPDATE = "USER_UPDATE";
        public static final String ROLE_PERMISSION_UPDATE = "ROLE_PERMISSION_UPDATE";
        public static final String STUDENT_IMPORT = "STUDENT_IMPORT";
        public static final String CONTRACT_AUTO_CANCEL = "CONTRACT_AUTO_CANCEL";
        public static final String APPLICATION_AUTO_CANCEL = "APPLICATION_AUTO_CANCEL";
        public static final String MAINTENANCE_AUTO_CLOSE = "MAINTENANCE_AUTO_CLOSE";
        public static final String DEPOSIT_REMINDER = "DEPOSIT_REMINDER";
        public static final String CONTRACT_EXPIRING_REMINDER = "CONTRACT_EXPIRING_REMINDER";
        public static final String DEPOSIT_COLLECT = "DEPOSIT_COLLECT";
        public static final String DEPOSIT_DEDUCT = "DEPOSIT_DEDUCT";
        public static final String DEPOSIT_TOPUP = "DEPOSIT_TOPUP";
        public static final String DEPOSIT_REFUND = "DEPOSIT_REFUND";
        public static final String INVOICE_BATCH_ISSUE = "INVOICE_BATCH_ISSUE";
        public static final String INVOICE_CONFIRM_PAYMENT = "INVOICE_CONFIRM_PAYMENT";
        public static final String MAINTENANCE_CONCLUDE = "MAINTENANCE_CONCLUDE";
        public static final String CONTRACT_MOVE_OUT = "CONTRACT_MOVE_OUT";
        public static final String STORAGE_CREATE = "STORAGE_CREATE";
        public static final String STORAGE_RETURN = "STORAGE_RETURN";
        public static final String CONTRACT_ACTIVATE = "CONTRACT_ACTIVATE";
        public static final String CONTRACT_EXTENSION_APPROVE = "CONTRACT_EXTENSION_APPROVE";
        public static final String CONTRACT_EXTENSION_REJECT = "CONTRACT_EXTENSION_REJECT";
        public static final String APPLICATION_ASSIGN = "APPLICATION_ASSIGN";
        public static final String APPLICATION_RELEASE_HOLD = "APPLICATION_RELEASE_HOLD";
        public static final String APPLICATION_PRIORITY_VERIFY = "APPLICATION_PRIORITY_VERIFY";
        public static final String SUMMER_CHOICE_APPROVE = "SUMMER_CHOICE_APPROVE";
        public static final String SUMMER_CHOICE_REJECT = "SUMMER_CHOICE_REJECT";

        private AuditAction() {}
    }

    /** Mã đối tượng audit/timeline tách khỏi mã quyền để giữ nguyên dữ liệu lịch sử. */
    public static final class EntityType {
        public static final String APPLICATION = "APPLICATION";
        public static final String ASSIGNMENT = "ASSIGNMENT";
        public static final String BED = "BED";
        public static final String CONTRACT = "CONTRACT";
        public static final String DEPOSIT_TRANSACTION = "DEPOSIT_TRANSACTION";
        public static final String EXTENSION = "EXTENSION";
        public static final String INVOICE = "INVOICE";
        public static final String MAINTENANCE = "MAINTENANCE";
        public static final String MOVE_OUT = "MOVE_OUT";
        public static final String NOTIFICATION = "NOTIFICATION";
        public static final String ROOM = "ROOM";
        public static final String STORAGE = "STORAGE";
        public static final String SUMMER_CHOICE = "SUMMER_CHOICE";
        public static final String TRANSFER = "TRANSFER";
        public static final String USER = "USER";
        public static final String UTILITY_RATE = "UTILITY_RATE";

        private EntityType() {}
    }

    /** Nguồn yêu cầu gia hạn; giá trị khớp CHECK của contract_extension_request.source. */
    public static final class ExtensionSource {
        public static final String STUDENT = "STUDENT";
        public static final String SUMMER_STAY = "SUMMER_STAY";

        private ExtensionSource() {}
    }

    /** Tác vụ nội bộ không giả danh Admin, actor_id null được hiển thị là SYSTEM. */
    public static final class SystemJob {
        public static final String CANCEL_PENDING = "cancel-pending-contracts";
        public static final String CLOSE_RESOLVED = "close-resolved-tickets";
        public static final String REMIND_DEPOSIT = "remind-overdue-deposits";
        public static final String REMIND_CONTRACT = "remind-expiring-contracts";
        public static final String EXPIRED_REASON = "SYSTEM: Quá hạn kích hoạt hợp đồng";
        public static final String AUTOCLOSE_REASON = "SYSTEM: Quá hạn nghiệm thu sau khi sửa xong";
        public static final String CONTRACT_SOURCE = "CONTRACT_EXPIRING";
        public static final String DEPOSIT_SOURCE = "DEPOSIT_OVERDUE";

        private SystemJob() {}
    }
}
