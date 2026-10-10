import { Breadcrumb, Flex, Typography } from 'antd'
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
    <header style={{ marginBottom: 16 }}>
      {breadcrumb && breadcrumb.length > 0 && (
        <Breadcrumb
          style={{ marginBottom: 8 }}
          items={breadcrumb.map((crumb) => ({ title: crumb.to ? <Link to={crumb.to}>{crumb.label}</Link> : crumb.label }))}
        />
      )}
      <Flex justify="space-between" align="flex-start" wrap gap={12}>
        <div className="dms-wrap">
          <Flex align="center" wrap gap={8}>
            <Typography.Title level={3} style={{ margin: 0 }}>
              {title}
            </Typography.Title>
            {status}
          </Flex>
          {subtitle && <Typography.Text type="secondary">{subtitle}</Typography.Text>}
        </div>
        {extra}
      </Flex>
    </header>
  )
}
