import { Button } from '../ui/Surface'
import ActionMenu, { type ActionMenuItem } from '../ui/ActionMenu'
import {
  KeyOutlined,
  LogoutOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { App, Avatar, Tooltip } from 'antd'
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

interface Props {
  /** Mục riêng của cổng (cổng sinh viên đặt Hồ sơ, Hợp đồng, Tiền cọc, Kỳ hè... ở đây trên mobile). */
  extraItems?: NavItem[]
  /** Hiện nút Đăng xuất cạnh avatar; cổng có sidebar đặt nút này ở góc dưới trái nên tắt. */
  showLogout?: boolean
}

/** Đăng xuất dùng chung: thu hồi token rồi chuyển sang Keycloak; lỗi hiện thông báo. */
function useLogout() {
  const { message } = App.useApp()
  const [isLoggingOut, setIsLoggingOut] = useState(false)
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
  return { isLoggingOut, onLogout }
}

interface LogoutButtonProps {
  /** Chỉ hiện biểu tượng (sidebar thu gọn). */
  compact?: boolean
  className?: string
}

/** Nút Đăng xuất độc lập, đặt ở bất kỳ vị trí nào trong khung. */
export function LogoutButton({ compact = false, className = '' }: LogoutButtonProps) {
  const { isLoggingOut, onLogout } = useLogout()
  const label = isLoggingOut ? 'Đang đăng xuất…' : 'Đăng xuất'
  return (
    <Tooltip title={compact ? label : ''} placement="right">
      <Button
        type="text"
        icon={<LogoutOutlined />}
        loading={isLoggingOut}
        onClick={() => void onLogout()}
        aria-label="Đăng xuất"
        className={`dms-logout-btn ${className}`}
      >
        {!compact && label}
      </Button>
    </Tooltip>
  )
}

/** Avatar + menu chung cho mọi vai trò: Hồ sơ, Tùy chỉnh màu sắc/giao diện theo người dùng (chuẩn V5), Đổi mật khẩu, Đăng xuất. */
export default function UserMenu({ extraItems = [], showLogout = true }: Props) {
  const { data: me } = useMe()
  const showName = useViewport() !== 'mobile'
  const { isLoggingOut, onLogout } = useLogout()

  if (!me) return null
  const profilePath = roleProfile[me.role]
  // Hồ sơ đã nằm trong menu sinh viên trên mobile; lọc mục đó để chỉ hiện một lần ở avatar.
  const visibleExtraItems = extraItems.filter(
    (item) =>
      item.path !== profilePath && hasAnyPermission(me, item.permissions),
  )

  const items: ActionMenuItem[] = [
    {
      key: 'who',
      group: true,
      label: `${me.fullName} · ${roleLabel[me.role]}`,
    },
    ...(hasPermission(me, Permission.PROFILE_READ)
      ? [
          {
            key: profilePath,
            icon: <UserOutlined />,
            label: 'Hồ sơ của tôi',
            to: profilePath,
          },
        ]
      : []),
    ...visibleExtraItems.map((item) => ({
      key: item.path,
      icon: item.icon && <item.icon />,
      label: item.label,
      to: item.path,
    })),
    { key: 'divider', divider: true },
    {
      key: 'password',
      icon: <KeyOutlined />,
      label: 'Đổi mật khẩu',
      to: CHANGE_PASSWORD_PATH,
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: isLoggingOut ? 'Đang đăng xuất…' : 'Đăng xuất',
      disabled: isLoggingOut,
      onClick: () => void onLogout(),
    },
  ]

  return (
    <>
      <ActionMenu
        items={items}
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
      </ActionMenu>
      {showLogout && <LogoutButton compact={!showName} />}
    </>
  )
}
