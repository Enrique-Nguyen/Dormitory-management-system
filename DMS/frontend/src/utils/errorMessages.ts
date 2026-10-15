import axios from 'axios'
import type { ApiError } from '../services/api/client'
import { formatDateTime } from './format'

// errorCode → câu tiếng Việt (UI 27.2, danh mục mã ở API Spec mục 5). Không parse chuỗi message.
// Mỗi câu nói rõ nguyên nhân → cách xử lý (Rule 06). Thêm mã vào đây cùng lúc với danh mục API Spec.
// Không đưa enum tiếng Anh (như currentStatus) vào câu hiển thị cho người dùng.
type Details = Record<string, unknown>

const messages: Record<string, (d: Details) => string> = {
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
  DUPLICATE_INVOICE: () => 'Sinh viên/phòng đã có hóa đơn của kỳ này.',
  CONTRACT_EXISTS: () => 'Sinh viên đã có hợp đồng đang chờ hoặc đang ở.',
  INVALID_STATE_TRANSITION: () =>
    'Thao tác không còn hợp lệ vì trạng thái đã thay đổi. Trang đã được tải lại.',
  CONCURRENT_UPDATE: () =>
    'Dữ liệu vừa được người khác thay đổi. Vui lòng thử lại.',
  FILE_TOO_LARGE: (d) =>
    `File vượt quá ${d.maxMb ?? ''} MB. Vui lòng chọn file nhỏ hơn.`,
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

  // 5.3 Phòng, giường
  BED_TAKEN: (d) =>
    `Giường ${d.bedCode ?? ''} vừa được phân cho sinh viên khác. Vui lòng chọn giường khác.`,
  BED_NOT_AVAILABLE: (d) =>
    `Giường ${d.bedCode ?? ''} hoặc phòng đang không hoạt động. Vui lòng chọn giường khác.`,
}

export function errorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiError>(error) && error.response?.data?.errorCode) {
    const { errorCode, details, requestId } = error.response.data
    const build = messages[errorCode]
    if (build) return build({ ...details, requestId })
    return error.response.data.message
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
