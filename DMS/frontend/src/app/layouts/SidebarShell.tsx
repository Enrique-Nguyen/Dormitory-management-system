import { SearchInput } from '../../components/ui/FormControls'
import { Badge, Button, Drawer } from '../../components/ui/Surface'
import { Permission } from '../../constants/authorization'
import {
  CheckOutlined,
  MenuOutlined,
  MoonOutlined,
  SunOutlined,
  TableOutlined,
} from '@ant-design/icons'
import { Dropdown, Flex, Layout, Menu, Tooltip, type MenuProps } from 'antd'
import { useState } from 'react'
import { Link, Outlet, useLocation } from 'react-router'
import NotificationBell from '../../components/layout/NotificationBell'
import UserMenu from '../../components/layout/UserMenu'
import { findActivePath, type NavGroup } from '../../constants/navigation'
import { useViewport } from '../../hooks/useViewport'
import { useMe } from '../../features/account/useMe'
import { hasAnyPermission, hasPermission } from '../../services/api/auth'
import { useAppTheme } from '../themeStore'
import { ACCENT_PRESETS, type AccentKey } from '../theme'

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
export default function SidebarShell({
  groups,
  portalName,
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
  const { pathname } = useLocation()
  const { data: me } = useMe()
  const { mode, accent, tableDense, toggleMode, setAccent, setTableDense } =
    useAppTheme()
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
      items={items}
      selectedKeys={active ? [active] : []}
      style={{ borderInlineEnd: 0, background: 'transparent' }}
    />
  )
  const brand = (
    <Link
      to={homePath}
      className="dms-brand-logo"
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 10,
        textDecoration: 'none',
      }}
    >
      <div
        style={{
          width: 34,
          height: 34,
          borderRadius: 8,
          background: 'var(--color-primary)',
          color: '#ffffff',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontWeight: 800,
          fontSize: 13,
          letterSpacing: '0.04em',
          boxShadow: '0 2px 6px rgba(0,0,0,0.15)',
          flexShrink: 0,
        }}
      >
        TLU
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span
            style={{
              fontSize: 15,
              fontWeight: 700,
              color: 'var(--color-heading)',
              letterSpacing: '-0.02em',
            }}
          >
            KTX Thủy Lợi
          </span>
          <span
            style={{
              fontSize: 11,
              fontWeight: 600,
              padding: '1px 6px',
              borderRadius: 4,
              background: 'var(--color-primary-light)',
              color: 'var(--color-primary)',
              border: '1px solid var(--color-primary-border)',
            }}
          >
            {portalName}
          </span>
        </div>
        <span
          style={{
            fontSize: 11,
            color: 'var(--color-text-muted)',
            fontWeight: 500,
          }}
        >
          DMS Management
        </span>
      </div>
    </Link>
  )

  const accentDropdownItems: MenuProps['items'] = Object.values(
    ACCENT_PRESETS,
  ).map((preset) => ({
    key: preset.key,
    label: (
      <span
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 8,
          minWidth: 150,
          padding: '2px 0',
        }}
      >
        <span
          style={{
            width: 12,
            height: 12,
            borderRadius: '50%',
            background: preset.primary,
            border: '1px solid rgba(0,0,0,0.12)',
            flexShrink: 0,
          }}
        />
        <span>{preset.label}</span>
        {accent === preset.key && (
          <CheckOutlined
            style={{ color: preset.primary, marginLeft: 'auto' }}
          />
        )}
      </span>
    ),
    onClick: () => setAccent(preset.key as AccentKey),
  }))

  return (
    <Layout hasSider style={{ minHeight: '100vh' }}>
      {isDesktop && (
        <Layout.Sider
          width={SIDER_WIDTH}
          theme="light"
          style={{
            position: 'sticky',
            top: 0,
            height: '100vh',
            overflow: 'auto',
            borderInlineEnd: '1px solid var(--color-border)',
            background: 'var(--color-bg-sidebar, #f8fafc)',
          }}
        >
          <div
            style={{
              padding: '14px 18px',
              borderBottom: '1px solid var(--color-border-light)',
            }}
          >
            {brand}
          </div>
          <nav aria-label="Menu chính" style={{ padding: '8px 0' }}>
            {menu}
          </nav>
        </Layout.Sider>
      )}
      <Layout style={{ minWidth: 0 }}>
        <Layout.Header className="dms-topbar">
          {!isDesktop && (
            <>
              <Button
                type="text"
                icon={<MenuOutlined />}
                aria-label="Mở menu"
                onClick={() => setDrawerOpen(true)}
                style={{ minWidth: 44, minHeight: 44 }}
              />
              {brand}
            </>
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
            <Dropdown
              menu={{ items: accentDropdownItems }}
              trigger={['click']}
              placement="bottomRight"
            >
              <Tooltip title="Chọn tông màu chủ đạo">
                <Button
                  type="text"
                  shape="circle"
                  style={{
                    width: 36,
                    height: 36,
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                  aria-label="Chọn màu chủ đạo"
                >
                  <span
                    style={{
                      width: 14,
                      height: 14,
                      borderRadius: '50%',
                      background:
                        ACCENT_PRESETS[accent]?.primary ?? '#1d4ed8',
                      boxShadow:
                        '0 0 0 2px var(--color-surface), 0 0 0 3.5px var(--color-border)',
                    }}
                  />
                </Button>
              </Tooltip>
            </Dropdown>
            <Tooltip
              title={
                tableDense
                  ? 'Độ giãn bảng: Tiêu chuẩn'
                  : 'Độ giãn bảng: Gọn gàng'
              }
            >
              <Button
                type="text"
                shape="circle"
                icon={<TableOutlined />}
                onClick={() => setTableDense(!tableDense)}
                style={{
                  width: 36,
                  height: 36,
                  color: tableDense
                    ? 'var(--color-primary)'
                    : 'var(--color-text-secondary)',
                }}
                aria-label="Đổi độ giãn bảng"
              />
            </Tooltip>
            {notificationsPath &&
              hasPermission(me, Permission.NOTIFICATION_READ) && (
                <NotificationBell to={notificationsPath} />
              )}
            <UserMenu />
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
        </Drawer>
      )}
    </Layout>
  )
}
