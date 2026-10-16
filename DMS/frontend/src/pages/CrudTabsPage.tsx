import { Action, permissionFor } from '../constants/authorization'
import { Tabs } from 'antd'
import CrudPage from './CrudPage'
import { useMe } from '../features/account/useMe'
import { resources } from '../features/crud/resources'
import { hasPermission } from '../services/api/auth'

/** Trang ghép chỉ chứa các bảng CRUD; không triển khai quy trình xét duyệt hoặc tài chính. */
export default function CrudTabsPage({
  tabs,
}: {
  tabs: { key: string; label: string; resource: string }[]
}) {
  const { data: me } = useMe()
  const allowedTabs = tabs.filter((tab) =>
    hasPermission(me, permissionFor(resources[tab.resource].permission, Action.READ)),
  )
  return (
    <Tabs
      destroyOnHidden
      items={allowedTabs.map((tab) => ({
        key: tab.key,
        label: tab.label,
        children: <CrudPage key={tab.resource} resource={tab.resource} />,
      }))}
    />
  )
}
