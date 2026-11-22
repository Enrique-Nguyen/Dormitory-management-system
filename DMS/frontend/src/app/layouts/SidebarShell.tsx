import { SearchInput } from '../../components/ui/FormControls'
import { Badge, Button, Drawer } from '../../components/ui/Surface'
import { Permission } from '../../constants/authorization'
import { MenuFoldOutlined, MenuOutlined, MenuUnfoldOutlined } from '@ant-design/icons'
import { Breadcrumb, Flex, Layout, Menu, type MenuProps } from 'antd'
import { useState } from 'react'
import { Link, Outlet, useLocation } from 'react-router'
import NotificationBell from '../../components/layout/NotificationBell'
import UserMenu, { LogoutButton } from '../../components/layout/UserMenu'
import {
  commonUiLabels,
  findActivePath,
  type NavGroup,
} from '../../constants/navigation'
import { useViewport } from '../../hooks/useViewport'
import { useMe } from '../../features/account/useMe'
import { hasAnyPermission, hasPermission } from '../../services/api/auth'
import { useAppTheme } from '../themeStore'
import ThemeSwitcher from '../../components/layout/ThemeSwitcher'

const SIDER_WIDTH = 256
const SIDER_COLLAPSED_WIDTH = 56
/** Khóa lưu riêng trên trình duyệt: chỉ ghi nhớ trạng thái thu gọn sidebar của người xem. */
const COLLAPSED_STORAGE_KEY = 'dms_sidebar_collapsed'

function readCollapsed(): boolean {
  try {
    return window.localStorage.getItem(COLLAPSED_STORAGE_KEY) === 'true'
  } catch {
    return false
  }
}

interface Props {
  groups: NavGroup[]
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
export default function SidebarShell({
  groups,
  homePath,
  badges = {},
  notificationsPath,
  onSearch,
  searchPlaceholder,
}: Props) {
  const viewport = useViewport()
  const isDesktop = viewport === 'desktop'
  const isMobile = viewport === 'mobile'
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [collapsed, setCollapsed] = useState(readCollapsed)
  const { pathname } = useLocation()
  const { data: me } = useMe()
  const { mode, solidAccentChrome } = useAppTheme()
  const allowedGroups = groups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) =>
        hasAnyPermission(me, item.permissions),
      ),
    }))
    .filter((group) => group.items.length > 0)
  const active = findActivePath(
    allowedGroups.flatMap((group) => group.items),
    pathname,
  )

  const activeGroup = allowedGroups.find((group) =>
    group.items.some((item) => item.path === active),
  )
  const activeItem = activeGroup?.items.find((item) => item.path === active)
  const breadcrumbItems = [
    { title: <Link to={homePath}>{commonUiLabels.home}</Link> },
    ...(activeGroup?.title ? [{ title: activeGroup.title }] : []),
    ...(activeItem && activeItem.path !== homePath
      ? [{ title: activeItem.label }]
      : []),
  ]

  const toggleCollapsed = () => {
    setCollapsed((current) => {
      const next = !current
      try {
        window.localStorage.setItem(COLLAPSED_STORAGE_KEY, String(next))
      } catch {
        // Trạng thái vẫn đúng trong phiên khi trình duyệt chặn lưu trữ.
      }
      return next
    })
  }

  const toMenuItem = (item: NavGroup['items'][number]) => ({
    key: item.path,
    icon: item.icon && <item.icon />,
    label: (
      <Link to={item.path} onClick={() => setDrawerOpen(false)}>
        <Flex justify="space-between" align="center">
          <span>{item.label}</span>
          {badges[item.path] ? (
            <Badge count={badges[item.path]} size="small" />
          ) : null}
        </Flex>
      </Link>
    ),
  })

  type MenuItems = NonNullable<MenuProps['items']>
  const items = allowedGroups.flatMap<MenuItems[number]>((group, index) => {
    const children = group.items.map(toMenuItem)
    if (!group.title) return children
    return [
      {
        key: `group-${index}`,
        type: 'group',
        label: group.title.toUpperCase(),
        children,
      },
    ]
  })

  const search = onSearch && (
    <SearchInput
      allowClear
      placeholder={searchPlaceholder}
      onSearch={onSearch}
      aria-label={searchPlaceholder ?? 'Tìm kiếm'}
    />
  )
  const menu = (
    <Menu
      mode="inline"
      inlineCollapsed={isDesktop && collapsed}
      items={items}
      selectedKeys={active ? [active] : []}
      style={{ borderInlineEnd: 0, background: 'transparent' }}
    />
  )
  const brandMark = (
    <span className="dms-brand-icon" aria-hidden>
      TLU
    </span>
  )
  const brand = (
    <Link to={homePath} className="dms-brand-logo dms-brand-sidebar">
      {brandMark}
      {!(isDesktop && collapsed) && (
        <span className="dms-brand-name">KTX Thủy Lợi</span>
      )}
    </Link>
  )

  return (
    <Layout hasSider style={{ minHeight: '100vh' }}>
      {isDesktop && (
        <Layout.Sider
          data-app-sidebar
          className="dms-sider"
          width={SIDER_WIDTH}
          collapsedWidth={SIDER_COLLAPSED_WIDTH}
          collapsed={collapsed}
          trigger={null}
          theme={mode}
          style={{
            position: 'sticky',
            top: 0,
            height: '100vh',
            overflow: 'hidden',
          }}
        >
          <div className="dms-sider-brand">{brand}</div>
          <nav aria-label="Menu chính" className="dms-sider-nav">
            {menu}
          </nav>
          <div className="dms-sider-footer">
            <LogoutButton compact={collapsed} />
          </div>
        </Layout.Sider>
      )}
      <Layout style={{ minWidth: 0 }}>
        <Layout.Header
          className="dms-topbar"
          data-topbar={solidAccentChrome ? 'accent' : undefined}
        >
          <Button
            type="text"
            className="dms-topbar-toggle"
            icon={
              isDesktop ? (
                collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />
              ) : (
                <MenuOutlined />
              )
            }
            aria-label={isDesktop ? 'Thu gọn hoặc mở rộng menu' : 'Mở menu'}
            onClick={() => (isDesktop ? toggleCollapsed() : setDrawerOpen(true))}
          />
          {!isMobile && (
            <Breadcrumb
              className="dms-topbar-breadcrumb"
              items={breadcrumbItems}
            />
          )}
          {/* Dưới 640 px không đủ chỗ cho ô tìm kiếm trên topbar: chuyển vào Drawer */}
          <div
            className="dms-topbar-search"
            style={{ flex: 1, minWidth: 0, maxWidth: 360 }}
          >
            {!isMobile && search}
          </div>
          <div style={{ flex: 1 }} />
          <Flex align="center" gap={4}>
            <ThemeSwitcher />
            {notificationsPath &&
              hasPermission(me, Permission.NOTIFICATION_READ) && (
                <NotificationBell to={notificationsPath} />
              )}
            <UserMenu showLogout={false} />
          </Flex>
        </Layout.Header>
        <Layout.Content>
          <main className="dms-page">
            <Outlet />
          </main>
        </Layout.Content>
      </Layout>
      {!isDesktop && (
        <Drawer
          placement="left"
          size={SIDER_WIDTH + 40}
          open={drawerOpen}
          onClose={() => setDrawerOpen(false)}
          title={brand}
          styles={{ body: { padding: 0 } }}
        >
          {isMobile && search && <div style={{ padding: 16 }}>{search}</div>}
          <nav aria-label="Menu chính">{menu}</nav>
          <div className="dms-sider-footer">
            <LogoutButton />
          </div>
        </Drawer>
      )}
    </Layout>
  )
}
