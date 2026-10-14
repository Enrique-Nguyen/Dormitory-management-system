import { Breadcrumb, Flex } from 'antd'
import type { ReactNode } from 'react'
import { Link } from 'react-router'

interface Props {
  title: ReactNode
  /** Rule 01: trạng thái hiện trước hành động, nên badge nằm cạnh tiêu đề. */
  status?: ReactNode
  subtitle?: ReactNode
  breadcrumb?: { label: string; to?: string }[]
  /** Vùng hành động (thường là ActionBar). */
  extra?: ReactNode
}

export default function PageHeader({ title, status, subtitle, breadcrumb, extra }: Props) {
  return (
    <header className="dms-page-header">
      {breadcrumb && breadcrumb.length > 0 && (
        <Breadcrumb
          style={{ marginBottom: 10, fontSize: 13 }}
          items={breadcrumb.map((crumb) => ({ title: crumb.to ? <Link to={crumb.to}>{crumb.label}</Link> : crumb.label }))}
        />
      )}
      <Flex justify="space-between" align="center" wrap gap={12}>
        <div className="dms-wrap">
          <Flex align="center" wrap gap={10}>
            <h1 className="dms-page-title">{title}</h1>
            {status}
          </Flex>
          {subtitle && <p className="dms-page-subtitle">{subtitle}</p>}
        </div>
        {extra}
      </Flex>
    </header>
  )
}
