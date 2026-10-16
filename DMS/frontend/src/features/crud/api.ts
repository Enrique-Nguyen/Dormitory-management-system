import { api } from '../../services/api/client'
import axios from 'axios'
import type { CrudRecord, CrudResource } from './types'

export const CRUD_PAGE_SIZES = [10, 20, 50, 100]
export const CRUD_DEFAULT_PAGE_SIZE = 20

export interface CrudQuery {
  search: string
  page: number
  size: number
  filters: Record<string, string>
}

/** Mỗi tab/màn có khóa riêng, giữ tìm kiếm khi quay lại và không trộn bộ lọc của màn khác. */
export function readCrudQuery(
  params: URLSearchParams,
  resource: string,
  config: CrudResource,
): CrudQuery {
  const prefix = `${resource}.`
  const requestedPage = Number(params.get(`${prefix}page`) ?? 1)
  const requestedSize = Number(
    params.get(`${prefix}size`) ?? CRUD_DEFAULT_PAGE_SIZE,
  )
  const size = CRUD_PAGE_SIZES.includes(requestedSize)
    ? requestedSize
    : CRUD_DEFAULT_PAGE_SIZE
  const filters: Record<string, string> = {}
  for (const field of config.filters ?? []) {
    const value = params.get(`${prefix}${field.name}`)?.trim()
    if (!value) continue
    if (
      field.options &&
      !field.options.some((option) => String(option.value) === value)
    )
      continue
    if (
      (field.kind === 'number' || field.lookup) &&
      (!Number.isSafeInteger(Number(value)) || Number(value) < (field.min ?? 1))
    )
      continue
    if (
      ['date', 'datetime'].includes(field.kind ?? '') &&
      !Number.isFinite(new Date(value).getTime())
    )
      continue
    filters[field.name] =
      field.kind === 'number' || field.lookup
        ? String(Number(value))
        : field.maxLength
          ? value.slice(0, field.maxLength)
          : value
  }
  return {
    search: params.get(`${prefix}q`)?.trim() ?? '',
    page:
      Number.isSafeInteger(requestedPage) &&
      requestedPage >= 1 &&
      (requestedPage - 1) * size <= 2147483647
        ? requestedPage
        : 1,
    size,
    filters,
  }
}

/** Chỉ cập nhật trạng thái của màn hiện tại; giữ query của các tab và chức năng khác. */
export function updateCrudQuery(
  previous: URLSearchParams,
  resource: string,
  query: CrudQuery,
): URLSearchParams {
  const params = new URLSearchParams(previous)
  const prefix = `${resource}.`
  for (const key of [...params.keys()])
    if (key.startsWith(prefix)) params.delete(key)
  if (query.search) params.set(`${prefix}q`, query.search)
  if (query.page !== 1) params.set(`${prefix}page`, String(query.page))
  if (query.size !== CRUD_DEFAULT_PAGE_SIZE)
    params.set(`${prefix}size`, String(query.size))
  for (const [name, value] of Object.entries(query.filters))
    if (value !== '') params.set(`${prefix}${name}`, value)
  return params
}

/** datetime-local dùng múi giờ trình duyệt; API nhận ISO có múi giờ rõ ràng. */
export function toLocalDateTime(value: unknown): string {
  const date = new Date(String(value))
  if (!Number.isFinite(date.getTime())) return ''
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16)
}

/** date dùng định dạng YYYY-MM-DD cho input[type="date"]; hỗ trợ chuỗi ISO, Date và timestamp. */
export function toLocalDateString(value: unknown): string {
  if (value == null || value === '') return ''
  if (typeof value === 'string') {
    if (/^\d{4}-\d{2}-\d{2}/.test(value)) return value.slice(0, 10)
    const date = new Date(value)
    if (Number.isFinite(date.getTime())) {
      return date.toISOString().slice(0, 10)
    }
    return ''
  }
  if (value instanceof Date && Number.isFinite(value.getTime())) {
    return value.toISOString().slice(0, 10)
  }
  if (typeof value === 'number' && Number.isFinite(value)) {
    const date = new Date(value)
    return Number.isFinite(date.getTime())
      ? date.toISOString().slice(0, 10)
      : ''
  }
  return ''
}

/**
 * Chuyển đổi dữ liệu bản ghi API sang định dạng hiển thị và chỉnh sửa của Ant Design Form.
 * Giữ nguyên giá trị boolean false, số 0, chuỗi rỗng tùy chọn và ánh xạ quan hệ lồng nhau.
 */
export function getRecordFormValues(
  record: CrudRecord,
  config: CrudResource,
  resource: string,
): Record<string, unknown> {
  const initial: Record<string, unknown> = { ...record }
  const allFields = [
    ...(config.fields ?? []),
    ...(config.editFields ?? []),
    ...(config.createFields ?? []),
  ]

  for (const field of allFields) {
    let val = initial[field.name]

    // Ánh xạ khóa ngoại lồng nhau: ví dụ studentId từ student.id hoặc unitId từ unit.id
    if (val == null && field.name.endsWith('Id')) {
      const objKey = field.name.slice(0, -2)
      const objVal = initial[objKey]
      if (
        objVal &&
        typeof objVal === 'object' &&
        'id' in (objVal as Record<string, unknown>)
      ) {
        val = (objVal as Record<string, unknown>).id
        initial[field.name] = val
      }
    }

    // Nếu val là đối tượng chứa id (select lookup đơn)
    if (
      val != null &&
      field.kind === 'select' &&
      !field.multiple &&
      typeof val === 'object' &&
      !Array.isArray(val) &&
      'id' in (val as Record<string, unknown>)
    ) {
      val = (val as Record<string, unknown>).id
      initial[field.name] = val
    }

    // Nếu là select multiple có danh sách đối tượng
    if (field.multiple && Array.isArray(val)) {
      initial[field.name] = val.map((item) =>
        item && typeof item === 'object' && 'id' in item
          ? (item as Record<string, unknown>).id
          : item,
      )
    } else if (val != null) {
      if (field.kind === 'datetime') {
        initial[field.name] = toLocalDateTime(val)
      } else if (field.kind === 'date') {
        initial[field.name] = toLocalDateString(val)
      }
    }
  }

  // Ánh xạ diện ưu tiên cho màn hình đơn đăng ký
  if (
    ['myApplications', 'applications'].includes(resource) &&
    Array.isArray(record.priorities)
  ) {
    initial.priorityCategoryIds = record.priorities
      .map((p) => {
        const item = p as Record<string, unknown>
        return (
          item.categoryId ??
          (typeof item.category === 'object' &&
          item.category &&
          'id' in item.category
            ? (item.category as Record<string, unknown>).id
            : undefined)
        )
      })
      .filter((id) => id != null)
  }

  return initial
}

/** Gửi điều kiện tìm kiếm có kiểu rõ ràng; trang API bắt đầu từ 0. */
export function createSearchRequest(query: CrudQuery, config: CrudResource) {
  const params: Record<string, string | number | boolean> = {
    page: query.page - 1,
    size: query.size,
    q: query.search,
  }
  for (const field of config.filters ?? []) {
    const value = query.filters[field.name]
    if (!value) continue
    params[field.name] =
      field.kind === 'datetime'
        ? new Date(value).toISOString()
        : field.filterValueType === 'boolean'
          ? value === 'true'
          : field.filterValueType === 'number' ||
              field.kind === 'number' ||
              field.lookup
            ? Number(value)
            : value
  }
  return params
}

/** Chỉ đọc lỗi trường đúng định dạng; không để response lỗi bất thường làm hỏng form. */
export function validationFieldErrors(
  error: unknown,
): { field: string; message: string }[] {
  if (!axios.isAxiosError(error)) return []
  const fields: unknown = error.response?.data?.details?.fieldErrors
  if (!Array.isArray(fields)) return []
  return fields.filter(
    (item): item is { field: string; message: string } =>
      !!item &&
      typeof item.field === 'string' &&
      typeof item.message === 'string',
  )
}

export interface CrudPage {
  content: CrudRecord[]
  totalElements: number
}
/** Chuẩn hóa response phân trang và hồ sơ đơn lẻ. */
export function normalizePage(data: unknown): CrudPage {
  if (Array.isArray(data))
    return { content: data as CrudRecord[], totalElements: data.length }
  if (data && typeof data === 'object') {
    const value = data as Record<string, unknown>
    if (Array.isArray(value.content))
      return {
        content: value.content as CrudRecord[],
        totalElements: Number(value.totalElements ?? value.content.length),
      }
    return { content: [value as CrudRecord], totalElements: 1 }
  }
  return { content: [], totalElements: 0 }
}

export function readValue(row: CrudRecord, path: string): unknown {
  return path
    .split('.')
    .reduce<unknown>(
      (value, part) =>
        value && typeof value === 'object'
          ? (value as Record<string, unknown>)[part]
          : undefined,
      row,
    )
}

/** Lookup tải đủ các trang được phép, để chọn tham chiếu mà không phải gõ ID. */
export async function getLookupRecords(
  endpoint: string,
): Promise<CrudRecord[]> {
  const all: CrudRecord[] = []
  for (let page = 0; page < 100; page++) {
    const result = await searchByPage(endpoint, { page, size: 100 })
    all.push(...result.content)
    if (all.length >= result.totalElements || result.content.length === 0) break
    if (page === 99)
      console.warn(`Lookup ${endpoint} chạm trần 100 trang, danh sách bị cắt.`)
  }
  return all
}

/** Danh sách luôn đi qua API tìm kiếm và phân trang tại máy chủ. */
export async function searchByPage(
  endpoint: string,
  request: Record<string, unknown>,
): Promise<CrudPage> {
  return normalizePage(
    (await api.post(`${endpoint}/search-by-page`, request)).data,
  )
}

/** Một API lưu chung; body có id thì sửa, không có id thì tạo. */
export async function saveOrUpdate(
  endpoint: string,
  values: unknown,
): Promise<unknown> {
  return (await api.post(`${endpoint}/save-or-update`, values)).data
}

export async function writeCrudRecord(
  endpoint: string,
  method: 'post' | 'patch' | 'put' | 'delete',
  values?: unknown,
) {
  // Axios gắn Bearer token Keycloak; backend kiểm quyền và phạm vi ở từng thao tác.
  return (await api.request({ url: endpoint, method, data: values }))
    .data as unknown
}
