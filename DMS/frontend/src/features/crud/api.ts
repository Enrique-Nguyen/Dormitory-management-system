import { api } from '../../services/api/client'
import type { CrudRecord } from './types'

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
  }
  return all
}

export async function writeCrudRecord(
  endpoint: string,
  method: 'post' | 'patch' | 'put' | 'delete',
  values?: unknown,
) {
  // Sau khi tải lại trang, cookie CSRF có thể đã hết hạn; lấy token trước mọi thao tác ghi.
  await api.get('/auth/csrf')
  return (await api.request({ url: endpoint, method, data: values }))
    .data as unknown
}
