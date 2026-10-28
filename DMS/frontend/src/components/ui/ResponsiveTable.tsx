import { Card, Skeleton } from './Surface'
import Table from './DataTable'
import {
  Flex,
  Pagination,
  type TableColumnsType,
  type TablePaginationConfig,
} from 'antd'
import type { Key, ReactNode } from 'react'
import { useIsMobile } from '../../hooks/useViewport'
import EmptyState from './EmptyState'

interface Props<T extends object> {
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
  /** Bật/tắt cột STT trên desktop (mặc định bật). */
  showStt?: boolean
}

function pick(record: unknown, dataIndex: unknown): unknown {
  const path = Array.isArray(dataIndex) ? dataIndex : [dataIndex]
  return path.reduce<unknown>(
    (value, key) =>
      (value as Record<PropertyKey, unknown> | undefined)?.[key as PropertyKey],
    record,
  )
}

/**
 * Bảng responsive: từ 640px hiển thị DataTable (STT ở cột đầu, Thao tác sau STT),
 * dưới 640px hiển thị danh sách thẻ có STT và Thao tác đặt nổi bật ở đầu mỗi thẻ.
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
  showStt = true,
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
        showStt={showStt}
        locale={{
          emptyText: (
            <EmptyState title={emptyTitle} description={emptyDescription} />
          ),
        }}
        scroll={{ x: 'max-content' }}
      />
    )
  }

  if (loading) return <Skeleton active paragraph={{ rows: 4 }} />
  if (!data || data.length === 0)
    return <EmptyState title={emptyTitle} description={emptyDescription} />

  const actionColumn = columns.find(
    (column) =>
      column.key === 'actions' ||
      column.key === 'action' ||
      String(column.title).toLowerCase().includes('thao tác'),
  )
  const fieldColumns = columns.filter(
    (column) =>
      column !== actionColumn &&
      column.key !== '_stt' &&
      column.key !== 'stt' &&
      String(column.title).toLowerCase() !== 'stt',
  )

  return (
    <Flex vertical gap={12}>
      {data.map((record, index) => {
        const sttNumber =
          typeof pagination === 'object' &&
          pagination?.current &&
          pagination?.pageSize
            ? (pagination.current - 1) * pagination.pageSize + index + 1
            : index + 1

        return (
          <Card
            key={rowKey(record)}
            size="small"
            className="dms-mobile-card"
          >
            {renderCard ? (
              renderCard(record)
            ) : (
              <>
                <Flex
                  justify="space-between"
                  align="center"
                  style={{
                    marginBottom: 10,
                    paddingBottom: 8,
                    borderBottom: '1px solid var(--color-border-light)',
                  }}
                >
                  <span className="dms-mobile-card-stt">
                    #{sttNumber}
                  </span>
                  {actionColumn?.render && (
                    <div className="dms-mobile-card-actions">
                      {actionColumn.render(
                        undefined,
                        record,
                        index,
                      ) as ReactNode}
                    </div>
                  )}
                </Flex>
                {fieldColumns.map((column, i) => {
                  const raw =
                    'dataIndex' in column
                      ? pick(record, column.dataIndex)
                      : undefined
                  const content = column.render
                    ? (column.render(raw, record, index) as ReactNode)
                    : (raw as ReactNode)
                  return (
                    <Flex
                      key={column.key ?? i}
                      justify="space-between"
                      gap={12}
                      className="dms-wrap"
                      style={{
                        padding: '6px 0',
                        borderBottom:
                          i < fieldColumns.length - 1
                            ? '1px dashed var(--color-border-light)'
                            : undefined,
                      }}
                    >
                      <span
                        style={{
                          color: 'var(--color-text-secondary)',
                          fontSize: 13,
                        }}
                      >
                        {column.title as ReactNode}
                      </span>
                      <span
                        style={{
                          textAlign: 'right',
                          fontWeight: 500,
                          fontSize: 13,
                        }}
                      >
                        {content}
                      </span>
                    </Flex>
                  )
                })}
              </>
            )}
          </Card>
        )
      })}
      {pagination && (
        <Pagination
          simple
          align="center"
          {...pagination}
          style={{ marginTop: 8 }}
        />
      )}
    </Flex>
  )
}
