import { Table, type TableProps } from 'antd'
import type { TableColumnType } from 'antd'
import EmptyState from './EmptyState'
import { useAppTheme } from '../../app/themeStore'

export interface DataTableProps<RecordType extends object>
  extends TableProps<RecordType> {
  /** Hiển thị cột số thứ tự (STT) ở cột đầu tiên. Mặc định là true. */
  showStt?: boolean
  /** Tiêu đề cho cột số thứ tự. Mặc định là 'STT'. */
  sttTitle?: string
  /** Độ rộng cột STT. Mặc định là 64px. */
  sttWidth?: number | string
}

function isActionColumn(col: any): boolean {
  if (!col) return false
  const key = String(col.key ?? '').toLowerCase()
  const dataIndex = String(col.dataIndex ?? '').toLowerCase()
  const title =
    typeof col.title === 'string' ? col.title.trim().toLowerCase() : ''
  return (
    key === 'actions' ||
    key === 'action' ||
    key === 'operation' ||
    key === 'operations' ||
    dataIndex === 'actions' ||
    dataIndex === 'action' ||
    title === 'thao tác' ||
    title === 'hành động'
  )
}

function isSttColumn(col: any): boolean {
  if (!col) return false
  const key = String(col.key ?? '').toLowerCase()
  const dataIndex = String(col.dataIndex ?? '').toLowerCase()
  const title =
    typeof col.title === 'string' ? col.title.trim().toLowerCase() : ''
  return (
    key === '_stt' ||
    key === 'stt' ||
    dataIndex === 'stt' ||
    title === 'stt' ||
    title === 'số tt' ||
    title === 'số thứ tự'
  )
}

/**
 * Bảng dữ liệu chuẩn DMS áp dụng cho tất cả màn hình:
 * - Cột đầu tiên luôn là STT (Số thứ tự, tính theo trang).
 * - Cột thứ hai luôn là Thao tác (Actions) nếu bảng có cột thao tác.
 * - Các cột dữ liệu theo sau.
 * - Giao diện thanh lịch, rõ ràng, không màu mè, chuẩn tương phản cao.
 */
export default function DataTable<RecordType extends object>({
  className = '',
  size,
  pagination = false,
  scroll,
  locale,
  columns,
  showStt = true,
  sttTitle = 'STT',
  sttWidth = 64,
  ...props
}: DataTableProps<RecordType>) {
  const { tableDense } = useAppTheme()
  const effectiveSize = size ?? (tableDense ? 'small' : 'middle')
  let processedColumns = columns

  if (columns && columns.length > 0) {
    const existingStt = columns.find(isSttColumn)
    const actionCol = columns.find(isActionColumn)
    const dataCols = columns.filter(
      (col) => !isSttColumn(col) && !isActionColumn(col),
    )

    // Tạo hoặc sử dụng cột STT
    let sttCol: TableColumnType<RecordType> | undefined = existingStt
      ? {
          ...existingStt,
          align: 'center' as const,
          className: ['dms-table-col-stt', existingStt.className]
            .filter(Boolean)
            .join(' '),
        }
      : undefined
    if (!sttCol && showStt) {
      sttCol = {
        title: sttTitle,
        key: '_stt',
        width: sttWidth,
        align: 'center',
        className: 'dms-table-col-stt',
        render: (_val: unknown, _record: RecordType, index: number) => {
          if (pagination && typeof pagination === 'object') {
            const current = pagination.current ?? 1
            const pageSize = pagination.pageSize ?? 10
            return (current - 1) * pageSize + index + 1
          }
          return index + 1
        },
      } as TableColumnType<RecordType>
    }

    // Đưa cột Thao tác lên vị trí thứ 2 (ngay sau STT), bỏ fixed:'right' để hiển thị liền mạch
    const cleanActionCol = actionCol
      ? ({
          ...actionCol,
          align: 'left',
          fixed: actionCol.fixed === 'right' ? undefined : actionCol.fixed,
          className: `dms-table-col-actions ${(actionCol as any).className ?? ''}`.trim(),
        } as TableColumnType<RecordType>)
      : undefined

    processedColumns = [
      ...(sttCol ? [sttCol] : []),
      ...(cleanActionCol ? [cleanActionCol] : []),
      ...dataCols,
    ]
  }

  return (
    <div className="dms-table-container">
      <Table<RecordType>
        {...props}
        className={`dms-data-table ${className}`}
        size={effectiveSize}
        pagination={pagination}
        columns={processedColumns}
        scroll={{ x: 'max-content', ...scroll }}
        locale={{ emptyText: <EmptyState />, ...locale }}
      />
    </div>
  )
}
