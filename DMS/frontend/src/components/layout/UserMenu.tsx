import { KeyOutlined, LogoutOutlined } from '@ant-design/icons'
import { App, Avatar, Button, Dropdown, type MenuProps } from 'antd'
import { Link } from 'react-router'
import { useState } from 'react'
import { CHANGE_PASSWORD_PATH, roleLabel, type NavItem } from '../../constants/navigation'
import { useMe } from '../../features/account/useMe'
import { hasAnyPermission, logout } from '../../services/api/auth'
import { useViewport } from '../../hooks/useViewport'
import { errorMessage } from '../../utils/errorMessages'

interface Props {
  /** Mục riêng của cổng (cổng sinh viên đặt Hồ sơ, Hợp đồng, Tiền cọc, Kỳ hè... ở đây trên mobile). */
  extraItems?: NavItem[]
}

/** Avatar + menu người dùng: mục riêng của cổng, Đổi mật khẩu, Đăng xuất. */
export default function UserMenu({ extraItems = [] }: Props) {
  const { data: me } = useMe()
  const { message } = App.useApp()
  const showName = useViewport() !== 'mobile'
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  if (!me) return null

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
    ...extraItems
      .filter((item) => hasAnyPermission(me, item.permissions))
      .map((item) => ({
        key: item.path,
        icon: item.icon && <item.icon />,
        label: <Link to={item.path}>{item.label}</Link>,
      })),
    ...(extraItems.length > 0 ? [{ type: 'divider' as const }] : []),
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
            background: 'linear-gradient(135deg, #1D4ED8, #3B82F6)',
            fontWeight: 600,
            fontSize: 13,
          }}
        >
          {me.fullName.trim().split(/\s+/).pop()?.[0]?.toUpperCase()}
        </Avatar>
        {showName && (
          <span style={{ fontWeight: 500, fontSize: 13, color: 'var(--color-heading)' }}>
            {me.fullName}
          </span>
        )}
      </Button>
    </Dropdown>
  )
}
