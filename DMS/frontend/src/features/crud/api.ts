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

export function crudRequestParams(query: CrudQuery, config: CrudResource) {
  const params: Record<string, string | number | undefined> = {
    page: query.page - 1,
    size: query.size,
    q: config.localSearch ? undefined : query.search,
  }
  for (const field of config.filters ?? []) {
    const value = query.filters[field.name]
    if (value)
      params[field.name] =
        field.kind === 'datetime' ? new Date(value).toISOString() : value
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
/** Tương thích danh mục dạng mảng, danh sách phân trang và hồ sơ đơn lẻ. */
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
    const { data } = await api.get(endpoint, { params: { page, size: 100 } })
    const result = normalizePage(data)
    all.push(...result.content)
    if (
      Array.isArray(data) ||
      all.length >= result.totalElements ||
      result.content.length === 0
    )
      break
    if (page === 99)
      console.warn(`Lookup ${endpoint} chạm trần 100 trang, danh sách bị cắt.`)
  }
  return all
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
