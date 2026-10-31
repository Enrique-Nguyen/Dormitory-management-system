import { Permission } from '../../constants/authorization'
import { Button, Flex, Layout, Tooltip } from 'antd'
import { MoonOutlined, SunOutlined } from '@ant-design/icons'
import { Link, Outlet } from 'react-router'
import NotificationBell from '../../components/layout/NotificationBell'
import UserMenu from '../../components/layout/UserMenu'
import { useMe } from '../../features/account/useMe'
import { hasPermission } from '../../services/api/auth'
import { useAppTheme } from '../themeStore'

/** Cổng KTV: tối giản, không sidebar, hành động chính lớn full-width (.dms-action-lg) (UI 4.3, 22). */
export default function TechnicianLayout() {
  const { data: me } = useMe()
  const { mode, toggleMode } = useAppTheme()
  return (
    <Layout className="dms-touch" style={{ minHeight: '100vh' }}>
      <Layout.Header className="dms-topbar">
        <Link to="/technician/tasks" className="dms-brand-logo">
          <span className="dms-brand-icon">TLU</span>
          <span>DMS</span>
          <span className="dms-portal-tag">Kỹ thuật</span>
        </Link>
        <div style={{ flex: 1 }} />
        <Flex align="center" gap={4}>
          <Tooltip
            title={
              mode === 'dark'
                ? 'Chuyển sang Giao diện Sáng'
                : 'Chuyển sang Giao diện Tối'
            }
          >
            <Button
              type="text"
              shape="circle"
              icon={mode === 'dark' ? <SunOutlined /> : <MoonOutlined />}
              onClick={toggleMode}
              style={{
                width: 36,
                height: 36,
                color: mode === 'dark' ? '#fbbf24' : 'var(--color-text-secondary)',
              }}
              aria-label="Đổi chế độ sáng tối"
            />
          </Tooltip>
          {hasPermission(me, Permission.NOTIFICATION_READ) && (
            <NotificationBell to="/technician/notifications" />
          )}
          <UserMenu />
        </Flex>
      </Layout.Header>
      <Layout.Content>
        <main className="dms-page" style={{ maxWidth: 640 }}>
          <Outlet />
        </main>
      </Layout.Content>
    </Layout>
  )
}
