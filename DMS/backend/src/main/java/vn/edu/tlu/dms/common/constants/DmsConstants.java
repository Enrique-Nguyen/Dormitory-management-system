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
        public static final String BED_OCCUPIED = "BED_OCCUPIED";
        public static final String BED_TAKEN = "BED_TAKEN";
        public static final String BOOTSTRAP_USERNAME_IN_USE = "BOOTSTRAP_USERNAME_IN_USE";
        public static final String CONCURRENT_UPDATE = "CONCURRENT_UPDATE";
        public static final String CONFIG_VALUE_INVALID = "CONFIG_VALUE_INVALID";
        public static final String CONTRACT_EXISTS = "CONTRACT_EXISTS";
        public static final String CROSS_UNIT_ONLY_STAFF = "CROSS_UNIT_ONLY_STAFF";
        public static final String DEPOSIT_INVALID = "DEPOSIT_INVALID";
        public static final String DUPLICATE_INVOICE = "DUPLICATE_INVOICE";
        public static final String DUPLICATE_USERNAME = "DUPLICATE_USERNAME";
        public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
        public static final String FORBIDDEN = "FORBIDDEN";
        public static final String IDENTITY_ALREADY_LINKED = "IDENTITY_ALREADY_LINKED";
        public static final String IDENTITY_NOT_CONFIGURED = "IDENTITY_NOT_CONFIGURED";
        public static final String IDENTITY_NOT_LINKED = "IDENTITY_NOT_LINKED";
        public static final String IDENTITY_UNAVAILABLE = "IDENTITY_UNAVAILABLE";
        public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
        public static final String INVALID_STATE_TRANSITION = "INVALID_STATE_TRANSITION";
        public static final String INVOICE_ALREADY_PAID = "INVOICE_ALREADY_PAID";
        public static final String LEADER_NOT_STAYING = "LEADER_NOT_STAYING";
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
        public static final String REQUEST_PENDING_EXISTS = "REQUEST_PENDING_EXISTS";
        public static final String RESOURCE_IN_USE = "RESOURCE_IN_USE";
        public static final String ROOM_CODE_EXISTS = "ROOM_CODE_EXISTS";
        public static final String ROOM_OCCUPIED = "ROOM_OCCUPIED";
        public static final String SEMESTER_OVERLAP = "SEMESTER_OVERLAP";
        public static final String SESSION_REVOKED = "SESSION_REVOKED";
        public static final String STORAGE_RECORD_EXISTS = "STORAGE_RECORD_EXISTS";
        public static final String STUDENT_ALREADY_ASSIGNED = "STUDENT_ALREADY_ASSIGNED";
        public static final String STUDENT_HAS_ACTIVE_CONTRACT = "STUDENT_HAS_ACTIVE_CONTRACT";
        public static final String SUMMER_CHOICE_EXISTS = "SUMMER_CHOICE_EXISTS";
        public static final String TECHNICIAN_OUT_OF_UNIT = "TECHNICIAN_OUT_OF_UNIT";
        public static final String TICKET_LOCATION_INVALID = "TICKET_LOCATION_INVALID";
        public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
        public static final String UNIT_NOT_IN_PERIOD = "UNIT_NOT_IN_PERIOD";
        public static final String USERNAME_EXISTS = "USERNAME_EXISTS";
        public static final String VALIDATION_FAILED = "VALIDATION_FAILED";

        private ErrorCode() {}
    }

    /** Tên field của hợp đồng validation, audit và ma trận quyền; không dùng làm tên cột SQL. */
    public static final class ApiField {
        public static final String ID = "id";
        public static final String FORM = "form";
        public static final String SORT = "sort";
        public static final String PAGE = "page";
        public static final String FIELD_ERRORS = "fieldErrors";
        public static final String FIELD = "field";
        public static final String MESSAGE = "message";
        public static final String ROLE = "role";
        public static final String VERSION = "version";
        public static final String PERMISSIONS = "permissions";

        private ApiField() {}
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
                        Action.CROSS_UNIT_ACCESS);

        public static final Map<String, String> ACTION_LABELS =
                Map.of(
                        Action.READ, "Tìm kiếm / xem",
                        Action.CREATE, "Thêm",
                        Action.UPDATE, "Sửa / xử lý",
                        Action.DELETE, "Xóa",
                        Action.LOCK, "Khóa tài khoản",
                        Action.UNLOCK, "Mở khóa tài khoản",
                        Action.RESET_PASSWORD, "Đặt lại mật khẩu",
                        Action.CROSS_UNIT_ACCESS, "Cấp quyền tổng hợp");

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

    /** Trạng thái đơn đăng ký; policy quyết định chuyển trạng thái hợp lệ. */
    public static final class ApplicationStatus {
        public static final String DRAFT = "DRAFT";
        public static final String SUBMITTED = "SUBMITTED";
        public static final String APPROVED = "APPROVED";
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

        private ContractStatus() {}
    }

    /** Gia hạn/chuyển phòng chỉ được sửa/xóa khi yêu cầu còn chờ xử lý. */
    public static final class StayRequestStatus {
        public static final String PENDING = "PENDING";

        private StayRequestStatus() {}
    }

    /** Trạng thái lựa chọn đăng ký hè được khung CRUD sử dụng. */
    public static final class SummerStatus {
        public static final String PENDING = "PENDING";

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

        private InvoiceType() {}
    }

    /** Phạm vi nhận thông báo; USER chỉ do hệ thống tạo, không nhận từ form thủ công. */
    public static final class NotificationScope {
        public static final String ALL = "ALL";
        public static final String UNIT = "UNIT";
        public static final String BUILDING = "BUILDING";
        public static final String FLOOR = "FLOOR";
        public static final String ROOM = "ROOM";
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
        public static final String CONTRACT_ACTIVATION_DAYS = "contract.activation_days";

        private ConfigKey() {}
    }

    /**
     * Mã sự kiện audit giữ nguyên dữ liệu lịch sử. Dù một số mã trùng quyền thao tác, audit là hợp
     * đồng riêng; không đổi mã audit khi đổi cách đặt tên hoặc cấp quyền.
     */
    public static final class AuditAction {
        public static final String BED_CREATE = "BED_CREATE";
        public static final String BED_DELETE = "BED_DELETE";
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

        private AuditAction() {}
    }

    /** Mã đối tượng audit/timeline tách khỏi mã quyền để giữ nguyên dữ liệu lịch sử. */
    public static final class EntityType {
        public static final String APPLICATION = "APPLICATION";
        public static final String BED = "BED";
        public static final String CONTRACT = "CONTRACT";
        public static final String INVOICE = "INVOICE";
        public static final String MAINTENANCE = "MAINTENANCE";
        public static final String NOTIFICATION = "NOTIFICATION";
        public static final String ROOM = "ROOM";
        public static final String USER = "USER";
        public static final String UTILITY_RATE = "UTILITY_RATE";

        private EntityType() {}
    }
}
