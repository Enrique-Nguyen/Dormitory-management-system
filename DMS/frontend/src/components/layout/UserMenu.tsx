import { KeyOutlined, LogoutOutlined } from '@ant-design/icons'
import { useQueryClient } from '@tanstack/react-query'
import { App, Avatar, Button, Dropdown, type MenuProps } from 'antd'
import { Link, useNavigate } from 'react-router'
import { CHANGE_PASSWORD_PATH, roleLabel, type NavItem } from '../../constants/navigation'
import { useMe } from '../../features/account/useMe'
import { logout } from '../../services/api/auth'
import { useViewport } from '../../hooks/useViewport'
import { errorMessage } from '../../utils/errorMessages'

interface Props {
  /** Mục riêng của cổng (cổng sinh viên đặt Hồ sơ, Hợp đồng, Tiền cọc, Kỳ hè... ở đây trên mobile). */
  extraItems?: NavItem[]
}

/** Avatar + menu người dùng: mục riêng của cổng, Đổi mật khẩu, Đăng xuất. */
export default function UserMenu({ extraItems = [] }: Props) {
  const { data: me } = useMe()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { message } = App.useApp()
  const showName = useViewport() !== 'mobile'

  if (!me) return null

  const onLogout = async () => {
    try {
      await logout()
    } catch (error) {
      message.error(errorMessage(error))
      return
    }
    queryClient.clear()
    navigate('/login', { replace: true })
  }

  const items: MenuProps['items'] = [
    { key: 'who', type: 'group', label: `${me.fullName} · ${roleLabel[me.role]}` },
    ...extraItems.map((item) => ({
      key: item.path,
      icon: item.icon && <item.icon />,
      label: <Link to={item.path}>{item.label}</Link>,
    })),
    ...(extraItems.length > 0 ? [{ type: 'divider' as const }] : []),
    { key: 'password', icon: <KeyOutlined />, label: <Link to={CHANGE_PASSWORD_PATH}>Đổi mật khẩu</Link> },
    { key: 'logout', icon: <LogoutOutlined />, label: 'Đăng xuất', onClick: onLogout },
  ]

  return (
    <Dropdown menu={{ items }} trigger={['click']} placement="bottomRight">
      <Button type="text" aria-label={`Menu tài khoản của ${me.fullName}`} style={{ minHeight: 44 }}>
        <Avatar size="small" style={{ background: '#1D4ED8' }}>
          {me.fullName.trim().split(/\s+/).pop()?.[0]?.toUpperCase()}
        </Avatar>
        {showName && <span>{me.fullName}</span>}
      </Button>
    </Dropdown>
  )
}
