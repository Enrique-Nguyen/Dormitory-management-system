import { MenuOutlined } from '@ant-design/icons'
import { Badge, Button, Drawer, Flex, Input, Layout, Menu, type MenuProps } from 'antd'
import { useState } from 'react'
import { Link, Outlet, useLocation } from 'react-router'
import NotificationBell from '../../components/layout/NotificationBell'
import UserMenu from '../../components/layout/UserMenu'
import { findActivePath, type NavGroup } from '../../constants/navigation'
import { useViewport } from '../../hooks/useViewport'

const SIDER_WIDTH = 240

interface Props {
  groups: NavGroup[]
  /** Tên cổng hiện cạnh logo: "DMS" + tên cổng. */
  portalName: string
  homePath: string
  /** Số đếm gắn vào mục menu theo path (ví dụ "Yêu cầu chờ xử lý"). */
  badges?: Record<string, number>
  /** Trang thông báo của cổng; bỏ trống thì không hiện chuông. */
  notificationsPath?: string
  /** Có thì hiện ô tìm kiếm trên topbar. */
  onSearch?: (value: string) => void
  searchPlaceholder?: string
}

/** Khung desktop-first dùng chung cho Staff và Admin (UI 2.1): sidebar cố định ≥1024 px, Drawer bên dưới. */
export default function SidebarShell({ groups, portalName, homePath, badges = {}, notificationsPath, onSearch, searchPlaceholder }: Props) {
  const viewport = useViewport()
  const isDesktop = viewport === 'desktop'
  const isMobile = viewport === 'mobile'
  const [drawerOpen, setDrawerOpen] = useState(false)
  const { pathname } = useLocation()
  const active = findActivePath(
    groups.flatMap((group) => group.items),
    pathname,
  )

  const toMenuItem = (item: NavGroup['items'][number]) => ({
    key: item.path,
    icon: item.icon && <item.icon />,
    label: (
      <Link to={item.path} onClick={() => setDrawerOpen(false)}>
        <Flex justify="space-between" align="center">
          <span>{item.label}</span>
          {badges[item.path] ? <Badge count={badges[item.path]} size="small" /> : null}
        </Flex>
      </Link>
    ),
  })

  type MenuItems = NonNullable<MenuProps['items']>
  const items = groups.flatMap<MenuItems[number]>((group, index) => {
    const children = group.items.map(toMenuItem)
    if (!group.title) return children
    return [{ key: `group-${index}`, type: 'group', label: group.title.toUpperCase(), children }]
  })

  const search = onSearch && (
    <Input.Search allowClear placeholder={searchPlaceholder} onSearch={onSearch} aria-label={searchPlaceholder ?? 'Tìm kiếm'} />
  )
  const menu = <Menu mode="inline" items={items} selectedKeys={active ? [active] : []} style={{ borderInlineEnd: 0 }} />
  const brand = (
    <Link to={homePath} className="dms-brand-logo">
      <span className="dms-brand-icon">TLU</span>
      <span>DMS</span>
      {!isMobile && <span className="dms-portal-tag">{portalName}</span>}
    </Link>
  )

  return (
    <Layout hasSider style={{ minHeight: '100vh' }}>
      {isDesktop && (
        <Layout.Sider
          width={SIDER_WIDTH}
          theme="light"
          style={{ position: 'sticky', top: 0, height: '100vh', overflow: 'auto', borderInlineEnd: '1px solid var(--color-border)', background: '#FFFFFF' }}
        >
          <div style={{ padding: '14px 20px', borderBottom: '1px solid var(--color-border-light)' }}>{brand}</div>
          <nav aria-label="Menu chính" style={{ padding: '8px 0' }}>{menu}</nav>
        </Layout.Sider>
      )}
      <Layout style={{ minWidth: 0 }}>
        <Layout.Header className="dms-topbar">
          {!isDesktop && (
            <>
              <Button type="text" icon={<MenuOutlined />} aria-label="Mở menu" onClick={() => setDrawerOpen(true)} style={{ minWidth: 44, minHeight: 44 }} />
              {brand}
            </>
          )}
          {/* Dưới 640 px không đủ chỗ cho ô tìm kiếm trên topbar: chuyển vào Drawer */}
          <div className="dms-topbar-search" style={{ flex: 1, minWidth: 0, maxWidth: 360 }}>
            {!isMobile && search}
          </div>
          <div style={{ flex: 1 }} />
          {notificationsPath && <NotificationBell to={notificationsPath} />}
          <UserMenu />
        </Layout.Header>
        <Layout.Content>
          <main className="dms-page">
            <Outlet />
          </main>
        </Layout.Content>
      </Layout>
      {!isDesktop && (
        <Drawer placement="left" size={SIDER_WIDTH + 40} open={drawerOpen} onClose={() => setDrawerOpen(false)} title={brand} styles={{ body: { padding: 0 } }}>
          {isMobile && search && <div style={{ padding: 16 }}>{search}</div>}
          <nav aria-label="Menu chính">{menu}</nav>
        </Drawer>
      )}
    </Layout>
  )
}
