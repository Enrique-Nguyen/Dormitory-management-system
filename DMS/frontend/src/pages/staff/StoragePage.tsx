import { useState } from 'react'
import { useSearchParams } from 'react-router'
import {
  Alert,
  Button,
  Card,
  DataTable,
  FilterBar,
  PageHeader,
  SearchInput,
  StatusBadge,
} from '../../components'
import { Permission } from '../../constants/authorization'
import { StorageState } from '../../constants/statusConfig'
import { CRUD_DEFAULT_PAGE_SIZE, CRUD_PAGE_SIZES } from '../../features/crud/api'
import { StorageModal, useStorageRecords } from '../../features/contract'
import { useMe } from '../../features/account/useMe'
import { hasPermission } from '../../services/api/auth'
import type { StorageRecordItem } from '../../services/api/checkout'
import { errorMessage } from '../../utils/errorMessages'
import { formatDate } from '../../utils/format'

/**
 * Kho gửi đồ (API 4.9): tra cứu phiếu theo phạm vi, tạo phiếu khi tạm rời hè và nhận lại đồ bằng
 * cách chọn giường trống. Quyền STORAGE_CREATE/STORAGE_UPDATE quyết định nút hiển thị; backend
 * vẫn kiểm tra điều kiện hợp đồng, lựa chọn hè và phân phòng.
 */
export default function StoragePage() {
  const { data: me } = useMe()
  const [searchParams] = useSearchParams()
  const linkedContractId = Number(searchParams.get('contractId'))
  const canCreate = hasPermission(me, Permission.STORAGE_CREATE)
  const canUpdate = hasPermission(me, Permission.STORAGE_UPDATE)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(CRUD_DEFAULT_PAGE_SIZE)
  const [searchText, setSearchText] = useState('')
  const [keyword, setKeyword] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [returning, setReturning] = useState<StorageRecordItem | null>(null)
  const records = useStorageRecords({
    page: page - 1,
    size: pageSize,
    ...(keyword ? { q: keyword } : {}),
  })

  function search(value: string) {
    setKeyword(value.trim())
    setPage(1)
  }

  return (
    <>
      <PageHeader
        title="Kho gửi đồ"
        subtitle="Phiếu gửi đồ tạm rời hè và phân lại giường khi sinh viên nhận lại đồ."
        extra={
          canCreate && (
            <Button type="primary" onClick={() => setCreateOpen(true)}>
              Tạo phiếu gửi đồ
            </Button>
          )
        }
      />
      <Card title="Danh sách phiếu gửi đồ">
        <FilterBar>
          <SearchInput
            value={searchText}
            placeholder="Tìm theo tên sinh viên..."
            onChange={(event) => {
              const value = event.target.value
              setSearchText(value)
              if (!value) search('')
            }}
            onSearch={search}
          />
        </FilterBar>
        {records.isError && (
          <Alert
            type="error"
            showIcon
            style={{ marginBottom: 16 }}
            title="Không thể tải danh sách phiếu gửi đồ"
            description={errorMessage(records.error)}
          />
        )}
        <DataTable<StorageRecordItem>
          rowKey="id"
          loading={records.isLoading || records.isFetching}
          dataSource={records.data?.content ?? []}
          scroll={{ x: 1100 }}
          columns={[
            {
              title: 'Phiếu',
              dataIndex: 'id',
              width: 90,
              render: (value: number) => `#${value}`,
            },
            {
              title: 'Sinh viên',
              dataIndex: 'fullName',
              render: (value: string | null | undefined) => value ?? '—',
            },
            {
              title: 'Hợp đồng',
              dataIndex: 'contractCode',
              render: (value: string | null | undefined, row) =>
                value ?? `#${row.contractId}`,
            },
            {
              title: 'Số kiện',
              dataIndex: 'packageCount',
              width: 90,
              align: 'right',
            },
            {
              title: 'Mô tả',
              dataIndex: 'description',
              ellipsis: true,
            },
            {
              title: 'Vị trí',
              dataIndex: 'location',
              ellipsis: true,
            },
            {
              title: 'Ngày gửi',
              dataIndex: 'depositedAt',
              width: 120,
              render: (value: string) => formatDate(value),
            },
            {
              title: 'Ngày nhận',
              dataIndex: 'returnedAt',
              width: 120,
              render: (value: string | null | undefined) => formatDate(value),
            },
            {
              title: 'Trạng thái',
              key: 'storageStatus',
              dataIndex: 'returnedAt',
              width: 120,
              render: (value: string | null | undefined) => (
                <StatusBadge
                  entity="storage"
                  status={value ? StorageState.RETURNED : StorageState.STORED}
                />
              ),
            },
            ...(canUpdate
              ? [
                  {
                    title: 'Thao tác',
                    key: 'actions',
                    width: 150,
                    render: (_: unknown, row: StorageRecordItem) =>
                      row.returnedAt ? (
                        '—'
                      ) : (
                        <Button
                          size="small"
                          onClick={() => setReturning(row)}
                        >
                          Nhận lại đồ
                        </Button>
                      ),
                  },
                ]
              : []),
          ]}
          pagination={{
            current: page,
            pageSize,
            total: records.data?.totalElements ?? 0,
            showSizeChanger: true,
            pageSizeOptions: CRUD_PAGE_SIZES,
            showTotal: (total) => `${total} phiếu`,
            onChange: (nextPage, nextSize) => {
              setPage(nextPage)
              setPageSize(nextSize)
            },
          }}
        />
      </Card>

      <StorageModal
        mode="create"
        open={createOpen}
        contractId={
          Number.isSafeInteger(linkedContractId) && linkedContractId > 0
            ? linkedContractId
            : undefined
        }
        onClose={() => setCreateOpen(false)}
      />
      <StorageModal
        mode="return"
        open={returning != null}
        record={returning}
        onClose={() => setReturning(null)}
      />
    </>
  )
}
