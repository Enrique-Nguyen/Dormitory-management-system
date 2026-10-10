import { Layout } from 'antd'
import { Link, Outlet } from 'react-router'
import NotificationBell from '../../components/layout/NotificationBell'
import UserMenu from '../../components/layout/UserMenu'

/** Cổng KTV: tối giản, không sidebar, hành động chính lớn full-width (.dms-action-lg) (UI 4.3, 22). */
export default function TechnicianLayout() {
  return (
    <Layout className="dms-touch" style={{ minHeight: '100vh' }}>
      <Layout.Header style={{ position: 'sticky', top: 0, zIndex: 10, display: 'flex', alignItems: 'center', gap: 12, borderBottom: '1px solid var(--color-border)' }}>
        <Link to="/technician/tasks" style={{ fontWeight: 700, fontSize: 18, color: '#1D4ED8' }}>
          DMS <span style={{ fontWeight: 400, color: '#6B7280', fontSize: 14 }}>· Kỹ thuật</span>
        </Link>
        <div style={{ flex: 1 }} />
        <NotificationBell to="/technician/notifications" />
        <UserMenu />
      </Layout.Header>
      <Layout.Content>
        <main className="dms-page" style={{ maxWidth: 640 }}>
          <Outlet />
        </main>
      </Layout.Content>
    </Layout>
  )
}
