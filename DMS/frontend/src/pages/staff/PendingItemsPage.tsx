import type { TableColumnsType } from 'antd'
import { Radio } from 'antd'
import {
  ClockCircleOutlined,
  EyeOutlined,
} from '@ant-design/icons'
import { useState } from 'react'
import {
  Alert,
  Button,
  Card,
  DataTable,
  FilterBar,
  Modal,
  PageHeader,
  SearchInput,
  Select,
  Tag,
} from '../../components'
import { pendingItemDetailSegments } from '../../constants/navigation'
import RecordDetailContent from '../../features/crud/RecordDetailContent'
import { pendingItemTypeLabels } from '../../constants/statusConfig'
import { CRUD_DEFAULT_PAGE_SIZE, CRUD_PAGE_SIZES } from '../../features/crud/api'
import {
  pendingItemEntityId,
  pendingItemStudent,
  pendingItemTypeKey,
  type PendingItem,
} from '../../services/api/contract'
import { errorMessage } from '../../utils/errorMessages'
import { formatDateTime } from '../../utils/format'
import { useMe } from '../../features/account/useMe'
import { usePendingItems } from '../../features/contract/useContract'

/** Nhãn đọc từ statusConfig, đoạn đường dẫn đọc từ navigation; mã loại khớp API. */
const TYPE_META: Record<string, { label: string; segment: string }> =
  Object.fromEntries(
    Object.entries(pendingItemTypeLabels).map(([code, label]) => [
      code,
      { label: label ?? code, segment: pendingItemDetailSegments[code] ?? '' },
    ]),
  )

function typeMeta(type: string | null | undefined) {
  return (
    TYPE_META[pendingItemTypeKey(type)] ?? {
      label: type ?? 'Khác',
      segment: '',
    }
  )
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
  const [selectedItem, setSelectedItem] = useState<PendingItem | null>(null)
  const [processing, setProcessing] = useState(false)
  const selectedId = selectedItem ? pendingItemEntityId(selectedItem) : null
  const selectedResource = selectedItem
    ? typeMeta(selectedItem.type).segment
    : ''

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
      title: 'Loại yêu cầu',
      key: 'type',
      width: 190,
      render: (_value, item) => (
        <Tag
          style={{
            color: 'var(--color-primary)',
            background: 'var(--color-primary-light)',
            borderColor: 'var(--color-primary-border)',
          }}
        >
          {typeMeta(item.type).label}
        </Tag>
      ),
    },
    {
      title: 'Sinh viên',
      key: 'student',
      render: (_value, item) => {
        const student = pendingItemStudent(item)
        if (student.fullName) {
          const names = student.fullName.trim().split(' ')
          const initial = names[names.length - 1]?.charAt(0) || student.fullName.charAt(0)
          return (
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: '50%',
                  background: 'var(--color-primary-light, #eff6ff)',
                  color: 'var(--color-primary, #1d4ed8)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontWeight: 700,
                  fontSize: 13,
                  border: '1px solid var(--color-primary-border, #bfdbfe)',
                  flexShrink: 0,
                }}
              >
                {initial}
              </div>
              <div style={{ display: 'flex', flexDirection: 'column' }}>
                <span
                  style={{
                    fontWeight: 600,
                    color: 'var(--color-heading)',
                    fontSize: 13.5,
                  }}
                >
                  {student.fullName}
                </span>
                {student.studentCode && (
                  <span
                    style={{
                      fontSize: 12,
                      color: 'var(--color-text-muted)',
                      fontFamily: 'var(--font-mono, monospace)',
                    }}
                  >
                    {student.studentCode}
                  </span>
                )}
              </div>
            </div>
          )
        }
        return item.studentId != null ? (
          <Tag>Sinh viên #{item.studentId}</Tag>
        ) : (
          '—'
        )
      },
    },
    {
      title: 'Thời gian gửi',
      dataIndex: 'submittedAt',
      width: 180,
      render: (value: string | null) => (
        <span
          style={{
            fontSize: 13,
            color: 'var(--color-text-secondary)',
            display: 'inline-flex',
            alignItems: 'center',
            gap: 6,
          }}
        >
          <ClockCircleOutlined
            style={{ color: 'var(--color-text-muted)', fontSize: 12 }}
          />
          {formatDateTime(value)}
        </span>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 140,
      render: (_value, item) =>
        pendingItemEntityId(item) != null && typeMeta(item.type).segment ? (
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => { setProcessing(false); setSelectedItem(item) }}
          >
            Xem chi tiết
          </Button>
        ) : '—',
    },
  ]

  return (
    <>
      <PageHeader
        title="Yêu cầu chờ xử lý"
        subtitle="Đơn đăng ký, gia hạn, chuyển phòng, lựa chọn kỳ hè và hợp đồng chờ kích hoạt trong đơn vị."
        status={
          total > 0 ? (
            <Tag
              color="error"
              style={{
                borderRadius: 12,
                padding: '2px 10px',
                fontSize: 12.5,
                fontWeight: 600,
              }}
            >
              {total} việc chờ
            </Tag>
          ) : undefined
        }
      />

      <Card
        title="Danh sách việc chờ"
        extra={
          <span style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>
            Tổng số: <strong>{total}</strong> yêu cầu
          </span>
        }
      >
        <div style={{ marginBottom: 16 }}>
          <Radio.Group
            className="dms-pending-type-filter"
            aria-label="Loại yêu cầu chờ xử lý"
            value={type ?? 'ALL'}
            onChange={(e) => {
              const val = e.target.value === 'ALL' ? undefined : e.target.value
              setType(val)
              setPage(1)
            }}
            buttonStyle="solid"
            size="middle"
          >
            <Radio.Button value="ALL">Tất cả</Radio.Button>
            {Object.entries(TYPE_META).map(([val, meta]) => (
              <Radio.Button key={val} value={val}>
                {meta.label}
              </Radio.Button>
            ))}
          </Radio.Group>
        </div>

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
      <Modal
        open={selectedItem !== null}
        title={processing ? 'Xử lý yêu cầu chờ' : 'Chi tiết yêu cầu chờ xử lý'}
        width={1080}
        onCancel={() => { setSelectedItem(null); setProcessing(false) }}
        destroyOnHidden
        footer={
          <>
            <Button onClick={() => { setSelectedItem(null); setProcessing(false) }}>Đóng</Button>
            {!processing && (
              <Button onClick={() => setProcessing(true)}>Xử lý nghiệp vụ</Button>
            )}
          </>
        }
      >
        {selectedId != null && selectedResource && (
          <RecordDetailContent
            resource={selectedResource}
            recordId={Number(selectedId)}
            role={me?.role}
            readOnly={!processing}
          />
        )}
      </Modal>
    </>
  )
}
