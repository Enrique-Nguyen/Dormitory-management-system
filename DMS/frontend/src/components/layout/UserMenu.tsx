import { Button } from '../ui/Surface'
import {
  BgColorsOutlined,
  CheckOutlined,
  KeyOutlined,
  LogoutOutlined,
  MoonOutlined,
  SunOutlined,
  TableOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { App, Avatar, Dropdown, type MenuProps } from 'antd'
import { Link } from 'react-router'
import { useState } from 'react'
import { Permission } from '../../constants/authorization'
import {
  CHANGE_PASSWORD_PATH,
  roleLabel,
  roleProfile,
  type NavItem,
} from '../../constants/navigation'
import { useMe } from '../../features/account/useMe'
import {
  hasAnyPermission,
  hasPermission,
  logout,
} from '../../services/api/auth'
import { useViewport } from '../../hooks/useViewport'
import { errorMessage } from '../../utils/errorMessages'
import { useAppTheme } from '../../app/themeStore'
import { ACCENT_PRESETS, type AccentKey } from '../../app/theme'

interface Props {
  /** Mục riêng của cổng (cổng sinh viên đặt Hồ sơ, Hợp đồng, Tiền cọc, Kỳ hè... ở đây trên mobile). */
  extraItems?: NavItem[]
}

/** Avatar + menu chung cho mọi vai trò: Hồ sơ, Tùy chỉnh màu sắc/giao diện theo người dùng (chuẩn V5), Đổi mật khẩu, Đăng xuất. */
export default function UserMenu({ extraItems = [] }: Props) {
  const { data: me } = useMe()
  const { message } = App.useApp()
  const showName = useViewport() !== 'mobile'
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const { mode, accent, tableDense, toggleMode, setAccent, setTableDense } = useAppTheme()

  if (!me) return null
  const profilePath = roleProfile[me.role]
  // Hồ sơ đã nằm trong menu sinh viên trên mobile; lọc mục đó để chỉ hiện một lần ở avatar.
  const visibleExtraItems = extraItems.filter(
    (item) =>
      item.path !== profilePath && hasAnyPermission(me, item.permissions),
  )

  const onLogout = async () => {
    if (isLoggingOut) return
    setIsLoggingOut(true)
    try {
      await logout()
    } catch (error) {
      message.error(errorMessage(error))
    } finally {
      setIsLoggingOut(false)
    }
  }

  const items: MenuProps['items'] = [
    {
      key: 'who',
      type: 'group',
      label: `${me.fullName} · ${roleLabel[me.role]}`,
    },
    ...(hasPermission(me, Permission.PROFILE_READ)
      ? [
          {
            key: profilePath,
            icon: <UserOutlined />,
            label: <Link to={profilePath}>Hồ sơ của tôi</Link>,
          },
        ]
      : []),
    ...visibleExtraItems.map((item) => ({
      key: item.path,
      icon: item.icon && <item.icon />,
      label: <Link to={item.path}>{item.label}</Link>,
    })),
    { type: 'divider' as const },
    {
      key: 'theme-mode',
      icon: mode === 'dark' ? <SunOutlined /> : <MoonOutlined />,
      label: mode === 'dark' ? 'Chế độ: Giao diện sáng' : 'Chế độ: Giao diện tối',
      onClick: () => toggleMode(),
    },
    {
      key: 'theme-accent',
      icon: <BgColorsOutlined />,
      label: 'Tông màu chủ đạo (V5)',
      children: Object.values(ACCENT_PRESETS).map((preset) => ({
        key: `accent-${preset.key}`,
        label: (
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
            <span
              style={{
                display: 'inline-block',
                width: 12,
                height: 12,
                borderRadius: '50%',
                background: preset.primary,
                border: '1px solid rgba(0,0,0,0.1)',
              }}
            />
            <span>{preset.label}</span>
            {accent === preset.key && (
              <CheckOutlined style={{ color: preset.primary, marginLeft: 'auto' }} />
            )}
          </span>
        ),
        onClick: () => setAccent(preset.key as AccentKey),
      })),
    },
    {
      key: 'table-density',
      icon: <TableOutlined />,
      label: tableDense ? 'Độ giãn bảng: Tiêu chuẩn' : 'Độ giãn bảng: Gọn gàng',
      onClick: () => setTableDense(!tableDense),
    },
    { type: 'divider' as const },
    {
      key: 'password',
      icon: <KeyOutlined />,
      label: <Link to={CHANGE_PASSWORD_PATH}>Đổi mật khẩu</Link>,
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: isLoggingOut ? 'Đang đăng xuất…' : 'Đăng xuất',
      disabled: isLoggingOut,
      onClick: onLogout,
    },
  ]

  return (
    <Dropdown menu={{ items }} trigger={['click']} placement="bottomRight">
      <Button
        type="text"
        aria-label={`Menu tài khoản của ${me.fullName}`}
        style={{
          minHeight: 40,
          display: 'inline-flex',
          alignItems: 'center',
          gap: 8,
          padding: '4px 10px',
          borderRadius: 'var(--radius-full)',
        }}
      >
        <Avatar
          size={28}
          style={{
            background: 'var(--color-primary-light)',
            color: 'var(--color-primary)',
            fontWeight: 600,
            fontSize: 13,
          }}
        >
          {me.fullName.trim().split(/\s+/).pop()?.[0]?.toUpperCase()}
        </Avatar>
        {showName && (
          <span
            style={{
              fontWeight: 500,
              fontSize: 13,
              color: 'var(--color-heading)',
            }}
          >
            {me.fullName}
          </span>
        )}
      </Button>
    </Dropdown>
  )
}
