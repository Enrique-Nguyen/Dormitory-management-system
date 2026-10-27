import axios from 'axios'
import type { ApiError } from '../services/api/client'
import { formatDate, formatDateTime } from './format'

// errorCode → câu tiếng Việt (UI 27.2, danh mục mã ở API Spec mục 5). Không parse chuỗi message.
// Mỗi câu nói rõ nguyên nhân → cách xử lý (Rule 06). Thêm mã vào đây cùng lúc với danh mục API Spec.
// Không đưa enum tiếng Anh (như currentStatus) vào câu hiển thị cho người dùng.
type Details = Record<string, unknown>

const messages: Record<string, (d: Details) => string> = {
  IMPORT_FILE_INVALID: () =>
    'File import không hợp lệ. Dùng file mẫu .xlsx, kiểm tra tên cột và giới hạn 5.000 dòng; không dùng công thức.',
  // 5.1 Chung
  IDENTITY_NOT_LINKED: () =>
    'Tài khoản đăng nhập chưa được cấp hồ sơ DMS. Liên hệ quản trị viên.',
  IDENTITY_NOT_CONFIGURED: () =>
    'Máy chủ chưa cấu hình kết nối quản lý tài khoản.',
  IDENTITY_UNAVAILABLE: () =>
    'Máy chủ quản lý tài khoản chưa sẵn sàng. Vui lòng thử lại.',
  DUPLICATE_USERNAME: () => 'Tên đăng nhập đã tồn tại.',
  UNAUTHENTICATED: () => 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
  SESSION_REVOKED: () =>
    'Phiên đã mất hiệu lực do thông tin bảo mật thay đổi. Vui lòng đăng nhập lại.',
  CSRF_TOKEN_INVALID: () =>
    'Phiên xác nhận thao tác đã thay đổi. Hãy tải lại trang rồi chủ động thử lại.',
  FORBIDDEN: () => 'Bạn không có quyền thực hiện thao tác này.',
  PERMISSION_MATRIX_STALE: () =>
    'Quyền của vai trò vừa được người khác cập nhật. Bảng đã được tải lại; hãy chọn lại các quyền cần sửa.',
  ADMIN_PERMISSIONS_IMMUTABLE: () =>
    'Quản trị viên luôn có toàn quyền, không thể thay đổi quyền của vai trò này.',
  PERMISSION_MATRIX_UNAVAILABLE: () =>
    'Chưa có bảng quyền của vai trò này. Liên hệ quản trị viên để kiểm tra dữ liệu phân quyền.',
  NOT_FOUND: () => 'Không tìm thấy dữ liệu hoặc bạn không có quyền xem.',
  VALIDATION_FAILED: () =>
    'Dữ liệu chưa hợp lệ. Kiểm tra các trường trong biểu mẫu.',
  RESOURCE_IN_USE: () =>
    'Dữ liệu đã có lịch sử hoặc đang được sử dụng, không thể sửa/xóa trực tiếp.',
  ROOM_OCCUPIED: () =>
    'Phòng đang có người ở hoặc giữ chỗ, không thể thực hiện thao tác này.',
  BED_OCCUPIED: () => 'Giường đang được sử dụng hoặc giữ chỗ.',
  ROOM_CODE_EXISTS: () => 'Mã phòng đã tồn tại trong tòa nhà.',
  SEMESTER_OVERLAP: () => 'Thời gian học kỳ trùng với học kỳ đã có.',
  RATE_ALREADY_EFFECTIVE: () => 'Đơn giá đã có hiệu lực. Hãy thêm bản giá mới.',
  READING_LOCKED: () => 'Chỉ số đã được đưa vào hóa đơn, không thể sửa/xóa.',
  READING_HAS_FOLLOWING_MONTH: () =>
    'Tháng sau đã có chỉ số, không thể thay đổi tháng trước.',
  READING_INVALID: () => 'Chỉ số cuối phải lớn hơn hoặc bằng chỉ số đầu.',
  DUPLICATE_DEPOSIT: () => 'Hợp đồng đã thu cọc ban đầu. Hãy tải lại sổ cọc.',
  DUPLICATE_DEDUCTION: () => 'Ticket này đã được trừ cọc cho hợp đồng.',
  DUPLICATE_REFUND: () => 'Hợp đồng đã hoàn cọc, không thể hoàn lần nữa.',
  REFUND_EXCEEDS_BALANCE: () => 'Số tiền hoàn vượt số dư cọc hiện tại.',
  NO_TOPUP_REQUIRED: () =>
    'Hợp đồng này không còn thiếu tiền cọc nên không cần nộp bù.',
  TOPUP_EXCEEDS_REQUIRED: () =>
    'Số tiền nộp bù vượt số cọc còn thiếu. Hãy nhập đúng số tiền còn thiếu.',
  CONTRACT_NOT_ACTIVE: () =>
    'Hợp đồng chưa hoặc không còn hiệu lực cho thao tác này.',
  DUPLICATE_INVOICE: () => 'Sinh viên/phòng đã có hóa đơn của kỳ này.',
  CONTRACT_EXISTS: () => 'Sinh viên đã có hợp đồng đang chờ hoặc đang ở.',
  INVALID_STATE_TRANSITION: () =>
    'Thao tác không còn hợp lệ vì trạng thái đã thay đổi. Trang đã được tải lại.',
  CONCURRENT_UPDATE: () =>
    'Dữ liệu vừa được người khác thay đổi. Vui lòng thử lại.',
  FILE_TOO_LARGE: (d) =>
    `File vượt quá ${d.maxMb ?? ''} MB. Vui lòng chọn file nhỏ hơn.`,
  DUPLICATE_VALUE: () => 'Giá trị này đã tồn tại. Vui lòng nhập giá trị khác.',
  METHOD_NOT_ALLOWED: () =>
    'Thao tác không được hỗ trợ. Hãy tải lại trang rồi thử lại.',
  UNSUPPORTED_MEDIA_TYPE: () =>
    'Định dạng dữ liệu gửi lên không được hỗ trợ. Hãy tải lại trang rồi thử lại.',
  INTERNAL_ERROR: (d) =>
    `Hệ thống gặp lỗi. Vui lòng thử lại sau (mã yêu cầu: ${d.requestId ?? 'không có'}).`,

  // 5.2 Tài khoản
  INVALID_CREDENTIALS: (d) =>
    d.remainingAttempts !== undefined
      ? `Tên đăng nhập hoặc mật khẩu không đúng. Còn ${d.remainingAttempts} lần thử trước khi tài khoản bị tạm khóa.`
      : 'Tên đăng nhập hoặc mật khẩu không đúng.',
  ACCOUNT_TEMP_LOCKED: (d) =>
    `Tài khoản tạm khóa${d.lockedUntil ? ` đến ${formatDateTime(String(d.lockedUntil))}` : ' 15 phút'} do nhập sai nhiều lần. Vui lòng thử lại sau.`,
  ACCOUNT_LOCKED: () =>
    'Tài khoản đã bị khóa. Liên hệ quản trị viên để được mở khóa.',
  PASSWORD_CHANGE_REQUIRED: () => 'Bạn cần đổi mật khẩu trước khi tiếp tục.',
  WRONG_CURRENT_PASSWORD: () =>
    'Mật khẩu hiện tại không đúng. Vui lòng nhập lại.',
  PASSWORD_POLICY: () =>
    'Mật khẩu mới phải có 8–64 ký tự, gồm cả chữ và số, và khác mật khẩu cũ.',
  USERNAME_EXISTS: (d) =>
    `Tên đăng nhập ${d.username ?? ''} đã tồn tại. Vui lòng chọn tên khác.`,

  STUDENT_CODE_EXISTS: () =>
    'Mã sinh viên đã tồn tại. Kiểm tra lại mã hoặc tìm hồ sơ đã có.',
  STUDENT_HAS_ACTIVE_CONTRACT: () =>
    'Sinh viên đang có chỗ ở hoặc giữ chỗ nên không đổi được giới tính. Hãy trả phòng trước.',
  CROSS_UNIT_ONLY_STAFF: () =>
    'Quyền tổng hợp chỉ cấp được cho tài khoản Staff.',
  CONFIG_VALUE_INVALID: (d) =>
    `Giá trị không hợp lệ${d.key ? ` cho ${String(d.key)}` : ''}. Kiểm tra lại rồi lưu.`,

  // 5.3 Phòng, giường
  BED_CODE_EXISTS: () => 'Mã giường đã có trong phòng này.',
  LEADER_ALREADY_ASSIGNED: (d) =>
    `Sinh viên đang là trưởng phòng của phòng${d.roomCode ? ` ${String(d.roomCode)}` : ' khác'}. Gỡ vai trò đó trước.`,
  LEADER_NOT_STAYING: (d) =>
    `Sinh viên này không đang ở trong phòng${d.roomCode ? ` ${String(d.roomCode)}` : ''}. Chọn sinh viên đang ở phòng.`,
  NOT_STAYING: () =>
    'Bạn hiện không ở trong ký túc xá nên không dùng được chức năng này.',
  BED_TAKEN: (d) =>
    `Giường ${d.bedCode ?? ''} vừa được phân cho sinh viên khác. Vui lòng chọn giường khác.`,
  BED_NOT_AVAILABLE: (d) =>
    `Giường ${d.bedCode ?? ''} hoặc phòng đang không hoạt động. Vui lòng chọn giường khác.`,
  GENDER_MISMATCH: (d) =>
    `Phòng${d.roomCode ? ` ${String(d.roomCode)}` : ''} dành cho giới tính khác, không phù hợp với sinh viên. Chọn phòng khác.`,
  STUDENT_ALREADY_ASSIGNED: () =>
    'Sinh viên đã có chỗ ở hoặc đang giữ chỗ. Tải lại trang để xem phân phòng hiện tại.',
  TRANSFER_CROSS_UNIT: () =>
    'Chỉ chuyển sang giường cùng đơn vị quản lý. Chọn giường khác.',
  SAME_BED: () => 'Giường đích trùng giường hiện tại. Chọn giường khác.',

  // 5.4+ Đăng ký, kỳ hè, bảo trì, hóa đơn
  PERIOD_NOT_OPEN: () =>
    'Đợt đăng ký chưa mở hoặc đã đóng. Hãy chờ đợt tiếp theo hoặc liên hệ Ban quản lý.',
  UNIT_NOT_IN_PERIOD: () =>
    'Đợt này không áp dụng cho đơn vị đã chọn. Hãy chọn đơn vị khác.',
  APPLICATION_HAS_CONTRACT: () =>
    'Đơn đã sinh hợp đồng nên không thể hủy. Hãy liên hệ Ban quản lý để xử lý hợp đồng.',
  PHOTO_INVALID: () =>
    'Ảnh không hợp lệ. Chỉ nhận tối đa 3 ảnh JPEG, PNG hoặc WebP, mỗi ảnh không quá 5 MB.',
  PHOTO_UPLOAD_FAILED: () => 'Không lưu được ảnh. Vui lòng thử lại.',
  TICKET_LOCATION_INVALID: () =>
    'Chọn một phòng hoặc một khu vực chung (tòa và tầng), không chọn cả hai.',
  TECHNICIAN_OUT_OF_UNIT: () =>
    'Kỹ thuật viên không thuộc đơn vị của ticket. Chọn kỹ thuật viên khác.',
  INVOICE_ALREADY_PAID: () => 'Hóa đơn đã được xác nhận thanh toán trước đó.',
  APPLICATION_EXISTS: () =>
    'Bạn đã có đơn trong đợt này. Hãy mở đơn hiện có thay vì tạo đơn mới.',
  SUMMER_CHOICE_EXISTS: () =>
    'Bạn đã gửi lựa chọn cho kỳ hè này. Hãy mở lựa chọn hiện có để theo dõi.',
  STORAGE_RECORD_EXISTS: () =>
    'Đã có phiếu gửi đồ chưa nhận lại. Hãy hoàn tất phiếu đó trước khi tạo phiếu mới.',
  TICKET_ALREADY_CONCLUDED: () =>
    'Ticket đã được kết luận nguyên nhân và chi phí. Hãy tải lại chi tiết ticket.',
  COMMON_AREA_NO_CHARGE: () =>
    'Ticket khu vực chung không tính chi phí cho sinh viên.',
  COST_INVALID: (d) =>
    `Chi phí kết luận không hợp lệ${d.repairCost ? ` (${String(d.repairCost)})` : ''}. Hư hỏng do sử dụng cần chi phí lớn hơn 0; hao mòn tự nhiên cần ghi chú và không có chi phí.`,
  CHARGED_STUDENTS_INVALID: () =>
    'Chọn ít nhất một sinh viên đang ở trong phòng lúc tạo ticket; không chọn người ngoài danh sách.',
  NO_OCCUPANT_TO_CHARGE: () =>
    'Không có sinh viên nào ở trong phòng tại thời điểm tạo ticket để trừ cọc.',
  MOVE_OUT_WARNINGS_NOT_ACKNOWLEDGED: () =>
    'Còn công nợ hoặc ticket chưa kết luận chi phí. Hãy xem cảnh báo và xác nhận trước khi trả phòng.',
  SUMMER_CHOICE_NOT_APPROVED: () =>
    'Lựa chọn tạm rời hè chưa được duyệt. Hãy chờ Staff duyệt lựa chọn trước khi gửi đồ.',
  TEMP_LEAVE_NOT_EXTENDED: () =>
    'Hợp đồng chưa được gia hạn sang năm học sau. Hãy hoàn tất gia hạn trước khi tạo phiếu gửi đồ.',
  DEPOSIT_INVALID: () =>
    'Giao dịch tiền cọc không hợp lệ. Hãy kiểm tra loại giao dịch và số tiền rồi thử lại.',
  RATE_EXISTS: (d) =>
    typeof d.effectiveFrom === 'string'
      ? `Đã có đơn giá áp dụng từ ngày ${formatDate(d.effectiveFrom)}. Hãy chọn ngày hiệu lực khác.`
      : 'Đã có đơn giá áp dụng từ ngày này. Hãy chọn ngày hiệu lực khác.',
  REQUEST_PENDING_EXISTS: () =>
    'Hợp đồng này đã có một yêu cầu đang chờ xử lý. Hãy chờ kết quả hoặc hủy yêu cầu cũ.',
  IDENTITY_ALREADY_LINKED: () =>
    'Tài khoản này đã được liên kết với một danh tính đăng nhập khác. Hãy kiểm tra lại với quản trị viên.',
  BOOTSTRAP_USERNAME_IN_USE: () =>
    'Tên đăng nhập khởi tạo đã tồn tại trên hệ thống định danh. Hãy dùng tên khác hoặc liên hệ quản trị viên.',
}

export function errorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiError>(error) && error.response?.data?.errorCode) {
    const { errorCode, details, requestId } = error.response.data
    const build = messages[errorCode]
    if (build) return build({ ...details, requestId })
    // Mã chưa có câu tiếng Việt: không hiện chuỗi mã thô (backend đặt message = errorCode).
    return `Không thực hiện được thao tác (mã: ${errorCode}). Vui lòng thử lại hoặc liên hệ Ban quản lý.`
  }
  if (axios.isAxiosError(error)) {
    // Có phản hồi nhưng không theo khuôn { errorCode }: CSRF bị từ chối, proxy 502/504...
    if (error.response) {
      return `Máy chủ phản hồi không như mong đợi (HTTP ${error.response.status}). Hãy tải lại trang rồi thử lại; nếu vẫn lỗi, liên hệ Ban quản lý.`
    }
    return 'Không kết nối được tới máy chủ. Kiểm tra kết nối mạng rồi thử lại.'
  }
  return 'Đã xảy ra lỗi ngoài dự kiến trên trang. Hãy tải lại trang rồi thử lại.'
}

/** Mã lỗi của response (nếu có), để rẽ nhánh xử lý ngoài việc hiện câu thông báo. */
export function errorCodeOf(error: unknown): string | undefined {
  return axios.isAxiosError<ApiError>(error)
    ? error.response?.data?.errorCode
    : undefined
}
