import type { Resource } from '../../constants/authorization'

export type CrudRecord = Record<string, unknown> & { id?: number | string }
export interface CrudField {
  name: string
  label: string
  kind?: 'text' | 'number' | 'date' | 'datetime' | 'textarea' | 'select' | 'boolean'
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
  /** Mã nghiệp vụ; READ/CREATE/UPDATE/DELETE được kiểm tra riêng từ /auth/me. */
  permission: Resource
  endpoint: string
  writeEndpoint?: string
  detailEndpoint?: string
  fields: CrudField[]
  columns: { key: string; label: string }[]
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
}
