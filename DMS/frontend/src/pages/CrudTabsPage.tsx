import { Tabs } from 'antd'
import CrudPage from './CrudPage'

/** Trang ghép chỉ chứa các bảng CRUD; không triển khai quy trình xét duyệt hoặc tài chính. */
export default function CrudTabsPage({
  tabs,
}: {
  tabs: { key: string; label: string; resource: string }[]
}) {
  return (
    <Tabs
      destroyOnHidden
      items={tabs.map((tab) => ({
        key: tab.key,
        label: tab.label,
        children: <CrudPage key={tab.resource} resource={tab.resource} />,
      }))}
    />
  )
}
