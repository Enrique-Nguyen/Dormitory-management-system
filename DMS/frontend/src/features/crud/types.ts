import type { Role } from '../../types/auth'

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
  endpoint: string
  writeEndpoint?: string
  detailEndpoint?: string
  fields: CrudField[]
  columns: { key: string; label: string }[]
  writeRoles?: Role[]
  createRoles?: Role[]
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
}
