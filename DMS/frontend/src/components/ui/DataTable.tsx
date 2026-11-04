import { Table, type TableProps, Select } from 'antd'
import type { TableColumnType, TablePaginationConfig } from 'antd'
import {
  DoubleLeftOutlined,
  DoubleRightOutlined,
  LeftOutlined,
  RightOutlined,
} from '@ant-design/icons'
import EmptyState from './EmptyState'
import { useAppTheme } from '../../app/themeStore'

export interface DataTableProps<RecordType extends object>
  extends TableProps<RecordType> {
  /** Hiển thị cột số thứ tự (STT) ở cột đầu tiên hoặc sau checkbox. Mặc định là true. */
  showStt?: boolean
  /** Tiêu đề cho cột số thứ tự. Mặc định là '#'. */
  sttTitle?: string
  /** Độ rộng cột STT. Mặc định là 52px. */
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
    title === '#' ||
    title === 'stt' ||
    title === 'số tt' ||
    title === 'số thứ tự'
  )
}

/**
 * Thanh phân trang chuẩn Parkway HRM V5:
 * - Bên trái: "Hiển thị X–Y trong Z" + Dropdown chọn số dòng/trang
 * - Bên phải: Cụm nút điều hướng << < 1 2 3 > >>
 */
export function TablePagination({
  current = 1,
  pageSize = 10,
  total = 0,
  pageSizeOptions = [10, 20, 50],
  onChange,
}: {
  current?: number
  pageSize?: number
  total?: number
  pageSizeOptions?: (number | string)[]
  onChange?: (page: number, pageSize: number) => void
}) {
  const totalPages = Math.max(1, Math.ceil(total / pageSize))
  const rangeStart = total === 0 ? 0 : (current - 1) * pageSize + 1
  const rangeEnd = Math.min(current * pageSize, total)
  const isFirst = current <= 1
  const isLast = current >= totalPages

  const numericPageSizeOptions = pageSizeOptions.map((s) => Number(s)).filter((n) => !Number.isNaN(n) && n > 0)
  const effectiveOptions = numericPageSizeOptions.length > 0 ? numericPageSizeOptions : [10, 20, 50]

  const handlePageChange = (nextPage: number) => {
    if (nextPage < 1 || nextPage > totalPages || nextPage === current) return
    onChange?.(nextPage, pageSize)
  }

  const handlePageSizeChange = (nextSize: number) => {
    if (nextSize === pageSize) return
    onChange?.(1, nextSize)
  }

  // Thuật toán hiển thị số trang chuẩn V5
  const getPageItems = (): (number | string)[] => {
    if (totalPages <= 7) {
      return Array.from({ length: totalPages }, (_, i) => i + 1)
    }
    if (current <= 4) {
      return [1, 2, 3, 4, 5, '...', totalPages]
    }
    if (current >= totalPages - 3) {
      return [1, '...', totalPages - 4, totalPages - 3, totalPages - 2, totalPages - 1, totalPages]
    }
    return [1, '...', current - 1, current, current + 1, '...', totalPages]
  }

  const pageItems = getPageItems()

  return (
    <div className="dms-table-pagination">
      <div className="dms-pagination-info">
        <span className="dms-pagination-range">
          Hiển thị <strong>{rangeStart}–{rangeEnd}</strong> trong <strong>{total}</strong>
        </span>
        <div className="dms-pagination-size-select">
          <span className="dms-pagination-size-label">Số dòng:</span>
          <Select
            size="small"
            value={pageSize}
            onChange={handlePageSizeChange}
            className="dms-pagination-select"
            options={effectiveOptions.map((s) => ({
              value: s,
              label: `${s}`,
            }))}
            dropdownMatchSelectWidth={false}
          />
        </div>
      </div>

      <div className="dms-pagination-controls">
        <button
          type="button"
          disabled={isFirst}
          onClick={() => handlePageChange(1)}
          className="dms-page-nav-btn"
          aria-label="Trang đầu"
          title="Trang đầu"
        >
          <DoubleLeftOutlined />
        </button>
        <button
          type="button"
          disabled={isFirst}
          onClick={() => handlePageChange(current - 1)}
          className="dms-page-nav-btn"
          aria-label="Trang trước"
          title="Trang trước"
        >
          <LeftOutlined />
        </button>

        <div className="dms-page-numbers">
          {pageItems.map((item, index) => {
            if (item === '...') {
              return (
                <span key={`dots-${index}`} className="dms-page-dots">
                  …
                </span>
              )
            }
            const pageNum = Number(item)
            const isActive = pageNum === current
            return (
              <button
                key={`page-${pageNum}`}
                type="button"
                onClick={() => handlePageChange(pageNum)}
                className={`dms-page-num-btn ${isActive ? 'dms-page-num-active' : ''}`}
                aria-current={isActive ? 'page' : undefined}
              >
                {pageNum}
              </button>
            )
          })}
        </div>

        <button
          type="button"
          disabled={isLast}
          onClick={() => handlePageChange(current + 1)}
          className="dms-page-nav-btn"
          aria-label="Trang sau"
          title="Trang sau"
        >
          <RightOutlined />
        </button>
        <button
          type="button"
          disabled={isLast}
          onClick={() => handlePageChange(totalPages)}
          className="dms-page-nav-btn"
          aria-label="Trang cuối"
          title="Trang cuối"
        >
          <DoubleRightOutlined />
        </button>
      </div>
    </div>
  )
}

/**
 * Bảng dữ liệu chuẩn Parkway HRM V5:
 * - Cột Checkbox chọn dòng ở đầu bảng (nếu bật rowSelection).
 * - Cột thứ hai là STT (Số thứ tự `#`, tự động tính theo trang).
 * - Cột thứ ba là Thao tác (Actions) nếu bảng có cột thao tác.
 * - Các cột dữ liệu theo sau.
 * - Chân trang phân trang chuẩn V5: "Hiển thị X–Y trong Z", dropdown chọn số dòng/trang, << < 1 2 3 > >>.
 */
export default function DataTable<RecordType extends object>({
  className = '',
  size,
  pagination = false,
  scroll,
  locale,
  columns,
  showStt = true,
  sttTitle = '#',
  sttWidth = 52,
  ...props
}: DataTableProps<RecordType>) {
  const { tableDense } = useAppTheme()
  const effectiveSize = size ?? (tableDense ? 'small' : 'middle')
  let processedColumns = columns

  const isPaginationConfig = typeof pagination === 'object' && pagination !== null
  const paginationConfig: TablePaginationConfig | undefined = isPaginationConfig ? pagination : undefined

  if (columns && columns.length > 0) {
    const existingStt = columns.find(isSttColumn)
    const actionCol = columns.find(isActionColumn)
    const dataCols = columns.filter(
      (col) => !isSttColumn(col) && !isActionColumn(col),
    )

    // Tạo hoặc sử dụng cột STT (#)
    let sttCol: TableColumnType<RecordType> | undefined = existingStt
      ? {
          ...existingStt,
          title: sttTitle,
          width: existingStt.width ?? sttWidth,
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
          if (paginationConfig) {
            const current = paginationConfig.current ?? 1
            const pageSize = paginationConfig.pageSize ?? 10
            return (current - 1) * pageSize + index + 1
          }
          return index + 1
        },
      } as TableColumnType<RecordType>
    }

    // Đưa cột Thao tác lên vị trí ngay sau STT, định dạng class dms-table-col-actions
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
        pagination={false}
        columns={processedColumns}
        scroll={{ x: 'max-content', ...scroll }}
        locale={{ emptyText: <EmptyState />, ...locale }}
      />
      {paginationConfig && (
        <TablePagination
          current={paginationConfig.current ?? 1}
          pageSize={paginationConfig.pageSize ?? 10}
          total={paginationConfig.total ?? 0}
          pageSizeOptions={paginationConfig.pageSizeOptions}
          onChange={(nextPage, nextSize) => {
            paginationConfig.onChange?.(nextPage, nextSize)
          }}
        />
      )}
    </div>
  )
}
