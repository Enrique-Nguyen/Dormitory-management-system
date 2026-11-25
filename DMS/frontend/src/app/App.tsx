import { QueryClientProvider } from '@tanstack/react-query'
import { App as AntApp, ConfigProvider } from 'antd'
import viVN from 'antd/locale/vi_VN'
import dayjs from 'dayjs'
import { useCallback, useEffect, useLayoutEffect, useRef } from 'react'
import { RouterProvider } from 'react-router'
import { useMe } from '../features/account/useMe'
import { watchAuthenticationChanges } from '../services/api/auth'
import { ErrorCode } from '../constants/authorization'
import {
  getPreferences,
  savePreferences,
  type UserPreferences,
} from '../services/api/preferences'
import { errorCodeOf, errorMessage } from '../utils/errorMessages'
import { queryClient } from './queryClient'
import { router } from './router'
import {
  commitPreferencesVersion,
  getActiveUserId,
  getCurrentPreferences,
  getPreferencesVersion,
  getUserChangeEpoch,
  hydratePreferences,
  readCachedPreferences,
  resetToDefaults,
  setActiveUser,
  subscribePreferenceSaves,
  useAppTheme,
  userPreferencesStorageKey,
} from './themeStore'

dayjs.locale('vi')

/** Thời gian gộp các thay đổi liên tiếp (đổi mode, accent, độ giãn bảng) thành một lần lưu. */
const SAVE_DEBOUNCE_MS = 600

const CONFLICT_RECOVERED_MESSAGE =
  'Tùy chọn giao diện vừa được thay đổi ở nơi khác. Đã tải lại tùy chọn mới nhất; hãy chọn lại nếu muốn giữ thay đổi của bạn.'

const CONFLICT_RELOAD_FAILED_MESSAGE =
  'Tùy chọn giao diện vừa được thay đổi ở nơi khác và chưa tải lại được bản mới nhất. Thay đổi của bạn chỉ được giữ trên thiết bị này; hãy thử lại khi kết nối ổn định.'

/** Ghi tùy chọn máy chủ vào store mà không phát sinh lần lưu ngược lại. */
function applyServerPreferences(server: UserPreferences) {
  hydratePreferences({
    mode: server.themeMode,
    accent: server.themePreset,
    tableDense: server.tableDense,
    version: server.version,
  })
}

/**
 * Cầu nối tùy chọn giao diện giữa /api/auth/me, /api/me/preferences và store cục bộ.
 *
 * - Đổi tài khoản/đăng xuất: dừng debounce, đưa giao diện về mặc định an toàn, nạp cache
 *   của đúng tài khoản rồi mới lấy bản máy chủ (máy chủ thắng cache).
 * - Thay đổi của người dùng: ghi cache riêng của tài khoản và debounce lưu lên máy chủ.
 * - Phản hồi của tài khoản cũ được bỏ qua bằng epoch, tránh áp nhầm sau khi đổi tài khoản.
 */
function ThemePreferenceBridge() {
  const { data: me } = useMe()
  const { message } = AntApp.useApp()
  const userId = me?.id ?? null

  const saveTimerRef = useRef<number | null>(null)
  const saveInFlightRef = useRef(false)
  const pendingSaveRef = useRef(false)
  const dirtyRef = useRef(false)
  const conflictReloadRef = useRef(false)
  const identityEpochRef = useRef(0)
  // Giữ API message mới nhất mà không phải đăng ký lại pipeline lưu khi context đổi tham chiếu.
  const messageRef = useRef(message)
  useEffect(() => {
    messageRef.current = message
  }, [message])

  const cancelScheduledSave = useCallback(() => {
    if (saveTimerRef.current !== null) {
      window.clearTimeout(saveTimerRef.current)
      saveTimerRef.current = null
    }
    pendingSaveRef.current = false
  }, [])

  // Đồng bộ danh tính trước khi trình duyệt vẽ khung hình kế tiếp để không lộ theme tài khoản cũ.
  useLayoutEffect(() => {
    cancelScheduledSave()
    dirtyRef.current = false
    conflictReloadRef.current = false
    identityEpochRef.current += 1
    const epoch = identityEpochRef.current

    setActiveUser(userId)
    resetToDefaults()
    if (userId === null) return

    const cached = readCachedPreferences(userId)
    if (cached) hydratePreferences(cached)

    // Người dùng có thể bấm đổi giao diện trước khi GET trả về: khi đó giữ thao tác của họ,
    // lần lưu kế tiếp sẽ đối soát version với máy chủ.
    const editsAtRequest = getUserChangeEpoch()
    let cancelled = false
    void getPreferences()
      .then((server) => {
        if (cancelled || identityEpochRef.current !== epoch) return
        if (getUserChangeEpoch() !== editsAtRequest) return
        applyServerPreferences(server)
      })
      .catch(() => {
        // Chưa tải được bản máy chủ: giữ cache của tài khoản này và thử đồng bộ ở lần lưu sau.
      })

    return () => {
      cancelled = true
    }
  }, [cancelScheduledSave, userId])

  // Lưu thay đổi của người dùng sau khi ngừng thao tác; mỗi lúc chỉ một request được gửi.
  useEffect(() => {
    let disposed = false

    function fireSave() {
      saveTimerRef.current = null
      if (saveInFlightRef.current) {
        pendingSaveRef.current = true
        return
      }
      void runSave()
    }

    function requestSave() {
      if (disposed || getActiveUserId() === null) return
      dirtyRef.current = true
      if (conflictReloadRef.current) return
      // Lần hẹn mới luôn gửi ảnh chụp mới nhất, nên không cần giữ cờ chờ của lần trước.
      pendingSaveRef.current = false
      if (saveTimerRef.current !== null) window.clearTimeout(saveTimerRef.current)
      saveTimerRef.current = window.setTimeout(fireSave, SAVE_DEBOUNCE_MS)
    }

    async function reloadAfterConflict(epoch: number) {
      cancelScheduledSave()
      conflictReloadRef.current = true
      const editsAtReload = getUserChangeEpoch()
      try {
        const server = await getPreferences()
        if (disposed || identityEpochRef.current !== epoch) return
        if (getUserChangeEpoch() !== editsAtReload) {
          messageRef.current.error(CONFLICT_RELOAD_FAILED_MESSAGE)
          return
        }
        applyServerPreferences(server)
        dirtyRef.current = false
        messageRef.current.error(CONFLICT_RECOVERED_MESSAGE)
      } catch {
        if (disposed || identityEpochRef.current !== epoch) return
        messageRef.current.error(CONFLICT_RELOAD_FAILED_MESSAGE)
      } finally {
        if (identityEpochRef.current === epoch) conflictReloadRef.current = false
      }
    }

    async function runSave() {
      if (disposed) return
      const savingUserId = getActiveUserId()
      if (savingUserId === null) return
      if (saveInFlightRef.current) {
        pendingSaveRef.current = true
        return
      }

      const epoch = identityEpochRef.current
      const editsAtSave = getUserChangeEpoch()
      const snapshot = getCurrentPreferences()
      const requestVersion = getPreferencesVersion()
      saveInFlightRef.current = true
      try {
        const saved = await savePreferences({
          themeMode: snapshot.mode,
          themePreset: snapshot.accent,
          tableDense: snapshot.tableDense,
          version: requestVersion,
        })
        if (disposed || identityEpochRef.current !== epoch) return
        commitPreferencesVersion(savingUserId, saved.version)
        if (getUserChangeEpoch() === editsAtSave) dirtyRef.current = false
      } catch (error) {
        if (disposed || identityEpochRef.current !== epoch) return
        if (errorCodeOf(error) === ErrorCode.CONCURRENT_UPDATE) {
          await reloadAfterConflict(epoch)
        } else {
          // Giữ thay đổi cục bộ đã ghi cache; không coi là đã lưu thành công.
          messageRef.current.error(errorMessage(error))
        }
      } finally {
        saveInFlightRef.current = false
        if (
          !disposed &&
          pendingSaveRef.current &&
          getActiveUserId() !== null &&
          saveTimerRef.current === null
        ) {
          pendingSaveRef.current = false
          saveTimerRef.current = window.setTimeout(fireSave, SAVE_DEBOUNCE_MS)
        }
      }
    }

    const unsubscribe = subscribePreferenceSaves(requestSave)
    return () => {
      disposed = true
      unsubscribe()
      if (saveTimerRef.current !== null) {
        window.clearTimeout(saveTimerRef.current)
        saveTimerRef.current = null
      }
    }
  }, [cancelScheduledSave])

  // Nhiều tab: chỉ áp cache của tài khoản đang hoạt động ở tab này; hydrate không tạo vòng lưu.
  useEffect(() => {
    const onStorage = (event: StorageEvent) => {
      if (!event.key || event.newValue === null) return
      const activeId = getActiveUserId()
      if (activeId === null || event.key !== userPreferencesStorageKey(activeId))
        return
      if (dirtyRef.current || saveInFlightRef.current) return
      const cached = readCachedPreferences(activeId)
      if (cached) hydratePreferences(cached)
    }
    window.addEventListener('storage', onStorage)
    return () => window.removeEventListener('storage', onStorage)
  }, [])

  return null
}

export default function App() {
  const { antdThemeConfig } = useAppTheme()

  useEffect(watchAuthenticationChanges, [])
  return (
    <ConfigProvider theme={antdThemeConfig} locale={viVN}>
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <ThemePreferenceBridge />
          <RouterProvider router={router} />
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>
  )
}
