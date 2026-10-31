import { theme as antdTheme, type ThemeConfig } from 'antd'

/**
 * HỆ THỐNG GIAO DIỆN VÀ MÀU SẮC CHUẨN DOANH NGHIỆP CHO DMS (THEO CẢM HỨNG HR-V5 & NOTION)
 *
 * - Thay thế hoàn toàn tông màu cũ (vàng đục/giấy cũ sepia) bằng Slate Neutral hiện đại, sắc nét.
 * - Hỗ trợ đa dạng Accent Presets (Blue, Indigo, Teal, Emerald, Violet, Amber) để người dùng tự chọn.
 * - Hỗ trợ Dark Mode chuẩn xác với độ tương phản cao, dịu mắt, đáp ứng chuẩn WCAG AA.
 * - Tương thích 100% với Ant Design v6 và hệ thống biến CSS toàn cục.
 */

export type ThemeMode = 'light' | 'dark'
export type AccentKey = 'blue' | 'indigo' | 'teal' | 'emerald' | 'violet' | 'amber'

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
  blue: {
    key: 'blue',
    label: 'Xanh Đại học (Mặc định)',
    primary: '#1d4ed8', // blue-700
    hover: '#1e40af',
    active: '#1e3a8a',
    light: '#eff6ff', // blue-50
    border: '#bfdbfe', // blue-200
  },
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
  emerald: {
    key: 'emerald',
    label: 'Xanh Lục bảo (Emerald)',
    primary: '#059669', // emerald-600
    hover: '#047857',
    active: '#065f46',
    light: '#ecfdf5',
    border: '#a7f3d0',
  },
  violet: {
    key: 'violet',
    label: 'Tím Violet (Thanh lịch)',
    primary: '#7c3aed', // violet-600
    hover: '#6d28d9',
    active: '#5b21b6',
    light: '#f5f3ff',
    border: '#ddd6fe',
  },
  amber: {
    key: 'amber',
    label: 'Cam Hổ phách (Ấm áp)',
    primary: '#d97706', // amber-600
    hover: '#b45309',
    active: '#92400e',
    light: '#fffbeb',
    border: '#fde68a',
  },
}

/** Trả về bảng màu chi tiết cho CSS Variables và Ant Design dựa trên Mode & Accent. */
export function getThemeColors(mode: ThemeMode = 'light', accentKey: AccentKey = 'blue') {
  const isDark = mode === 'dark'
  const accent = ACCENT_PRESETS[accentKey] ?? ACCENT_PRESETS.blue

  if (isDark) {
    return {
      primary: accent.primary,
      primaryHover: accent.hover,
      primaryActive: accent.active,
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
      controlBorderHover: accent.primary,
      accent: accent.primary,
      accentHover: accent.hover,
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

  // Light Mode (Sạch, sáng chuẩn Enterprise SaaS)
  return {
    primary: accent.primary,
    primaryHover: accent.hover,
    primaryActive: accent.active,
    primaryLight: accent.light,
    primaryBorder: accent.border,
    background: '#f1f5f9', // slate-100: tạo độ tương phản rõ rệt với card trắng
    backgroundAlt: '#e2e8f0', // slate-200
    surface: '#ffffff', // card trắng tinh, nổi bật trên nền slate-100
    surfaceHover: '#f8fafc',
    text: '#0f172a', // slate-900: độ sắc nét tuyệt đối
    textSecondary: '#334155', // slate-700: thông tin phụ dễ đọc
    textMuted: '#64748b', // slate-500
    heading: '#0f172a', // slate-900: tiêu đề đậm rõ
    border: '#cbd5e1', // slate-300: viền sắc nét, định hình card rõ ràng
    borderLight: '#e2e8f0', // slate-200
    borderStrong: '#94a3b8', // slate-400
    controlBorder: '#cbd5e1',
    controlBorderHover: accent.primary,
    accent: accent.primary,
    accentHover: accent.hover,
    accentLight: accent.light,
    accentBorder: accent.border,
    success: '#059669', // emerald-600
    successBg: '#ecfdf5', // emerald-50
    successBorder: '#a7f3d0', // emerald-200
    warning: '#d97706', // amber-600
    warningBg: '#fffbeb', // amber-50
    warningBorder: '#fde68a', // amber-200
    danger: '#dc2626', // red-600
    dangerBg: '#fef2f2', // red-50
    dangerBorder: '#fecaca', // red-200
    info: '#0284c7', // sky-600
    infoBg: '#f0f9ff', // sky-50
    infoBorder: '#bae6fd', // sky-200
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
export function initializeTheme(mode: ThemeMode = 'light', accentKey: AccentKey = 'blue') {
  if (typeof document === 'undefined') return

  const root = document.documentElement
  const isDark = mode === 'dark'
  const colors = getThemeColors(mode, accentKey)

  // Bật/tắt class dark cho CSS rules
  if (mode === 'dark') {
    root.classList.add('dark')
    root.setAttribute('data-theme-mode', 'dark')
  } else {
    root.classList.remove('dark')
    root.setAttribute('data-theme-mode', 'light')
  }
  root.setAttribute('data-theme-accent', accentKey)

  const cssVarMap: Record<string, string> = {
    '--color-primary': colors.primary,
    '--color-primary-hover': colors.primaryHover,
    '--color-primary-active': colors.primaryActive,
    '--color-primary-light': colors.primaryLight,
    '--color-primary-border': colors.primaryBorder,
    '--color-bg': colors.background,
    '--color-bg-alt': colors.backgroundAlt,
    '--color-bg-sidebar': isDark ? '#0f172a' : '#f8fafc',
    '--color-surface': colors.surface,
    '--color-surface-hover': colors.surfaceHover,
    '--color-text': colors.text,
    '--color-text-secondary': colors.textSecondary,
    '--color-text-muted': colors.textMuted,
    '--color-heading': colors.heading,
    '--color-border': colors.border,
    '--color-border-light': colors.borderLight,
    '--color-border-strong': colors.borderStrong,
    '--color-accent': colors.accent,
    '--color-accent-hover': colors.accentHover,
    '--color-accent-light': colors.accentLight,
    '--color-accent-border': colors.accentBorder,
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
    '--font-sans': "'Be Vietnam Pro', Inter, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif",
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
export function getAntdThemeConfig(mode: ThemeMode = 'light', accentKey: AccentKey = 'blue'): ThemeConfig {
  const isDark = mode === 'dark'
  const colors = getThemeColors(mode, accentKey)

  return {
    algorithm: isDark ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
    token: {
      fontFamily: "'Be Vietnam Pro', Inter, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif",
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
      borderRadius: 8,
      borderRadiusLG: 12,
      borderRadiusSM: 6,
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
        itemBorderRadius: 6,
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
        borderRadius: 6,
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
        borderRadiusLG: 10,
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
        borderRadius: 8,
        cellPaddingBlock: 10,
        cellPaddingInline: 12,
      },
      Modal: {
        borderRadiusLG: 16,
        headerBg: colors.surface,
        contentBg: colors.surface,
      },
      Drawer: {
        colorBgElevated: colors.surface,
      },
      Input: {
        controlHeight: 38,
        borderRadius: 8,
        colorBgContainer: isDark ? '#0f172a' : '#ffffff',
        colorBorder: colors.controlBorder,
      },
      Select: {
        controlHeight: 38,
        borderRadius: 8,
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