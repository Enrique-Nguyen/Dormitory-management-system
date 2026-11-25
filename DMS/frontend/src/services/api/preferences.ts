import type { AccentKey, ThemeMode } from '../../app/theme'
import { api } from './client'

// Hợp đồng DMS-PREF-01: tùy chọn giao diện của chính tài khoản đang đăng nhập.
// Backend luôn lấy danh tính từ token nên client không gửi userId/owner trong body.

/** Tùy chọn giao diện đã lưu trên máy chủ kèm version chống ghi đè. */
export interface UserPreferences {
  themeMode: ThemeMode
  themePreset: AccentKey
  tableDense: boolean
  /** Version bản ghi hiện tại; gửi lại khi lưu để backend phát hiện xung đột. */
  version: number
}

/** Body của POST /api/me/preferences/save-or-update. */
export interface SaveUserPreferencesInput {
  themeMode: ThemeMode
  themePreset: AccentKey
  tableDense: boolean
  version: number
}

/** Lấy tùy chọn giao diện đã lưu của tài khoản hiện tại (GET /api/me/preferences). */
export async function getPreferences(): Promise<UserPreferences> {
  return (await api.get<UserPreferences>('/me/preferences')).data
}

/**
 * Lưu tùy chọn giao diện của tài khoản hiện tại
 * (POST /api/me/preferences/save-or-update). Version cũ trả HTTP 409 CONCURRENT_UPDATE.
 */
export async function savePreferences(
  input: SaveUserPreferencesInput,
): Promise<UserPreferences> {
  return (
    await api.post<UserPreferences>('/me/preferences/save-or-update', input)
  ).data
}
