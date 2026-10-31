import { useSyncExternalStore } from 'react'
import {
  ACCENT_PRESETS,
  getAntdThemeConfig,
  initializeTheme,
  type AccentKey,
  type ThemeMode,
} from './theme'

/**
 * STORE QUẢN LÝ TÙY CHỌN GIAO DIỆN CÁ NHÂN HÓA CHO TỪNG NGƯỜI DÙNG (CHUẨN REACT 19)
 *
 * - Giữ trạng thái hiện tại trong bộ nhớ; ThemePreferenceBridge chịu trách nhiệm tải từ
 *   máy chủ và lưu thay đổi (debounce, xử lý 409, đồng bộ nhiều tab).
 * - Cache localStorage được namespace theo id tài khoản: `dms_user_theme_preferences:<id>`.
 *   Không đọc cache của tài khoản khác; bỏ qua khóa chung cũ `dms_user_theme_preferences`
 *   vì không xác định được chủ sở hữu.
 * - `version` là version bản ghi máy chủ gần nhất mà tab này biết, dùng khi gửi
 *   save-or-update để backend phát hiện xung đột.
 */

export interface ThemePreferences {
  mode: ThemeMode
  accent: AccentKey
  tableDense: boolean
}

/** Tùy chọn kèm version bản ghi máy chủ mà tab hiện tại đang biết. */
export interface ThemePreferencesState extends ThemePreferences {
  version: number
}

/** Đầu vào hydrate: dữ liệu từ cache của tài khoản hoặc từ GET /api/me/preferences. */
export interface ThemePreferencesHydration extends ThemePreferences {
  version?: number
}

/** Giao diện an toàn trước khi biết tài khoản: trang đăng nhập không lộ theme tài khoản cũ. */
export const DEFAULT_THEME_PREFERENCES: Readonly<ThemePreferences> = Object.freeze({
  mode: 'light',
  accent: 'blue',
  tableDense: false,
})

const STORAGE_KEY_PREFIX = 'dms_user_theme_preferences'

let activeUserId: number | null = null
let version = 0
let userChangeEpoch = 0
let currentPreferences: ThemePreferences = { ...DEFAULT_THEME_PREFERENCES }

const listeners = new Set<() => void>()
const preferenceSaveListeners = new Set<() => void>()

/** Khóa cache riêng của một tài khoản; id phải lấy từ /api/auth/me của phiên hiện tại. */
export function userPreferencesStorageKey(userId: number): string {
  return `${STORAGE_KEY_PREFIX}:${userId}`
}

function isThemeMode(value: unknown): value is ThemeMode {
  return value === 'light' || value === 'dark'
}

function isAccentKey(value: unknown): value is AccentKey {
  return typeof value === 'string' && Object.hasOwn(ACCENT_PRESETS, value)
}

function normalizeVersion(value: unknown): number {
  return typeof value === 'number' && Number.isInteger(value) && value >= 0
    ? value
    : 0
}

/** Đọc cache của đúng tài khoản; trả null nếu chưa có hoặc dữ liệu không hợp lệ. */
export function readCachedPreferences(
  userId: number,
): ThemePreferencesState | null {
  if (typeof window === 'undefined') return null
  try {
    const raw = window.localStorage.getItem(userPreferencesStorageKey(userId))
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<ThemePreferencesState> | null
    if (!parsed || !isThemeMode(parsed.mode) || !isAccentKey(parsed.accent))
      return null
    return {
      mode: parsed.mode,
      accent: parsed.accent,
      tableDense: parsed.tableDense === true,
      version: normalizeVersion(parsed.version),
    }
  } catch {
    return null
  }
}

/** Ghi cache của tài khoản đang hoạt động; bỏ qua khi chưa biết tài khoản. */
function persistCurrentPreferences() {
  if (activeUserId === null || typeof window === 'undefined') return
  try {
    const key = userPreferencesStorageKey(activeUserId)
    const state: ThemePreferencesState = { ...currentPreferences, version }
    const raw = JSON.stringify(state)
    // Không ghi lại giá trị y hệt để tránh vòng lặp sự kiện storage giữa các tab.
    if (window.localStorage.getItem(key) === raw) return
    window.localStorage.setItem(key, raw)
  } catch {
    // Trình duyệt chặn localStorage: tùy chọn vẫn dùng được trong phiên hiện tại.
  }
}

function notifyUi() {
  // Cập nhật CSS Variables và class dark trước khi component React render lại.
  initializeTheme(currentPreferences.mode, currentPreferences.accent)
  listeners.forEach((listener) => listener())
}

/** Áp giá trị mới cho giao diện; trả về true nếu mode/accent/tableDense thực sự đổi. */
function applyPreferences(next: ThemePreferences): boolean {
  if (
    next.mode === currentPreferences.mode &&
    next.accent === currentPreferences.accent &&
    next.tableDense === currentPreferences.tableDense
  )
    return false
  currentPreferences = {
    mode: next.mode,
    accent: next.accent,
    tableDense: next.tableDense,
  }
  notifyUi()
  return true
}

function subscribe(callback: () => void) {
  listeners.add(callback)
  return () => {
    listeners.delete(callback)
  }
}

function getSnapshot(): ThemePreferences {
  return currentPreferences
}

function getServerSnapshot(): ThemePreferences {
  return DEFAULT_THEME_PREFERENCES
}

/**
 * Đăng ký nhận tín hiệu "người dùng vừa đổi tùy chọn" để bridge lưu lên máy chủ.
 * Hydrate từ cache/máy chủ và reset cố ý không phát tín hiệu, tránh lưu lặp (echo).
 */
export function subscribePreferenceSaves(listener: () => void) {
  preferenceSaveListeners.add(listener)
  return () => {
    preferenceSaveListeners.delete(listener)
  }
}

/** Chuyển sang tài khoản khác (hoặc null khi đăng xuất); version của tài khoản cũ bị bỏ. */
export function setActiveUser(userId: number | null) {
  if (activeUserId === userId) return
  activeUserId = userId
  version = 0
}

/**
 * Áp tùy chọn tải từ cache của tài khoản hoặc từ máy chủ.
 * Ghi lại vào cache của tài khoản đang hoạt động nhưng không phát tín hiệu lưu.
 */
export function hydratePreferences(prefs: ThemePreferencesHydration) {
  const next: ThemePreferences = {
    mode: isThemeMode(prefs.mode)
      ? prefs.mode
      : DEFAULT_THEME_PREFERENCES.mode,
    accent: isAccentKey(prefs.accent)
      ? prefs.accent
      : DEFAULT_THEME_PREFERENCES.accent,
    tableDense: prefs.tableDense === true,
  }
  if (prefs.version !== undefined) version = normalizeVersion(prefs.version)
  applyPreferences(next)
  persistCurrentPreferences()
}

/** Đưa giao diện về mặc định an toàn (đăng xuất/đổi tài khoản); không xóa cache tài khoản cũ. */
export function resetToDefaults() {
  version = 0
  applyPreferences(DEFAULT_THEME_PREFERENCES)
}

/** Áp một thay đổi do người dùng, ghi cache tài khoản và phát tín hiệu lưu. */
function applyUserPreferences(patch: Partial<ThemePreferences>) {
  const changed = applyPreferences({ ...currentPreferences, ...patch })
  if (!changed || activeUserId === null) return
  userChangeEpoch += 1
  persistCurrentPreferences()
  preferenceSaveListeners.forEach((listener) => listener())
}

export function setThemeMode(mode: ThemeMode) {
  applyUserPreferences({ mode })
}

export function setAccentColor(accent: AccentKey) {
  applyUserPreferences({ accent })
}

export function toggleThemeMode() {
  setThemeMode(currentPreferences.mode === 'light' ? 'dark' : 'light')
}

export function setTableDensity(tableDense: boolean) {
  applyUserPreferences({ tableDense })
}

/**
 * Cập nhật version sau khi máy chủ xác nhận lưu thành công cho đúng tài khoản.
 * Ghi kèm cache để tab khác nhận được version mới qua sự kiện storage và tránh 409 oan.
 */
export function commitPreferencesVersion(userId: number, savedVersion: number) {
  if (activeUserId !== userId) return
  version = normalizeVersion(savedVersion)
  persistCurrentPreferences()
}

/** Tài khoản đang hoạt động; null khi chưa đăng nhập hoặc vừa đăng xuất. */
export function getActiveUserId(): number | null {
  return activeUserId
}

/** Ảnh chụp tùy chọn hiện tại, dùng cho payload lưu ở thời điểm debounce chạy. */
export function getCurrentPreferences(): ThemePreferences {
  return currentPreferences
}

/** Version bản ghi máy chủ mà tab hiện tại đang biết. */
export function getPreferencesVersion(): number {
  return version
}

/**
 * Số lần người dùng đã chủ động đổi tùy chọn trong tab này. Hydrate từ cache/máy chủ không tăng,
 * nên bridge dùng được để không ghi đè thay đổi vừa bấm trong lúc request cũ còn đang bay.
 */
export function getUserChangeEpoch(): number {
  return userChangeEpoch
}

/** Hook phản ứng cho giao diện: lấy theme hiện tại và các hàm đổi màu. */
export function useAppTheme() {
  const prefs = useSyncExternalStore(
    subscribe,
    getSnapshot,
    getServerSnapshot,
  )

  return {
    ...prefs,
    setMode: setThemeMode,
    setAccent: setAccentColor,
    toggleMode: toggleThemeMode,
    setTableDense: setTableDensity,
    antdThemeConfig: getAntdThemeConfig(prefs.mode, prefs.accent),
  }
}
