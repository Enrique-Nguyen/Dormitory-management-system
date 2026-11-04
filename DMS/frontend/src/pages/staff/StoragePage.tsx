import { DownloadOutlined } from '@ant-design/icons'
import { App } from 'antd'
import { useState } from 'react'
import { useSearchParams } from 'react-router'
import {
  Alert,
  Button,
  Card,
  DataTable,
  FilterBar,
  Input,
  InputNumber,
  PageHeader,
  SearchInput,
  Select,
  StatusBadge,
} from '../../components'
import { Permission } from '../../constants/authorization'
import { StorageState } from '../../constants/statusConfig'
import { CRUD_DEFAULT_PAGE_SIZE, CRUD_PAGE_SIZES } from '../../features/crud/api'
import { downloadWorkbook, EXCEL_MAX_EXPORT_ROWS } from '../../features/crud/excel'
import { excelUiLabels, resources } from '../../features/crud/resources'
import ReferenceSelect from '../../features/crud/ReferenceSelect'
import { StorageModal, useStorageRecords } from '../../features/contract'
import { useMe } from '../../features/account/useMe'
import { hasPermission } from '../../services/api/auth'
import {
  searchStorageRecords,
  type StorageRecordItem,
} from '../../services/api/checkout'
import { errorMessage } from '../../utils/errorMessages'
import { formatDate } from '../../utils/format'

/**
 * Kho gửi đồ (API 4.9): tra cứu phiếu theo phạm vi, tạo phiếu khi tạm rời hè và nhận lại đồ bằng
 * cách chọn giường trống. Quyền STORAGE_CREATE/STORAGE_UPDATE quyết định nút hiển thị; backend
 * vẫn kiểm tra điều kiện hợp đồng, lựa chọn hè và phân phòng.
 */
export default function StoragePage() {
  const { message } = App.useApp()
  const config = resources.storage
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
  const [exporting, setExporting] = useState(false)
  const [returned, setReturned] = useState<string>()
  const [contractId, setContractId] = useState<number | null>(null)
  const [id, setId] = useState<number | null>(null)
  const [packageCount, setPackageCount] = useState<number | null>(null)
  const [location, setLocation] = useState('')
  const [description, setDescription] = useState('')
  const [depositedFrom, setDepositedFrom] = useState('')
  const [depositedTo, setDepositedTo] = useState('')
  const [returnedFrom, setReturnedFrom] = useState('')
  const [returnedTo, setReturnedTo] = useState('')
  const query = {
    page: page - 1,
    size: pageSize,
    ...(keyword ? { q: keyword } : {}),
    ...(returned !== undefined ? { returned: returned === 'true' } : {}),
    ...(contractId ? { contractId } : {}),
    ...(id ? { id } : {}),
    ...(packageCount ? { packageCount } : {}),
    ...(location.trim() ? { location: location.trim() } : {}),
    ...(description.trim() ? { description: description.trim() } : {}),
    ...(depositedFrom ? { depositedFrom } : {}),
    ...(depositedTo ? { depositedTo } : {}),
    ...(returnedFrom ? { returnedFrom } : {}),
    ...(returnedTo ? { returnedTo } : {}),
    sort: ['id,desc'],
  }
  const records = useStorageRecords(query)

  async function exportRecords() {
    setExporting(true)
    try {
      const rows: StorageRecordItem[] = []
      // Dùng cùng truy vấn với bảng, chỉ thay phân trang để xuất đủ kết quả từ máy chủ.
      for (let exportPage = 0; ; exportPage++) {
        const result = await searchStorageRecords({
          ...query,
          page: exportPage,
          size: 100,
        })
        if (
          result.totalElements > EXCEL_MAX_EXPORT_ROWS ||
          rows.length + result.content.length > EXCEL_MAX_EXPORT_ROWS
        ) {
          void message.warning(excelUiLabels.tooManyExportRows)
          return
        }
        rows.push(...result.content)
        if (!result.content.length || rows.length >= result.totalElements) break
      }
      if (!rows.length) {
        void message.info(excelUiLabels.exportEmpty)
        return
      }
      await downloadWorkbook(
        config.title,
        config.columns,
        rows.map((row) => ({ ...row })),
      )
      void message.success(excelUiLabels.exportSuccess)
    } catch (error) {
      void message.error(errorMessage(error))
    } finally {
      setExporting(false)
    }
  }

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
            placeholder="Tìm theo tên sinh viên hoặc mã hợp đồng..."
            onChange={(event) => {
              const value = event.target.value
              setSearchText(value)
              if (!value) search('')
            }}
            onSearch={search}
          />
          <Select allowClear aria-label={config.filters?.[0].label} placeholder={config.filters?.[0].label}
            value={returned} options={config.filters?.[0].options}
            onChange={(value: string | undefined) => { setReturned(value); setPage(1) }} />
          <ReferenceSelect aria-label={config.filters?.[1].label} placeholder={config.filters?.[1].label}
            endpoint="/contracts" labelField="code" allowClear value={contractId ?? undefined}
            onChange={(value) => { setContractId(typeof value === 'number' ? value : null); setPage(1) }} />
          <InputNumber aria-label="Mã phiếu" placeholder="Mã phiếu" min={1} precision={0} style={{ width: 160 }}
            value={id} onChange={(value) => { setId(typeof value === 'number' ? value : null); setPage(1) }} />
          <InputNumber aria-label="Số kiện" placeholder="Số kiện" min={1} precision={0} style={{ width: 160 }}
            value={packageCount} onChange={(value) => { setPackageCount(typeof value === 'number' ? value : null); setPage(1) }} />
          <Input aria-label="Vị trí" placeholder="Vị trí" maxLength={100} value={location} style={{ width: 200 }}
            onChange={(event) => { setLocation(event.target.value); setPage(1) }} />
          <Input aria-label="Mô tả" placeholder="Mô tả" maxLength={500} value={description} style={{ width: 200 }}
            onChange={(event) => { setDescription(event.target.value); setPage(1) }} />
          <label>Ngày gửi từ
            <Input type="date" aria-label="Ngày gửi từ" value={depositedFrom} max={depositedTo || undefined}
              onChange={(event) => { setDepositedFrom(event.target.value); setPage(1) }} />
          </label>
          <label>Ngày gửi đến
            <Input type="date" aria-label="Ngày gửi đến" value={depositedTo} min={depositedFrom || undefined}
              onChange={(event) => { setDepositedTo(event.target.value); setPage(1) }} />
          </label>
          <label>Ngày nhận từ
            <Input type="date" aria-label="Ngày nhận từ" value={returnedFrom} max={returnedTo || undefined}
              onChange={(event) => { setReturnedFrom(event.target.value); setPage(1) }} />
          </label>
          <label>Ngày nhận đến
            <Input type="date" aria-label="Ngày nhận đến" value={returnedTo} min={returnedFrom || undefined}
              onChange={(event) => { setReturnedTo(event.target.value); setPage(1) }} />
          </label>
          <Button
            icon={<DownloadOutlined />}
            loading={exporting}
            onClick={() => void exportRecords()}
          >
            {excelUiLabels.export}
          </Button>
        </FilterBar>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          title={excelUiLabels.readonlyHint}
          description={config.note}
        />
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
              setPage(nextSize === pageSize ? nextPage : 1)
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
