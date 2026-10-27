import type { TableColumnsType } from 'antd'
import { useState } from 'react'
import { Link } from 'react-router'
import {
  Alert,
  Button,
  Card,
  DataTable,
  FilterBar,
  PageHeader,
  SearchInput,
  Select,
  Tag,
} from '../../components'
import { portalPath } from '../../constants/navigation'
import { CRUD_DEFAULT_PAGE_SIZE, CRUD_PAGE_SIZES } from '../../features/crud/api'
import {
  PendingItemType,
  pendingItemEntityId,
  pendingItemStudent,
  pendingItemTypeKey,
  type PendingItem,
} from '../../services/api/contract'
import { errorMessage } from '../../utils/errorMessages'
import { formatDateTime } from '../../utils/format'
import { useMe } from '../../features/account/useMe'
import { usePendingItems } from '../../features/contract/useContract'

/** Nhãn và đoạn đường dẫn chi tiết cho từng loại việc; mã loại khớp API. */
const TYPE_META: Record<string, { label: string; segment: string }> = {
  [PendingItemType.APPLICATION]: {
    label: 'Đơn đăng ký',
    segment: 'applications',
  },
  [PendingItemType.EXTENSION]: { label: 'Gia hạn', segment: 'extensions' },
  [PendingItemType.TRANSFER]: { label: 'Chuyển phòng', segment: 'transfers' },
  [PendingItemType.SUMMER]: { label: 'Lựa chọn hè', segment: 'summer' },
  [PendingItemType.CONTRACT]: {
    label: 'Hợp đồng chờ kích hoạt',
    segment: 'contracts',
  },
}

function typeMeta(type: string | null | undefined) {
  return (
    TYPE_META[pendingItemTypeKey(type)] ?? {
      label: type ?? 'Khác',
      segment: '',
    }
  )
}

function itemPath(role: Parameters<typeof portalPath>[0], item: PendingItem) {
  const meta = typeMeta(item.type)
  if (!meta.segment) return null
  const id = pendingItemEntityId(item)
  // Chưa có ID đối tượng thì không mở màn chi tiết; liên kết về danh sách sẽ rơi vào route lạ.
  if (id == null) return null
  return `${portalPath(role, `/staff/${meta.segment}`)}/${id}`
}

/**
 * Hộp "Yêu cầu chờ xử lý" (UI 22A): gom đơn đăng ký, gia hạn, chuyển phòng, lựa chọn hè và hợp
 * đồng chờ kích hoạt trong phạm vi Staff. Lọc theo loại và tìm theo sinh viên ở máy chủ; mỗi dòng
 * mở thẳng màn chi tiết tương ứng.
 */
export default function PendingItemsPage() {
  const { data: me } = useMe()
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(CRUD_DEFAULT_PAGE_SIZE)
  const [searchText, setSearchText] = useState('')
  const [keyword, setKeyword] = useState('')
  const [type, setType] = useState<string | undefined>()

  const pendingItems = usePendingItems({
    page: page - 1,
    size: pageSize,
    ...(keyword ? { q: keyword } : {}),
    ...(type ? { type } : {}),
  })
  const rows = pendingItems.data?.content ?? []
  const total = pendingItems.data?.totalElements ?? 0

  function search(value: string) {
    setKeyword(value.trim())
    setPage(1)
  }

  const columns: TableColumnsType<PendingItem> = [
    {
      title: 'Loại',
      key: 'type',
      render: (_value, item) => <Tag>{typeMeta(item.type).label}</Tag>,
    },
    {
      title: 'Sinh viên',
      key: 'student',
      render: (_value, item) => {
        const student = pendingItemStudent(item)
        if (student.fullName)
          return student.studentCode
            ? `${student.fullName} · ${student.studentCode}`
            : student.fullName
        return item.studentId != null ? `Sinh viên #${item.studentId}` : '—'
      },
    },
    {
      title: 'Gửi lúc',
      dataIndex: 'submittedAt',
      render: (value: string | null) => formatDateTime(value),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_value, item) => {
        const path = itemPath(me?.role, item)
        return path ? <Link to={path}>Xem chi tiết</Link> : '—'
      },
    },
  ]

  return (
    <>
      <PageHeader
        title="Yêu cầu chờ xử lý"
        subtitle="Đơn đăng ký, gia hạn, chuyển phòng, lựa chọn kỳ hè và hợp đồng chờ kích hoạt trong đơn vị."
        extra={
          <Button
            onClick={() => void pendingItems.refetch()}
            loading={pendingItems.isFetching}
          >
            Làm mới
          </Button>
        }
      />

      <Card title="Danh sách việc chờ">
        <FilterBar>
          <SearchInput
            value={searchText}
            placeholder="Tìm theo tên sinh viên hoặc mã sinh viên..."
            onChange={(event) => {
              const value = event.target.value
              setSearchText(value)
              if (!value) search('')
            }}
            onSearch={search}
          />
          <Select<string>
            allowClear
            placeholder="Lọc theo loại việc"
            style={{ minWidth: 220 }}
            value={type}
            onChange={(value) => {
              setType(value)
              setPage(1)
            }}
            options={Object.entries(TYPE_META).map(([value, meta]) => ({
              value,
              label: meta.label,
            }))}
          />
        </FilterBar>

        {pendingItems.isError && (
          <Alert
            type="error"
            showIcon
            style={{ marginBottom: 16 }}
            title="Không thể tải danh sách yêu cầu chờ"
            description={errorMessage(pendingItems.error)}
          />
        )}

        <DataTable<PendingItem>
          rowKey={(item) =>
            String(
              item.id ??
                `${pendingItemTypeKey(item.type)}-${pendingItemEntityId(item) ?? ''}-${item.submittedAt ?? ''}`,
            )
          }
          loading={pendingItems.isLoading || pendingItems.isFetching}
          dataSource={rows}
          columns={columns}
          scroll={{ x: 760 }}
          pagination={{
            current: page,
            pageSize,
            total,
            showSizeChanger: true,
            pageSizeOptions: CRUD_PAGE_SIZES,
            showTotal: (count) => `${count} việc`,
            onChange: (nextPage, nextPageSize) => {
              setPage(nextPageSize !== pageSize ? 1 : nextPage)
              setPageSize(nextPageSize)
            },
          }}
        />
      </Card>
    </>
  )
}
