import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Alert,
  Button,
  Card,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
} from 'antd'
import axios from 'axios'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import PageHeader from '../components/layout/PageHeader'
import { useMe } from '../features/account/useMe'
import {
  getLookupRecords,
  normalizePage,
  readValue,
  writeCrudRecord,
} from '../features/crud/api'
import { resources } from '../features/crud/resources'
import type { CrudField, CrudRecord } from '../features/crud/types'
import { api } from '../services/api/client'
import { errorMessage } from '../utils/errorMessages'

const labels: Record<string, string> = {
  ACTIVE: 'Hoạt động',
  BROKEN: 'Hỏng',
  RETIRED: 'Ngừng dùng',
  CLOSED: 'Đã đóng',
  MAINTENANCE: 'Bảo trì',
  DRAFT: 'Nháp',
  SUBMITTED: 'Đã nộp',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối',
  CANCELLED: 'Đã hủy',
  PENDING: 'Chờ xử lý',
  UNPAID: 'Chưa thanh toán',
  PAID: 'Đã thanh toán',
  M: 'Nam',
  F: 'Nữ',
  STAFF: 'Quản lý',
  ADMIN: 'Quản trị',
  STUDENT: 'Sinh viên',
  TECHNICIAN: 'Kỹ thuật viên',
  ROOM_FEE: 'Phí phòng',
  UTILITY: 'Điện nước',
  ELECTRIC: 'Điện',
  WATER: 'Nước',
  LOW: 'Thấp',
  MEDIUM: 'Vừa',
  HIGH: 'Cao',
  OPEN: 'Mở',
  FURNITURE: 'Nội thất',
  DOOR_LOCK: 'Cửa/khóa',
  OTHER: 'Khác',
  STAY: 'Ở hè',
  TEMP_LEAVE: 'Tạm rời hè',
  LEAVE: 'Rời KTX',
}

function display(value: unknown): string {
  if (value == null || value === '') return '—'
  if (typeof value === 'boolean') return value ? 'Có' : 'Không'
  if (typeof value === 'number') return value.toLocaleString('vi-VN')
  if (Array.isArray(value)) return value.map(display).join(', ')
  if (typeof value === 'object')
    return Object.values(value)
      .filter((v) => v != null && typeof v !== 'object')
      .map(display)
      .join(' · ')
  return labels[String(value)] ?? String(value)
}

function hasPeriodOpened(value: unknown) {
  return new Date(String(value)).getTime() <= Date.now()
}

function LookupInput({
  field,
  disabled,
}: {
  field: CrudField
  disabled?: boolean
}) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['crud-lookup', field.lookup],
    queryFn: () => getLookupRecords(field.lookup!),
    enabled: !!field.lookup,
  })
  const options = field.lookup
    ? data?.map((row) => ({
        value: row.id,
        label: `${display(readValue(row, field.lookupLabel ?? 'name'))} (#${row.id})`,
      }))
    : field.options?.map((option) => ({
        ...option,
        label: labels[option.label] ?? option.label,
      }))
  // Form.Item truyền value/onChange qua wrapper nên Select phải nằm trực tiếp trong component này.
  return (
    <Form.Item
      noStyle
      name={field.name}
      rules={[
        {
          required: field.required,
          message: `Chọn ${field.label.toLowerCase()}`,
        },
      ]}
    >
      <Select
        showSearch
        optionFilterProp="label"
        mode={field.multiple ? 'multiple' : undefined}
        allowClear={!field.required}
        loading={isLoading}
        disabled={disabled}
        options={options}
        placeholder={error ? 'Không tải được danh sách, hãy thử lại' : 'Chọn…'}
      />
    </Form.Item>
  )
}

function CrudInput({ field }: { field: CrudField }) {
  switch (field.kind) {
    case 'number':
      return (
        <InputNumber
          style={{ width: '100%' }}
          min={field.min ?? 0}
          max={field.max}
        />
      )
    case 'date':
      return <Input type="date" />
    case 'datetime':
      return <Input type="datetime-local" />
    case 'textarea':
      return <Input.TextArea rows={4} maxLength={field.maxLength} showCount />
    case 'boolean':
      return <Switch />
    default:
      return <Input maxLength={field.maxLength} />
  }
}

export default function CrudPage({ resource }: { resource: string }) {
  const config = resources[resource]
  const { data: me } = useMe()
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const { id: routeId } = useParams()
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const [editing, setEditing] = useState<CrudRecord | null | undefined>(
    undefined,
  )
  const [detail, setDetail] = useState<CrudRecord | undefined>()
  const [busy, setBusy] = useState(false)
  const [form] = Form.useForm()
  const scopeValue = Form.useWatch('scope', form)
  const typeValue = Form.useWatch('type', form)
  const role = me?.role
  const canWrite =
    !config.readOnly && !!role && !!config.writeRoles?.includes(role)
  const canCreate =
    canWrite &&
    !config.singleton &&
    !['configs'].includes(resource) &&
    (config.createRoles ?? config.writeRoles)?.includes(role!)
  const idKey = config.idKey ?? 'id'
  const writeEndpoint = config.writeEndpoint ?? config.endpoint
  const detailEndpoint = config.detailEndpoint ?? config.endpoint
  const { data, error, isLoading, isFetching, refetch } = useQuery({
    queryKey: ['crud', resource, page, config.localSearch ? '' : search],
    queryFn: async () =>
      normalizePage(
        (
          await api.get(config.endpoint, {
            params: {
              page: page - 1,
              size: 20,
              q: config.localSearch ? undefined : search,
            },
          })
        ).data,
      ),
  })
  const rows =
    config.localSearch && search
      ? data?.content.filter((row) =>
          JSON.stringify(row)
            .toLocaleLowerCase()
            .includes(search.toLocaleLowerCase()),
        )
      : data?.content
  const fields = editing
    ? (config.editFields ?? config.fields).filter((field) => !field.createOnly)
    : (config.createFields ?? config.fields).filter((field) => !field.editOnly)
  const visible = (field: CrudField) =>
    !field.visibleWhen ||
    (field.visibleWhen.field === 'scope' ? scopeValue : typeValue) ===
      field.visibleWhen.value

  useEffect(() => {
    if (!routeId) return
    let cancelled = false
    api
      .get(`${detailEndpoint}/${routeId}`)
      .then((response) => {
        if (!cancelled) setDetail(response.data as CrudRecord)
      })
      .catch((e) => {
        if (!cancelled) void message.error(errorMessage(e))
      })
    return () => {
      cancelled = true
    }
  }, [routeId, detailEndpoint, message])

  function canEditRow(row: CrudRecord) {
    return (
      canWrite &&
      (!config.editStates || config.editStates.includes(String(row.status))) &&
      (!config.rowOwned || row[config.rowOwned] === me?.id)
    )
  }
  async function refresh() {
    await queryClient.invalidateQueries({ queryKey: ['crud'] })
    await queryClient.invalidateQueries({ queryKey: ['crud-lookup'] })
    await queryClient.invalidateQueries({ queryKey: ['notifications'] })
  }
  async function openEditor(row?: CrudRecord) {
    form.resetFields()
    if (!row) {
      setEditing(null)
      form.setFieldsValue({
        active: true,
        role: 'STAFF',
        type: 'ROOM_FEE',
        scope: 'UNIT',
        unitId: me?.managementUnit?.id,
      })
      return
    }
    try {
      const selected =
        config.singleton || config.localSearch || resource === 'configs'
          ? row
          : ((await api.get(`${detailEndpoint}/${row[idKey]}`))
              .data as CrudRecord)
      setEditing(selected)
      const initial = { ...selected }
      for (const field of config.fields)
        if (field.kind === 'datetime' && initial[field.name])
          initial[field.name] = new Date(String(initial[field.name]))
            .toLocaleString('sv-SE')
            .slice(0, 16)
            .replace(' ', 'T')
      if (resource === 'myApplications' && Array.isArray(selected.priorities))
        initial.priorityCategoryIds = selected.priorities.map(
          (p) => (p as CrudRecord).categoryId,
        )
      form.setFieldsValue(initial)
    } catch (e) {
      void message.error(errorMessage(e))
    }
  }
  async function saveRecord(values: Record<string, unknown>) {
    setBusy(true)
    try {
      const payload: Record<string, unknown> = {}
      for (const field of fields.filter(visible)) {
        const value = values[field.name]
        if (value == null || value === '') continue
        payload[field.name] =
          field.kind === 'datetime'
            ? new Date(String(value)).toISOString()
            : value
      }
      // Các DTO sửa chỉ số/thông báo cần giữ lại định danh/phạm vi đã chọn khi tạo.
      if (editing && ['readings', 'notifications'].includes(resource))
        for (const field of config.fields.filter((f) => f.createOnly))
          if (editing[field.name] != null)
            payload[field.name] = editing[field.name]
      if (
        resource === 'periods' &&
        editing &&
        hasPeriodOpened(editing.openAt)
      ) {
        for (const key of Object.keys(payload))
          if (key !== 'closeAt') delete payload[key]
      }
      let body: unknown = payload
      if (!editing && config.createFormat === 'multipart') {
        const multipart = new FormData()
        multipart.append(
          'data',
          new Blob([JSON.stringify(payload)], { type: 'application/json' }),
        )
        body = multipart
      }
      const endpoint = config.singleton
        ? writeEndpoint
        : editing
          ? `${writeEndpoint}/${editing[idKey]}`
          : writeEndpoint
      const result = await writeCrudRecord(
        endpoint,
        resource === 'configs' ? 'put' : editing ? 'patch' : 'post',
        body,
      )
      if (result && typeof result === 'object' && 'temporaryPassword' in result)
        modal.info({
          title: 'Mật khẩu tạm (hiển thị một lần)',
          content: (
            <Typography.Text copyable>
              {String((result as CrudRecord).temporaryPassword)}
            </Typography.Text>
          ),
        })
      else void message.success('Đã lưu')
      setEditing(undefined)
      await refresh()
    } catch (e) {
      if (axios.isAxiosError(e)) {
        const errors = e.response?.data?.details?.fieldErrors as
          | { field: string; message: string }[]
          | undefined
        if (errors)
          form.setFields(
            errors.map((item) => ({
              name: item.field,
              errors: [item.message],
            })),
          )
      }
      void message.error(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }
  function deleteRecord(row: CrudRecord) {
    let reason = ''
    modal.confirm({
      title: `${config.deleteLabel ?? 'Xóa'} bản ghi này?`,
      content:
        config.deleteAction === 'lock' ? (
          <Input.TextArea
            placeholder="Lý do khóa (ít nhất 5 ký tự)"
            onChange={(event) => {
              reason = event.target.value
            }}
          />
        ) : (
          'Thao tác sẽ được kiểm tra quyền và lịch sử dữ liệu tại máy chủ.'
        ),
      okText: config.deleteLabel ?? 'Xóa',
      cancelText: 'Đóng',
      okButtonProps: { danger: true },
      onOk: async () => {
        if (config.deleteAction === 'lock' && reason.trim().length < 5) {
          void message.error('Nhập lý do ít nhất 5 ký tự')
          throw new Error('Thiếu lý do')
        }
        try {
          await writeCrudRecord(
            `${writeEndpoint}/${row[idKey]}${config.deleteAction ? `/${config.deleteAction}` : ''}`,
            config.deleteAction ? 'post' : 'delete',
            config.deleteAction === 'lock' ? { reason } : undefined,
          )
          void message.success('Đã cập nhật')
          await refresh()
        } catch (e) {
          void message.error(errorMessage(e))
          throw e
        }
      },
    })
  }
  async function showDetail(row: CrudRecord) {
    try {
      setDetail(
        config.singleton ||
          config.localSearch ||
          resource === 'configs' ||
          resource === 'inbox' ||
          resource === 'storage' ||
          resource === 'myRoom'
          ? row
          : resource === 'deposits'
            ? ((await api.get(`/contracts/${row.id}/deposit`))
                .data as CrudRecord)
            : ((await api.get(`${detailEndpoint}/${row[idKey]}`))
                .data as CrudRecord),
      )
    } catch (e) {
      void message.error(errorMessage(e))
    }
  }
  const detailFields = [
    ...config.columns,
    ...config.fields.map((field) => ({ key: field.name, label: field.label })),
  ].filter(
    (item, index, list) =>
      list.findIndex((other) => other.key === item.key) === index,
  )

  return (
    <>
      <PageHeader
        title={config.title}
        subtitle="Tìm kiếm và quản lý dữ liệu"
        extra={
          <Space>
            <Button
              loading={isFetching}
              onClick={() => {
                void refetch()
              }}
            >
              Tải lại
            </Button>
            {canCreate && (
              <Button
                type="primary"
                onClick={() => {
                  void openEditor()
                }}
              >
                Thêm mới
              </Button>
            )}
          </Space>
        }
      />
      {config.note && (
        <Alert
          style={{ marginBottom: 16 }}
          type="info"
          title={config.note}
          showIcon
        />
      )}
      <Card>
        <Input.Search
          aria-label={`Tìm kiếm ${config.title.toLowerCase()}`}
          placeholder="Nhập từ khóa tìm kiếm"
          allowClear
          enterButton="Tìm kiếm"
          onSearch={(value) => {
            setSearch(value.trim())
            setPage(1)
          }}
          style={{ maxWidth: 480, marginBottom: 16 }}
        />
        {error ? (
          <Alert
            type="error"
            title={errorMessage(error)}
            action={
              <Button
                onClick={() => {
                  void refetch()
                }}
              >
                Thử lại
              </Button>
            }
          />
        ) : (
          <Table<CrudRecord>
            loading={isLoading}
            rowKey={(row) => String(row[idKey] ?? JSON.stringify(row))}
            dataSource={rows}
            scroll={{ x: 780 }}
            locale={{ emptyText: <Empty description="Chưa có dữ liệu" /> }}
            pagination={
              config.singleton
                ? false
                : {
                    current: page,
                    pageSize: 20,
                    total: config.localSearch
                      ? rows?.length
                      : data?.totalElements,
                    showSizeChanger: false,
                    onChange: setPage,
                  }
            }
            columns={[
              ...config.columns.map((column) => ({
                title: column.label,
                key: column.key,
                render: (_: unknown, row: CrudRecord) => (
                  <span>{display(readValue(row, column.key))}</span>
                ),
              })),
              {
                title: 'Thao tác',
                key: 'actions',
                fixed: 'right',
                width: canWrite ? 210 : 95,
                render: (_: unknown, row: CrudRecord) => (
                  <Space wrap>
                    <Button
                      size="small"
                      onClick={() => {
                        void showDetail(row)
                      }}
                    >
                      Xem
                    </Button>
                    {canEditRow(row) && (
                      <Button
                        size="small"
                        onClick={() => {
                          void openEditor(row)
                        }}
                      >
                        Sửa
                      </Button>
                    )}
                    {canEditRow(row) &&
                      !config.singleton &&
                      resource !== 'configs' &&
                      !(resource === 'students' && role !== 'ADMIN') && (
                        <Button
                          size="small"
                          danger
                          onClick={() => deleteRecord(row)}
                        >
                          {config.deleteLabel ?? 'Xóa'}
                        </Button>
                      )}
                    {resource === 'users' && row.locked === true && (
                      <Button
                        size="small"
                        onClick={() => {
                          void writeCrudRecord(
                            `/admin/users/${row.id}/unlock`,
                            'post',
                          )
                            .then(refresh)
                            .catch((e) => message.error(errorMessage(e)))
                        }}
                      >
                        Mở khóa
                      </Button>
                    )}
                  </Space>
                ),
              },
            ]}
          />
        )}
      </Card>
      <Modal
        title={`${editing ? 'Sửa' : 'Thêm'} ${config.title.toLowerCase()}`}
        open={editing !== undefined}
        destroyOnHidden
        onCancel={() => setEditing(undefined)}
        onOk={() => form.submit()}
        confirmLoading={busy}
        okText="Lưu"
        cancelText="Đóng"
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={(values) => {
            void saveRecord(values)
          }}
          preserve={false}
        >
          {fields
            .filter(visible)
            .filter(
              (field) =>
                !(
                  resource === 'maintenance' &&
                  role === 'STUDENT' &&
                  field.name === 'roomId'
                ),
            )
            .map((field) =>
              field.kind === 'select' ? (
                <Form.Item
                  key={field.name}
                  label={field.label}
                  required={field.required}
                >
                  <LookupInput field={field} />
                </Form.Item>
              ) : (
                <Form.Item
                  key={field.name}
                  name={field.name}
                  label={field.label}
                  valuePropName={field.kind === 'boolean' ? 'checked' : 'value'}
                  rules={[
                    {
                      required: field.required,
                      message: `Nhập ${field.label.toLowerCase()}`,
                    },
                    ...(field.maxLength
                      ? [
                          {
                            max: field.maxLength,
                            message: `Tối đa ${field.maxLength} ký tự`,
                          },
                        ]
                      : []),
                  ]}
                >
                  <CrudInput field={field} />
                </Form.Item>
              ),
            )}
        </Form>
      </Modal>
      <Drawer
        title={`Chi tiết ${config.title.toLowerCase()}`}
        open={!!detail}
        width={640}
        onClose={() => {
          setDetail(undefined)
          if (routeId) navigate('..')
        }}
      >
        {detail && (
          <>
            <Descriptions
              column={1}
              bordered
              items={detailFields.map((item) => ({
                key: item.key,
                label: item.label,
                children: display(readValue(detail, item.key)),
              }))}
            />
            {resource === 'rooms' && Array.isArray(detail.beds) && (
              <BedCrudPanel
                roomId={Number(detail.id)}
                beds={detail.beds as CrudRecord[]}
                reload={async () => {
                  setDetail(
                    (await api.get(`/rooms/${detail.id}`)).data as CrudRecord,
                  )
                  await refresh()
                }}
              />
            )}
            {['items', 'transactions', 'timeline'].map(
              (key) =>
                Array.isArray(detail[key]) && (
                  <Card
                    key={key}
                    title={
                      {
                        items: 'Dòng hóa đơn',
                        transactions: 'Giao dịch cọc',
                        timeline: 'Lịch sử',
                      }[key]
                    }
                    style={{ marginTop: 16 }}
                  >
                    {(detail[key] as CrudRecord[]).map((item, index) => (
                      <Typography.Paragraph key={index}>
                        {display(item)}
                      </Typography.Paragraph>
                    ))}
                  </Card>
                ),
            )}
          </>
        )}
      </Drawer>
    </>
  )
}

/** Bảng giường con của phòng: thêm, sửa mã và xóa giường chưa từng được phân. */
function BedCrudPanel({
  roomId,
  beds,
  reload,
}: {
  roomId: number
  beds: CrudRecord[]
  reload: () => Promise<void>
}) {
  const { message, modal } = App.useApp()
  const [form] = Form.useForm()
  const [editing, setEditing] = useState<CrudRecord | null | undefined>()
  const [busy, setBusy] = useState(false)
  async function save(values: Record<string, unknown>) {
    setBusy(true)
    try {
      await writeCrudRecord(
        editing ? `/beds/${editing.id}` : `/rooms/${roomId}/beds`,
        editing ? 'patch' : 'post',
        values,
      )
      setEditing(undefined)
      await reload()
    } catch (e) {
      void message.error(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }
  return (
    <Card
      title="Giường trong phòng"
      style={{ marginTop: 16 }}
      extra={
        <Button
          onClick={() => {
            form.resetFields()
            setEditing(null)
          }}
        >
          Thêm giường
        </Button>
      }
    >
      <Table<CrudRecord>
        rowKey="id"
        pagination={false}
        dataSource={beds}
        columns={[
          { title: 'Mã', dataIndex: 'code' },
          {
            title: 'Trạng thái',
            dataIndex: 'status',
            render: (value) => <Tag>{display(value)}</Tag>,
          },
          { title: 'Người ở', dataIndex: 'occupant', render: display },
          {
            title: 'Thao tác',
            render: (_, bed) => (
              <Space>
                <Button
                  size="small"
                  onClick={() => {
                    form.setFieldsValue({ code: bed.code })
                    setEditing(bed)
                  }}
                >
                  Sửa
                </Button>
                <Button
                  size="small"
                  danger
                  onClick={() =>
                    modal.confirm({
                      title: 'Xóa giường chưa có lịch sử?',
                      onOk: async () => {
                        try {
                          await writeCrudRecord(`/beds/${bed.id}`, 'delete')
                          await reload()
                        } catch (e) {
                          void message.error(errorMessage(e))
                          throw e
                        }
                      },
                    })
                  }
                >
                  Xóa
                </Button>
              </Space>
            ),
          },
        ]}
      />
      <Modal
        title={editing ? 'Sửa giường' : 'Thêm giường'}
        open={editing !== undefined}
        onCancel={() => setEditing(undefined)}
        onOk={() => form.submit()}
        confirmLoading={busy}
        okText="Lưu"
        cancelText="Đóng"
      >
        <Form
          form={form}
          onFinish={(values) => {
            void save(values)
          }}
        >
          <Form.Item
            name="code"
            label="Mã giường"
            rules={[
              { required: true, message: 'Nhập mã giường' },
              { max: 10, message: 'Tối đa 10 ký tự' },
            ]}
          >
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
