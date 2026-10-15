import type { Resource, Role } from '../../constants/authorization'

export type CrudRecord = Record<string, unknown> & { id?: number | string }
export interface CrudField {
  name: string
  label: string
  kind?:
    | 'text'
    | 'number'
    | 'date'
    | 'datetime'
    | 'textarea'
    | 'select'
    | 'boolean'
  required?: boolean
  /** Khi sửa, giá trị rỗng gửi null để xóa dữ liệu (chỉ dùng khi backend phân biệt null và vắng mặt). */
  clearable?: boolean
  maxLength?: number
  min?: number
  max?: number
  createOnly?: boolean
  editOnly?: boolean
  options?: { value: string | number; label: string }[]
  lookup?: string
  lookupLabel?: string
  multiple?: boolean
  visibleWhen?: { field: string; value: string }
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
  /** Bộ lọc GET được màn khai báo; chỉ gửi các tên này, không nhận tên cột tùy ý từ URL. */
  filters?: CrudField[]
  columns: { key: string; label: string }[]
  /** Trường chỉ hiện ở chi tiết, không được đưa vào payload thêm/sửa. */
  detailColumns?: { key: string; label: string }[]
  localSearch?: boolean
  note?: string
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
  /** Phương thức ghi khi sửa; mặc định PATCH. */
  writeMethod?: 'put'
  /** Không có nút tạo mới (bản ghi tồn tại sẵn, chỉ sửa). */
  noCreate?: boolean
  /** Dòng đã có đủ dữ liệu: không gọi API chi tiết khi mở xem/sửa. */
  inlineDetail?: boolean
  /** Màn nghiệp vụ riêng theo vai trò: nút Xem điều hướng tới `${đường dẫn}/${id}` thay vì mở modal. */
  detailRoutes?: Partial<Record<Role, string>>
}
