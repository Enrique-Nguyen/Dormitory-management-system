import { Card, Flex, Pagination, Skeleton, Table, type TableColumnsType, type TablePaginationConfig } from 'antd'
import type { Key, ReactNode } from 'react'
import { useIsMobile } from '../../hooks/useViewport'
import EmptyState from './EmptyState'

interface Props<T> {
  columns: TableColumnsType<T>
  data: T[] | undefined
  rowKey: (record: T) => Key
  loading?: boolean
  /** Phân trang phía server (API Spec 2.5: page bắt đầu từ 0, nhớ đổi sang 1-based khi truyền vào). Mặc định tắt
   *  ở cả hai chế độ để desktop và mobile hiện cùng một tập dòng. */
  pagination?: TablePaginationConfig | false
  emptyTitle?: string
  emptyDescription?: ReactNode
  /** Tùy biến thẻ trên mobile; mặc định dựng từ `columns`. */
  renderCard?: (record: T) => ReactNode
}

function pick(record: unknown, dataIndex: unknown): unknown {
  const path = Array.isArray(dataIndex) ? dataIndex : [dataIndex]
  return path.reduce<unknown>((value, key) => (value as Record<PropertyKey, unknown> | undefined)?.[key as PropertyKey], record)
}

/**
 * Bảng từ 640 px, danh sách thẻ dưới 640 px (UI mục 25). Mặc định thẻ hiện mỗi cột là một dòng "tên: giá trị";
 * cột có `key: 'actions'` đưa xuống chân thẻ.
 */
export default function ResponsiveTable<T extends object>({
  columns,
  data,
  rowKey,
  loading,
  pagination = false,
  emptyTitle = 'Chưa có dữ liệu',
  emptyDescription,
  renderCard,
}: Props<T>) {
  const isMobile = useIsMobile()

  if (!isMobile) {
    return (
      <Table<T>
        columns={columns}
        dataSource={data}
        rowKey={rowKey}
        loading={loading}
        pagination={pagination}
        locale={{ emptyText: <EmptyState title={emptyTitle} description={emptyDescription} /> }}
        scroll={{ x: 'max-content' }}
      />
    )
  }

  if (loading) return <Skeleton active />
  if (!data || data.length === 0) return <EmptyState title={emptyTitle} description={emptyDescription} />

  const actionColumn = columns.find((column) => column.key === 'actions')
  const fieldColumns = columns.filter((column) => column !== actionColumn)

  return (
    <Flex vertical gap={12}>
      {data.map((record, index) => (
        <Card key={rowKey(record)} size="small">
          {renderCard ? (
            renderCard(record)
          ) : (
            <>
              {fieldColumns.map((column, i) => {
                const raw = 'dataIndex' in column ? pick(record, column.dataIndex) : undefined
                const content = column.render ? (column.render(raw, record, index) as ReactNode) : (raw as ReactNode)
                return (
                  <Flex key={column.key ?? i} justify="space-between" gap={12} className="dms-wrap" style={{ padding: '4px 0' }}>
                    <span style={{ color: '#6B7280' }}>{column.title as ReactNode}</span>
                    <span style={{ textAlign: 'right' }}>{content}</span>
                  </Flex>
                )
              })}
              {actionColumn?.render && (
                <div style={{ marginTop: 8 }}>{actionColumn.render(undefined, record, index) as ReactNode}</div>
              )}
            </>
          )}
        </Card>
      ))}
      {pagination && <Pagination simple align="center" {...pagination} />}
    </Flex>
  )
}
