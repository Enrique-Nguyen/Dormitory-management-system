import type { ThemeConfig } from 'antd'

// Design tokens: UI/UX mục 2.1a. Sửa ở đây, không đặt màu cứng trong component.
export const theme: ThemeConfig = {
  token: {
    fontFamily: "'Be Vietnam Pro', Inter, system-ui, sans-serif",
    fontSize: 14,
    colorPrimary: '#1D4ED8',
    colorSuccess: '#15803D',
    colorWarning: '#B45309',
    colorError: '#B91C1C',
    colorInfo: '#0369A1',
    colorText: '#1F2937',
    colorTextSecondary: '#6B7280',
    colorBgLayout: '#F5F7FA',
    colorBgContainer: '#FFFFFF',
    borderRadius: 6,
    borderRadiusLG: 10,
    // Spacing bội số của 4
    paddingXS: 8,
    paddingSM: 12,
    padding: 16,
    paddingLG: 24,
  },
  components: {
    Layout: {
      headerBg: '#FFFFFF',
      headerHeight: 56,
      headerPadding: '0 16px',
      siderBg: '#FFFFFF',
    },
    Menu: {
      itemBorderRadius: 6,
    },
  },
}
