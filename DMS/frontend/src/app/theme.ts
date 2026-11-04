import { theme as antdTheme, type ThemeConfig } from 'antd'

export type ThemeMode = 'light' | 'dark'
export type AccentKey = 'blue' | 'indigo' | 'teal' | 'emerald' | 'violet' | 'amber' | 'gray' | 'brown' | 'pink' | 'red'
export type V5AccentKey =
  | 'default'
  | 'gray'
  | 'brown'
  | 'orange'
  | 'yellow'
  | 'green'
  | 'blue'
  | 'purple'
  | 'pink'
  | 'red'
  | 'custom'
export type ThemeStyle = 'clear' | 'modern' | 'classic'

export interface CustomAccent {
  key: 'default' | 'yellow' | 'custom'
  primary: string
  light: string
}

export interface V5AccentPreset {
  key: V5AccentKey
  label: string
  swatch: string
  swatchBg: string
  backendKey?: AccentKey
}

export const DEFAULT_CUSTOM_ACCENT: CustomAccent = { key: 'custom', primary: '#0a78be', light: '#deeef8' }
export const YELLOW_ACCENT = { primary: '#cb912f', light: '#fbf3db' }
export const THEME_UI_LABELS = {
  title: 'Tùy chỉnh giao diện',
  appearance: 'GIAO DIỆN',
  accent: 'MÀU CHỦ ĐẠO',
  default: 'Mặc định',
  yellow: 'Vàng',
  custom: 'Tùy chỉnh',
  darkMode: 'Chế độ tối',
  lightMode: 'Chế độ sáng',
  dark: 'Chế độ tối',
  light: 'Chế độ sáng',
  tableBorders: 'Viền ô bảng',
  borders: 'Viền ô bảng',
  yes: 'Có',
  no: 'Không',
  dense: 'Bảng gọn',
  foreground: 'Màu chủ đạo',
  background: 'Màu nền nhấn',
  apply: 'Áp dụng',
} as const

export const V5_ACCENT_PRESETS: V5AccentPreset[] = [
  { key: 'default', label: 'Mặc định', swatch: '#cb912f', swatchBg: '#fbf3db' },
  { key: 'gray', label: 'Xám', swatch: '#787774', swatchBg: '#f1f1ef', backendKey: 'gray' },
  { key: 'brown', label: 'Nâu', swatch: '#9f6b53', swatchBg: '#f4eeee', backendKey: 'brown' },
  { key: 'orange', label: 'Cam', swatch: '#d9730d', swatchBg: '#fbecdd', backendKey: 'amber' },
  { key: 'yellow', label: 'Vàng', swatch: '#cb912f', swatchBg: '#fbf3db' },
  { key: 'green', label: 'Xanh lá', swatch: '#448361', swatchBg: '#edf3ec', backendKey: 'emerald' },
  { key: 'blue', label: 'Xanh dương', swatch: '#0a78be', swatchBg: '#deeef8', backendKey: 'blue' },
  { key: 'purple', label: 'Tím', swatch: '#9065b0', swatchBg: '#f4eeff', backendKey: 'violet' },
  { key: 'pink', label: 'Hồng', swatch: '#c14c8a', swatchBg: '#fbe4ee', backendKey: 'pink' },
  { key: 'red', label: 'Đỏ', swatch: '#ee0033', swatchBg: '#ffe0e8', backendKey: 'red' },
]

export const ACCENT_MENU_ORDER: AccentKey[] = ['gray', 'brown', 'amber', 'emerald', 'blue', 'violet', 'pink', 'red']

export const THEME_STYLES: Record<ThemeStyle, { label: string; description: string; radius: number }> = {
  clear: { label: 'Đơn giản', description: 'Phẳng, khoảng đạt, tối giản', radius: 4 },
  modern: { label: 'Hiện đại', description: 'Kính mờ trong suốt, ánh sáng mềm', radius: 6 },
  classic: { label: 'Cổ điển', description: 'Header màu, đường viền rõ, sọc bảng', radius: 4 },
}

export interface AccentMeta {
  key: AccentKey
  label: string
  primary: string
  hover: string
  active: string
  light: string
  border: string
}

export const ACCENT_PRESETS: Record<AccentKey, AccentMeta> = {
  blue: { key: 'blue', label: 'Xanh dương', primary: '#0a78be', hover: '#0867a3', active: '#075585', light: '#deeef8', border: '#b7d7ec' },
  indigo: {
    key: 'indigo',
    label: 'Tím Indigo (Hiện đại)',
    primary: '#4f46e5', // indigo-600
    hover: '#4338ca',
    active: '#3730a3',
    light: '#eef2ff',
    border: '#c7d2fe',
  },
  teal: {
    key: 'teal',
    label: 'Xanh Mòng két (Teal)',
    primary: '#0d9488', // teal-600
    hover: '#0f766e',
    active: '#115e59',
    light: '#f0fdfa',
    border: '#99f6e4',
  },
  emerald: { key: 'emerald', label: 'Xanh lá', primary: '#448361', hover: '#397252', active: '#306044', light: '#edf3ec', border: '#b7d1bc' },
  violet: { key: 'violet', label: 'Tím', primary: '#9065b0', hover: '#7c559b', active: '#694883', light: '#f4eeff', border: '#d8c7e8' },
  amber: { key: 'amber', label: 'Cam', primary: '#d9730d', hover: '#bd630b', active: '#9e5309', light: '#fbecdd', border: '#efcba8' },
  gray: { key: 'gray', label: 'Xám', primary: '#787774', hover: '#666560', active: '#55544f', light: '#f1f1ef', border: '#d6d5d2' },
  brown: { key: 'brown', label: 'Nâu', primary: '#9f6b53', hover: '#8a5c47', active: '#744d3b', light: '#f4eeee', border: '#dfc8be' },
  pink: { key: 'pink', label: 'Hồng', primary: '#c14c8a', hover: '#a64076', active: '#8b3663', light: '#fbe4ee', border: '#edbdd3' },
  red: { key: 'red', label: 'Đỏ', primary: '#ee0033', hover: '#cc002c', active: '#ad0025', light: '#ffe0e8', border: '#f7a7b8' },
}

function lightenAccent(hex: string, amount: number): string {
  const channels = [1, 3, 5].map((offset) => parseInt(hex.slice(offset, offset + 2), 16))
  return `#${channels.map((channel) => Math.round(channel + (255 - channel) * amount).toString(16).padStart(2, '0')).join('')}`
}

/** Trả về bảng màu chi tiết cho CSS Variables và Ant Design dựa trên Mode & Accent. */
export function getThemeColors(mode: ThemeMode = 'light', accentKey: AccentKey = 'blue', custom?: CustomAccent | null) {
  const isDark = mode === 'dark'
  const preset = ACCENT_PRESETS[accentKey] ?? ACCENT_PRESETS.blue
  const accent = custom ? {
    ...preset,
    primary: custom.primary,
    hover: custom.primary,
    active: custom.primary,
    light: custom.light,
    border: lightenAccent(custom.primary, 0.7),
  } : preset

  if (isDark) {
    const darkAccent = lightenAccent(accent.primary, 0.48)
    return {
      primary: darkAccent,
      primaryHover: lightenAccent(accent.primary, 0.62),
      primaryActive: lightenAccent(accent.primary, 0.34),
      primaryLight: '#1e293b', // slate-800
      primaryBorder: '#334155', // slate-700
      background: '#0b1120', // deep slate
      backgroundAlt: '#111827', // gray-900
      surface: '#1e293b', // slate-800
      surfaceHover: '#334155', // slate-700
      text: '#f8fafc', // slate-50
      textSecondary: '#cbd5e1', // slate-300
      textMuted: '#94a3b8', // slate-400
      heading: '#ffffff',
      border: '#334155', // slate-700
      borderLight: '#1e293b', // slate-800
      borderStrong: '#475569', // slate-600
      controlBorder: '#475569',
      controlBorderHover: darkAccent,
      accent: darkAccent,
      accentHover: lightenAccent(accent.primary, 0.62),
      accentLight: '#1e293b',
      accentBorder: accent.border,
      success: '#10b981',
      successBg: '#064e3b',
      successBorder: '#047857',
      warning: '#f59e0b',
      warningBg: '#78350f',
      warningBorder: '#b45309',
      danger: '#ef4444',
      dangerBg: '#7f1d1d',
      dangerBorder: '#b91c1c',
      info: '#0ea5e9',
      infoBg: '#0c4a6e',
      infoBorder: '#0284c7',
      shadowSmall: '0 1px 2px 0 rgb(0 0 0 / 0.3)',
      shadowMedium: '0 4px 12px 0 rgb(0 0 0 / 0.4)',
      shadowLarge: '0 12px 28px 0 rgb(0 0 0 / 0.5)',
      shadowHeader: '0 1px 3px 0 rgb(0 0 0 / 0.3)',
    }
  }

 
  return {
    primary: accent.primary,
    primaryHover: accent.hover,
    primaryActive: accent.active,
    primaryLight: accent.light,
    primaryBorder: accent.border,
    background: '#f7f6f3',
    backgroundAlt: '#f1f1ef',
    surface: '#ffffff',
    surfaceHover: '#f7f6f3',
    text: '#211f1b',
    textSecondary: '#5f5e5a',
    textMuted: '#8a8884',
    heading: '#211f1b',
    border: '#d6d5d2',
    borderLight: '#e4e3e0',
    borderStrong: '#b9b8b4',
    controlBorder: '#d6d5d2',
    controlBorderHover: accent.primary,
    accent: accent.primary,
    accentHover: accent.hover,
    accentLight: accent.light,
    accentBorder: accent.border,
    success: '#059669',
    successBg: '#ecfdf5',
    successBorder: '#a7f3d0',
    warning: '#d97706',
    warningBg: '#fffbeb',
    warningBorder: '#fde68a',
    danger: '#dc2626',
    dangerBg: '#fef2f2',
    dangerBorder: '#fecaca',
    info: '#0284c7',
    infoBg: '#f0f9ff',
    infoBorder: '#bae6fd',
    shadowSmall: '0 1px 3px 0 rgb(0 0 0 / 0.08), 0 1px 2px -1px rgb(0 0 0 / 0.06)',
    shadowMedium: '0 4px 6px -1px rgb(0 0 0 / 0.09), 0 2px 4px -2px rgb(0 0 0 / 0.07)',
    shadowLarge: '0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.08)',
    shadowHeader: '0 1px 3px 0 rgb(0 0 0 / 0.06)',
  }
}

/** Màu biểu đồ Dashboard đồng bộ với Theme hiện tại. */
export const dashboardChartColors = {
  occupied: '#1d4ed8',
  held: '#d97706',
  available: '#059669',
  roomFee: '#2563eb',
  utility: '#0284c7',
  topup: '#f59e0b',
  track: '#e2e8f0',
  application: '#3b82f6',
  contract: '#10b981',
  maintenance: '#f97316',
} as const

/**
 * Gắn CSS Variables và class dark vào thẻ <html>.
 * Được gọi khi ứng dụng khởi tạo hoặc khi người dùng đổi theme/accent.
 */
export function initializeTheme(mode: ThemeMode = 'light', accentKey: AccentKey = 'blue', custom?: CustomAccent | null) {
  if (typeof document === 'undefined') return

  const root = document.documentElement
  const isDark = mode === 'dark'
  const colors = getThemeColors(mode, accentKey, custom)

  // Bật/tắt class dark cho CSS rules
  if (mode === 'dark') {
    root.classList.add('dark')
    root.setAttribute('data-theme-mode', 'dark')
  } else {
    root.classList.remove('dark')
    root.setAttribute('data-theme-mode', 'light')
  }

  const v5AccentMap: Record<string, string> = {
    amber: 'orange',
    emerald: 'green',
    violet: 'purple',
  }
  const effectiveAccent = custom ? custom.key : (v5AccentMap[accentKey] ?? accentKey)
  root.setAttribute('data-theme-accent', effectiveAccent)
  root.setAttribute('data-accent', effectiveAccent)

  const cssVarMap: Record<string, string> = {
    '--color-primary': colors.primary,
    '--color-on-primary': isDark ? '#0b1120' : '#ffffff',
    '--color-primary-hover': colors.primaryHover,
    '--color-primary-active': colors.primaryActive,
    '--color-primary-light': colors.primaryLight,
    '--color-primary-border': colors.primaryBorder,
    '--color-bg': colors.background,
    '--color-bg-alt': colors.backgroundAlt,
    '--color-bg-sidebar': isDark ? '#0f172a' : `color-mix(in srgb, ${colors.primaryLight} 28%, #fbfaf8)`,
    '--color-table-header': `color-mix(in srgb, ${colors.primaryLight} 35%, ${colors.surfaceHover})`,
    '--color-surface': colors.surface,
    '--color-surface-hover': colors.surfaceHover,
    '--color-text': colors.text,
    '--color-text-secondary': colors.textSecondary,
    '--color-text-muted': colors.textMuted,
    '--color-heading': colors.heading,
    '--color-border': colors.border,
    '--color-border-light': colors.borderLight,
    '--color-border-strong': colors.borderStrong,
    '--color-accent': colors.primary,
    '--color-accent-hover': colors.primaryHover,
    '--color-accent-active': colors.primaryActive,
    '--color-accent-light': colors.primaryLight,
    '--color-accent-border': colors.primaryBorder,
    '--color-accent-bg': colors.primaryLight,
    '--src-accent': colors.primary,
    '--src-accent-bg': colors.primaryLight,
    '--color-control-border': colors.controlBorder,
    '--color-control-border-hover': colors.controlBorderHover,
    '--color-success': colors.success,
    '--color-success-bg': colors.successBg,
    '--color-success-border': colors.successBorder,
    '--color-warning': colors.warning,
    '--color-warning-bg': colors.warningBg,
    '--color-warning-border': colors.warningBorder,
    '--color-danger': colors.danger,
    '--color-danger-bg': colors.dangerBg,
    '--color-danger-border': colors.dangerBorder,
    '--color-info': colors.info,
    '--color-info-bg': colors.infoBg,
    '--color-info-border': colors.infoBorder,
    '--color-neutral': colors.textSecondary,
    '--color-neutral-bg': colors.backgroundAlt,
    '--font-sans': "-apple-system, BlinkMacSystemFont, 'Segoe UI', Inter, sans-serif",
    '--font-serif': "'Lora', 'Noto Serif', Georgia, 'Times New Roman', serif",
    '--shadow-xs': colors.shadowSmall,
    '--shadow-sm': colors.shadowSmall,
    '--shadow-md': colors.shadowMedium,
    '--shadow-lg': colors.shadowLarge,
    '--shadow-xl': colors.shadowLarge,
    '--shadow-card': colors.shadowSmall,
    '--shadow-card-hover': colors.shadowMedium,
    '--shadow-header': colors.shadowHeader,
    '--shadow-top': '0 -2px 10px rgb(0 0 0 / 0.04)',
    '--shadow-float-up': '0 -4px 12px rgb(0 0 0 / 0.06)',
  }

  for (const [name, val] of Object.entries(cssVarMap)) {
    root.style.setProperty(name, val)
  }
}

/**
 * Sinh cấu hình Ant Design v6 ThemeConfig động theo chế độ và màu nhấn.
 */
export function getAntdThemeConfig(mode: ThemeMode = 'light', accentKey: AccentKey = 'blue', style: ThemeStyle = 'modern', custom?: CustomAccent | null): ThemeConfig {
  const isDark = mode === 'dark'
  const colors = getThemeColors(mode, accentKey, custom)
  const radius = THEME_STYLES[style].radius

  return {
    algorithm: isDark ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
    token: {
      fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Inter, sans-serif",
      fontSize: 14,
      fontSizeHeading1: 26,
      fontSizeHeading2: 22,
      fontSizeHeading3: 18,
      fontSizeHeading4: 16,
      fontSizeHeading5: 14,
      colorPrimary: colors.primary,
      colorPrimaryHover: colors.primaryHover,
      colorPrimaryActive: colors.primaryActive,
      colorPrimaryBg: colors.primaryLight,
      colorPrimaryBgHover: colors.primaryBorder,
      colorSuccess: colors.success,
      colorSuccessBg: colors.successBg,
      colorWarning: colors.warning,
      colorWarningBg: colors.warningBg,
      colorError: colors.danger,
      colorErrorBg: colors.dangerBg,
      colorInfo: colors.info,
      colorInfoBg: colors.infoBg,
      colorLink: colors.primary,
      colorLinkHover: colors.primaryHover,
      colorText: colors.text,
      colorTextHeading: colors.heading,
      colorTextSecondary: colors.textSecondary,
      colorTextTertiary: colors.textMuted,
      colorTextPlaceholder: colors.textMuted,
      colorBgLayout: colors.background,
      colorBgContainer: colors.surface,
      colorBgElevated: isDark ? '#1e293b' : '#ffffff',
      colorBgMask: isDark ? 'rgba(0, 0, 0, 0.7)' : 'rgba(15, 23, 42, 0.45)',
      colorBorder: colors.border,
      colorBorderSecondary: colors.borderLight,
      colorSplit: colors.borderLight,
      borderRadius: radius,
      borderRadiusLG: radius,
      borderRadiusSM: radius,
      boxShadow: colors.shadowSmall,
      boxShadowSecondary: colors.shadowMedium,
      boxShadowTertiary: colors.shadowLarge,
      controlHeight: 38,
      controlHeightLG: 44,
      controlHeightSM: 30,
      paddingXS: 8,
      paddingSM: 12,
      padding: 16,
      paddingLG: 24,
      marginXS: 8,
      marginSM: 12,
      margin: 16,
      marginLG: 24,
    },
    components: {
      Layout: {
        headerBg: isDark ? '#1e293b' : '#ffffff',
        headerHeight: 58,
        headerPadding: '0 20px',
        siderBg: isDark ? '#0f172a' : '#f8fafc',
        bodyBg: colors.background,
      },
      Menu: {
        itemBorderRadius: radius,
        itemHeight: 38,
        itemMarginInline: 8,
        itemHoverBg: isDark ? '#1e293b' : 'rgba(0, 0, 0, 0.04)',
        itemHoverColor: colors.primary,
        itemSelectedBg: isDark ? '#1e293b' : colors.primaryLight,
        itemSelectedColor: colors.primary,
        groupTitleColor: colors.textMuted,
        groupTitleFontSize: 11,
      },
      Button: {
        controlHeight: 36,
        borderRadius: radius,
        primaryColor: isDark ? '#0b1120' : '#ffffff',
        fontWeight: 500,
        boxShadow: 'none',
        primaryShadow: 'none',
        defaultBg: isDark ? '#1e293b' : '#ffffff',
        defaultColor: colors.text,
        defaultBorderColor: colors.border,
        defaultHoverBg: isDark ? '#334155' : colors.surfaceHover,
        defaultHoverColor: colors.primary,
        defaultHoverBorderColor: colors.primary,
      },
      Card: {
        borderRadiusLG: radius,
        headerHeight: 48,
        headerFontSize: 15,
        colorBgContainer: colors.surface,
        colorBorderSecondary: colors.borderLight,
      },
      Table: {
        headerBg: isDark ? '#0f172a' : '#f1f5f9',
        headerColor: isDark ? '#cbd5e1' : '#1e293b',
        headerSplitColor: colors.borderLight,
        rowHoverBg: isDark ? '#1e293b' : '#f8fafc',
        borderColor: colors.borderLight,
        borderRadius: radius,
        cellPaddingBlock: 10,
        cellPaddingInline: 12,
      },
      Modal: {
        borderRadiusLG: radius,
        headerBg: colors.surface,
        contentBg: colors.surface,
      },
      Drawer: {
        colorBgElevated: colors.surface,
      },
      Input: {
        controlHeight: 38,
        borderRadius: radius,
        colorBgContainer: isDark ? '#0f172a' : '#ffffff',
        colorBorder: colors.controlBorder,
      },
      Select: {
        controlHeight: 38,
        borderRadius: radius,
      },
      Tabs: {
        horizontalItemPadding: '10px 16px',
        itemSelectedColor: colors.primary,
        inkBarColor: colors.primary,
      },
      Badge: {
        fontSize: 12,
      },
    },
  }
}

/** Cấu hình mặc định Light Blue khi ứng dụng khởi chạy lần đầu. */
export const theme = getAntdThemeConfig('light', 'blue')