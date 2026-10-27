import type { Resource, Role } from '../../constants/authorization'

export type CrudRecord = Record<string, unknown> & { id?: number | string }
export interface CrudField {
  name: string
  label: string
  kind?:
    'text' | 'number' | 'date' | 'datetime' | 'textarea' | 'select' | 'boolean'
  required?: boolean
  maxLength?: number
  min?: number
  max?: number
  createOnly?: boolean
  editOnly?: boolean
  options?: { value: string | number; label: string }[]
  lookup?: string
  /** Bộ lọc lookup gửi trong body search-by-page, không ghép query vào đường dẫn API. */
  lookupFilters?: Record<string, string | number | boolean>
  lookupLabel?: string
  multiple?: boolean
  /** Kiểu JSON của bộ lọc khi control/URL giữ giá trị dạng chuỗi. */
  filterValueType?: 'string' | 'number' | 'boolean'
  visibleWhen?: { field: string; value: string }
  /** Gợi ý ngắn dưới trường nhập; nội dung tĩnh khai tại registry tài nguyên. */
  hint?: string
}

/** Cách hiển thị cột danh sách; dữ liệu vẫn lấy nguyên từ API. */
export type CrudColumnFormat = 'money' | 'number' | 'date' | 'datetime' | 'month'

export interface CrudColumn {
  key: string
  label: string
  format?: CrudColumnFormat
  align?: 'left' | 'center' | 'right'
}

/** Nhóm trường trong form thêm/sửa; thứ tự nhóm theo khai báo. */
export interface CrudFormSection {
  title: string
  note?: string
  fields: string[]
}

/** Mỗi màn chỉ khai báo API và trường được ghi; mẫu trang dùng chung không tự suy ra nghiệp vụ. */
export interface CrudResource {
  title: string
  /** Mã nghiệp vụ; READ/CREATE/UPDATE/DELETE được kiểm tra riêng từ /auth/me. */
  permission: Resource
  endpoint: string
  writeEndpoint?: string
  detailEndpoint?: string
  fields: CrudField[]
  /** Giá trị gợi ý của form tạo; dữ liệu nghiệp vụ chính thức vẫn do API kiểm tra. */
  createDefaults?: Record<string, unknown>
  /** Bộ lọc POST search-by-page; chỉ gửi các tên được màn khai báo. */
  filters?: CrudField[]
  columns: CrudColumn[]
  /** Trường chỉ hiện ở chi tiết, không được đưa vào payload thêm/sửa. */
  detailColumns?: { key: string; label: string }[]
  /** Dùng dữ liệu danh sách cho form khi tài nguyên không có API chi tiết riêng. */
  useListRecordForEdit?: boolean
  note?: string
  /** Nhãn mô tả ngắn dưới tiêu đề màn danh sách. */
  subtitle?: string
  /** Nhãn nút tạo mới; mặc định do trang dùng chung đặt. */
  createLabel?: string
  searchPlaceholder?: string
  emptyTitle?: string
  emptyHint?: string
  /** Nhóm trường trong form; trường không khai báo hiển thị ở nhóm còn lại. */
  formSections?: CrudFormSection[]
  /** Màn Tài chính dùng khung trình bày thống nhất cho danh sách và hộp thoại. */
  finance?: boolean
  editStates?: string[]
  deleteLabel?: string
  deleteAction?: string
  createFormat?: 'multipart'
  singleton?: boolean
  idKey?: string
  readOnly?: boolean
  rowOwned?: string
  createFields?: CrudField[]
  editFields?: CrudField[]
  /** Khi sửa, gửi lại các trường createOnly đã chọn lúc tạo (DTO sửa cần định danh/phạm vi). */
  keepCreateOnlyOnEdit?: boolean
  /** Sau thời điểm openAt chỉ được sửa closeAt (gia hạn). */
  closeAtOnlyAfterOpen?: boolean
  /** Không có nút tạo mới (bản ghi tồn tại sẵn, chỉ sửa). */
  noCreate?: boolean
  /** Dòng đã có đủ dữ liệu: không gọi API chi tiết khi mở xem/sửa. */
  inlineDetail?: boolean
  /** Màn nghiệp vụ riêng theo vai trò: nút Xem điều hướng tới `${đường dẫn}/${id}` thay vì mở modal. */
  detailRoutes?: Partial<Record<Role, string>>
}
