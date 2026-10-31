import { ConfirmPaymentModal, DepositLedgerModal } from '../features/billing'
import { EmptyState, InvoiceStatus, StatusBadge } from '../components'
import { CreateTicketModal } from '../features/maintenance'
import {
  Input,
  InputNumber,
  SearchInput,
  Select,
  Switch,
  TextArea,
} from '../components/ui/FormControls'
import {
  Alert,
  Button,
  Card,
  FilterBar,
  FormGrid,
  Modal,
} from '../components/ui/Surface'
import Table from '../components/ui/DataTable'
import {
  Action,
  Permission,
  Role,
  permissionFor,
} from '../constants/authorization'
import {
  DeleteOutlined,
  DollarCircleOutlined,
  EditOutlined,
  EyeOutlined,
  GlobalOutlined,
  KeyOutlined,
  ReloadOutlined,
  UnlockOutlined,
} from '@ant-design/icons'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Form,
  Result,
  Space,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import { useEffect, useMemo, useRef, useState } from 'react'
import RecordDetailContent, { hasRecordDetailContent } from '../features/crud/RecordDetailContent'
import {
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from 'react-router'
import PageHeader from '../components/layout/PageHeader'
import { useMe } from '../features/account/useMe'
import {
  getLookupRecords,
  getRecordFormValues,
  createSearchRequest,
  searchByPage,
  saveOrUpdate,
  CRUD_PAGE_SIZES,
  normalizePage,
  readCrudQuery,
  readValue,
  toLocalDateTime,
  updateCrudQuery,
  validationFieldErrors,
  writeCrudRecord,
} from '../features/crud/api'
import { resources, billingUiLabels, crudDetailUiLabels, crudReferenceLabels, crudSecretFieldNames } from '../features/crud/resources'
import { crudValueLabels, InvoiceStatus as InvoiceState } from '../constants/statusConfig'
import type { CrudColumn, CrudField, CrudRecord } from '../features/crud/types'
import { api } from '../services/api/client'
import { hasPermission } from '../services/api/auth'
import { errorMessage } from '../utils/errorMessages'
import {
  formatDate,
  formatDateTime,
  formatMoney,
  formatMonth,
  formatNumber,
} from '../utils/format'
import { useApiErrorHandler } from '../hooks/useApiErrorHandler'

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
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value))
    return formatDateTime(value)
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value))
    return formatDate(value)
  return crudValueLabels[String(value)] ?? String(value)
}

/** Cột số/tiền/ngày dùng định dạng chung; trạng thái hóa đơn và hợp đồng dùng badge có nhãn. */
function renderColumnCell(
  resource: string,
  column: CrudColumn,
  row: CrudRecord,
) {
  if (['invoices', 'myInvoices'].includes(resource) && column.key === 'status')
    return (
      <InvoiceStatus
        status={String(row.status)}
        dueDate={row.dueDate as string | null}
      />
    )
  if (['deposits', 'myDeposits'].includes(resource) && column.key === 'status')
    return <StatusBadge entity="contract" status={String(row.status)} />
  const raw = readValue(row, column.key)
  if (column.format === 'money' || column.format === 'number') {
    const value = typeof raw === 'number' ? raw : Number(raw)
    if (raw == null || raw === '' || !Number.isFinite(value)) return '—'
    return column.format === 'money'
      ? formatMoney(value)
      : formatNumber(value)
  }
  if (column.format === 'date') return formatDate(raw as string | null)
  if (column.format === 'datetime') return formatDateTime(raw as string | null)
  if (column.format === 'month') return formatMonth(raw as string | null)
  if (raw != null && typeof raw === 'object') {
    return <pre className="dms-detail-json">{JSON.stringify(raw, null, 2)}</pre>
  }
  return <span>{display(raw)}</span>
}

/**
 * Hiển thị giá trị trong chế độ Xem (View mode) chuẩn HR-V5:
 * Trình bày rõ ràng với badge, tag, định dạng tiền, số, ngày tháng, text wrap
 * thay vì các ô input xám disabled khó đọc.
 */
function referenceLabel(field: CrudField, record: CrudRecord, value?: unknown): unknown {
  if (field.name === "priorityCategoryIds" && Array.isArray(record.priorities)) {
    const priority = (record.priorities as CrudRecord[]).find((item) =>
      String(item.categoryId ?? readValue(item, "category.id")) === String(value))
    if (priority) return priority.name ?? priority.categoryName ?? readValue(priority, 'category.name')
  }
  for (const key of crudReferenceLabels[field.name] ?? []) {
    const label = readValue(record, key)
    if (label != null && label !== "") return label
  }
  const reference = record[field.name.replace(/Id$/, "")]
    ?? (field.name === "managementUnitId" ? record.unit : undefined)
  return (reference && typeof reference === "object"
    ? readValue(reference as CrudRecord, field.lookupLabel ?? "name")
    : undefined)
    ?? (field.lookupLabel?.includes(".") ? readValue(record, field.lookupLabel) : undefined)
}

function renderViewFieldValue(field: CrudField, record: CrudRecord) {
  const value = readValue(record, field.name)
  if (field.lookup) {
    const values = Array.isArray(value) ? value : [value]
    return <span>{values.map((item) => {
      const label = referenceLabel(field, record, item)
      return label != null ? display(label) : item == null ? "—" : "#" + String(item)
    }).join(", ") || "—"}</span>
  }
  if (value == null || value === "") return <span>—</span>
  if (field.kind === "boolean" || typeof value === "boolean") {
    return <Tag color={value ? "success" : "default"}>{display(value)}</Tag>
  }

  // Danh mục tùy chọn options
  if (field.options) {
    const matched = field.options.find(
      (opt) => String(opt.value) === String(value),
    )
    if (matched) {
      const label = crudValueLabels[matched.label] ?? matched.label
      return (
        <Tag color="blue" style={{ marginInlineEnd: 0, fontWeight: 500 }}>
          {label}
        </Tag>
      )
    }
  }

  // Ngày tháng
  if (field.kind === 'date') {
    return <span>{formatDate(String(value))}</span>
  }
  if (field.kind === 'datetime') {
    return <span>{formatDateTime(String(value))}</span>
  }

  // Số lượng & tiền tệ
  if (field.kind === 'number' || typeof value === 'number') {
    const num = Number(value)
    const isMoney = /amount|price|cost|fee|total|deposit|balance/i.test(
      field.name,
    )
    return (
      <span
        style={{
          fontFamily: 'var(--font-mono, monospace)',
          fontWeight: 600,
        }}
      >
        {isMoney ? formatMoney(num) : formatNumber(num)}
      </span>
    )
  }

  // Đoạn văn dài textarea
  if (field.kind === 'textarea') {
    return (
      <div
        style={{
          whiteSpace: 'pre-wrap',
          lineHeight: 1.6,
          color: 'var(--color-heading)',
          padding: '8px 12px',
          background: 'var(--color-surface-hover)',
          borderRadius: 'var(--radius-sm)',
          border: '1px solid var(--color-border-light)',
        }}
      >
        {String(value)}
      </div>
    )
  }

  // Mã / ID / Số điện thoại / Email: cho phép sao chép nhanh
  const isCodeOrId = /code|id|username|studentCode|phone|email/i.test(
    field.name,
  )
  if (isCodeOrId && typeof value === 'string') {
    return (
      <Typography.Text copyable={{ tooltips: ['Sao chép', 'Đã chép'] }}>
        {String(value)}
      </Typography.Text>
    )
  }

  return <span>{display(value)}</span>
}

function hasPeriodOpened(value: unknown) {
  return new Date(String(value)).getTime() <= Date.now()
}

function LookupInput({
  field,
  disabled,
  record,
}: {
  field: CrudField
  disabled?: boolean
  record?: CrudRecord
}) {
  const selected = Form.useWatch(field.name, { preserve: true })
  const { data, isLoading, error } = useQuery({
    queryKey: ['crud-lookup', field.lookup, field.lookupFilters],
    queryFn: () => getLookupRecords(field.lookup!, field.lookupFilters),
    // Chi tiết đã có ID/metadata: không tải danh mục khác mà vai trò có thể không được xem.
    enabled: !!field.lookup && !disabled,
    // Danh mục tham chiếu ít đổi; refresh() vẫn invalidate sau mỗi lần ghi.
    staleTime: 5 * 60 * 1000,
  })
  let options = field.lookup
    ? data?.map((row) => ({
        value: row.id,
        label: `${display(readValue(row, field.lookupLabel ?? 'name'))} (#${row.id})`,
      }))
    : field.options?.map((option) => ({
        ...option,
        label: crudValueLabels[option.label] ?? option.label,
      }))
  if (field.lookup) {
    const values: unknown[] = Array.isArray(selected) ? selected : selected == null ? [] : [selected]
    const retained = values
      .filter((value): value is string | number => typeof value === "string" || typeof value === "number")
      .filter((value) => !options?.some((option) => String(option.value) === String(value)))
      .map((value) => {
        const label = referenceLabel(field, record ?? {}, value)
        return { value, label: label == null ? "#" + value : display(label) + " (#" + value + ")" }
      })
    options = [...(options ?? []), ...retained]
  }
  // Form.Item truyền value/onChange qua wrapper nên Select phải nằm trực tiếp trong component này.
  return (
    <Form.Item
      noStyle
      name={field.name}
      rules={[
        {
          required: !disabled && field.required,
          message: `Chọn ${field.label.toLowerCase()}`,
        },
      ]}
    >
      <Select
        aria-label={field.label}
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

/** Form.Item phải bọc control trực tiếp để value/onChange được nối vào Form. */
function renderCrudInput(field: CrudField, disabled?: boolean) {
  switch (field.kind) {
    case 'number':
      return (
        <InputNumber
          disabled={disabled}
          min={field.min ?? 0}
          max={field.max}
          style={{ width: '100%' }}
        />
      )
    case 'date':
      return <Input type="date" disabled={disabled} />
    case 'datetime':
      return <Input type="datetime-local" disabled={disabled} />
    case 'textarea':
      return (
        <TextArea
          rows={4}
          maxLength={field.maxLength}
          showCount
          disabled={disabled}
        />
      )
    case 'boolean':
      return <Switch disabled={disabled} />
    default:
      return <Input maxLength={field.maxLength} disabled={disabled} />
  }
}

export default function CrudPage({ resource }: { resource: string }) {
  const config = resources[resource]
  const { data: me } = useMe()
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams, setSearchParams] = useSearchParams()
  const { id: routeId } = useParams()
  const query = readCrudQuery(searchParams, resource, config)
  const { search, page, size, filters: activeFilters } = query
  const handleApiError = useApiErrorHandler()
  const [modalMode, setModalMode] = useState<'create' | 'edit' | 'view' | 'workflow' | null>(
    null,
  )
  const [activeRecord, setActiveRecord] = useState<CrudRecord | null>(null)
  const [maintenanceCreateOpen, setMaintenanceCreateOpen] = useState(false)
  const isViewMode = modalMode === 'view'
  const editing =
    modalMode === 'edit'
      ? activeRecord
      : modalMode === 'create'
        ? null
        : modalMode === 'view'
          ? activeRecord
          : undefined
  const [busy, setBusy] = useState(false)
  const busyRef = useRef(false)
  const confirmationPendingRef = useRef(false)
  const modalRequestRef = useRef(0)
  useEffect(() => () => { modalRequestRef.current += 1 }, [resource])
  const [form] = Form.useForm()
  const [filterForm] = Form.useForm()
  const scopeValue = Form.useWatch('scope', form)
  const typeValue = Form.useWatch('type', form)
  const userRoleValue = Form.useWatch('role', form)
  const role = me?.role
  const canRead = hasPermission(
    me,
    permissionFor(config.permission, Action.READ),
  )
  const canUpdate =
    !config.readOnly &&
    hasPermission(me, permissionFor(config.permission, Action.UPDATE))
  const canDelete =
    !config.readOnly &&
    hasPermission(
      me,
      resource === 'users'
        ? Permission.USER_LOCK
        : permissionFor(config.permission, Action.DELETE),
    )
  const canCreate =
    !config.readOnly &&
    hasPermission(me, permissionFor(config.permission, Action.CREATE)) &&
    !config.singleton &&
    !config.noCreate
  const canWrite = canCreate || canUpdate || canDelete
  const [paymentInvoice, setPaymentInvoice] = useState<CrudRecord | null>(null)
  const [ledgerRecord, setLedgerRecord] = useState<CrudRecord | null>(null)
  const idKey = config.idKey ?? 'id'
  const writeEndpoint =
    resource === 'students' && role === Role.ADMIN
      ? '/admin/students'
      : (config.writeEndpoint ?? config.endpoint)
  const detailEndpoint = config.detailEndpoint ?? config.endpoint
  const invoiceResource = resource === 'invoices' || resource === 'myInvoices'
  const invoiceItems =
    invoiceResource && Array.isArray(activeRecord?.items)
      ? (activeRecord.items as CrudRecord[])
      : []
  /** Xóa dùng endpoint riêng khi backend không mở DELETE ở endpoint ghi. */
  const deleteEndpoint = config.deleteEndpoint ?? writeEndpoint
  function changeQuery(patch: Partial<typeof query>) {
    setSearchParams((previous) =>
      updateCrudQuery(previous, resource, { ...query, ...patch }),
    )
  }
  const filterSignature = JSON.stringify(activeFilters)
  useEffect(() => {
    if (!config.filters?.length || config.singleton) return
    filterForm.resetFields()
    const initial: Record<string, string | number> = {
      ...JSON.parse(filterSignature),
    }
    for (const field of config.filters ?? []) {
      if (initial[field.name] && (field.kind === 'number' || field.lookup))
        initial[field.name] = Number(initial[field.name])
      else if (initial[field.name] && field.kind === 'datetime')
        initial[field.name] = toLocalDateTime(initial[field.name])
    }
    filterForm.setFieldsValue(initial)
  }, [filterSignature, filterForm, config, resource])
  const { data, error, isLoading, isFetching, refetch } = useQuery({
    queryKey: ['crud', me?.id, resource, page, size, search, activeFilters],
    enabled: canRead,
    queryFn: async () =>
      config.singleton
        ? normalizePage((await api.get(config.endpoint)).data)
        : searchByPage(config.endpoint, createSearchRequest(query, config)),
  })
  const rows = data?.content
  useEffect(() => {
    if (error) {
      filterForm.setFields(
        validationFieldErrors(error).map((item) => ({
          name: item.field,
          errors: [item.message],
        })),
      )
    }
  }, [error, filterForm])
  // Xóa bản ghi cuối trang hoặc mở link quá trang hiện có thì quay về trang cuối hợp lệ.
  const total = data?.totalElements
  useEffect(() => {
    if (total === undefined || isFetching || error || config.singleton) return
    const lastPage = Math.max(1, Math.ceil(total / size))
    if (page > lastPage)
      setSearchParams(
        (previous) =>
          updateCrudQuery(previous, resource, {
            ...readCrudQuery(previous, resource, config),
            page: lastPage,
          }),
        { replace: true },
      )
  }, [total, size, page, resource, config, isFetching, error, setSearchParams])
  const writableFields = (modalMode === "create"
    ? (config.createFields ?? config.fields).filter((field) => !field.editOnly)
    : (config.editFields ?? config.fields).filter((field) => !field.createOnly))
  const baseFields = modalMode === "create"
    ? writableFields
    : [...new Map([...config.fields, ...(config.createFields ?? []), ...(config.editFields ?? [])]
        .filter((field) => !field.sensitive && !crudSecretFieldNames.has(field.name))
        .map((field) => [field.name, field])).values()]
  const fields = baseFields
    .filter(
      (field) =>
        !(
          resource === 'profile' &&
          role !== Role.STUDENT &&
          ['hometown', 'emergencyContact'].includes(field.name)
        ),
    )
    .filter(
      (field) =>
        !(
          resource === 'users' &&
          editing?.role === Role.STUDENT &&
          field.name === 'role'
        ),
    )
    .filter(
      (field) =>
        !(
          resource === 'users' &&
          field.name === 'managementUnitId' &&
          (userRoleValue === Role.ADMIN || editing?.role === Role.STUDENT)
        ),
    )
    .map((field) =>
      field.name === 'contractId' && role === Role.ADMIN
        ? { ...field, lookup: '/contracts' }
        : field,
    )
    .map((field) =>
      resource === 'users' && field.name === 'managementUnitId'
        ? {
            ...field,
            required:
              userRoleValue === Role.STAFF || userRoleValue === Role.TECHNICIAN,
          }
        : field,
    )
  const writeFields = writableFields
    .filter((field) => fields.some((candidate) => candidate.name === field.name))
    .map((field) => ({ ...field, ...fields.find((candidate) => candidate.name === field.name) }))
  const isWritableField = (field: CrudField) =>
    (modalMode === 'create' || modalMode === 'edit') &&
    writeFields.some((candidate) => candidate.name === field.name) &&
    !(modalMode === "edit" && resource === "users" && field.name === "role" && editing?.id === me?.id) &&
    !(modalMode === "edit" && config.closeAtOnlyAfterOpen && editing && hasPeriodOpened(editing.openAt) && field.name !== "closeAt")
  const visible = (field: CrudField) =>
    !field.visibleWhen ||
    (isViewMode
      ? activeRecord?.[field.visibleWhen.field]
      : (field.visibleWhen.field === 'scope' ? scopeValue : typeValue)
        ?? activeRecord?.[field.visibleWhen.field]
        ?? config.createDefaults?.[field.visibleWhen.field]) ===
      field.visibleWhen.value

  const createDefaults = useMemo(
    () => ({
      ...config.createDefaults,
      unitId: me?.managementUnit?.id,
    }),
    [config.createDefaults, me?.managementUnit?.id],
  )

  const formInitialValues = useMemo(() => {
    if (modalMode === 'edit' || modalMode === 'view') {
      return activeRecord
        ? getRecordFormValues(activeRecord, config, resource)
        : undefined
    }
    if (modalMode === 'create') {
      return createDefaults
    }
    return undefined
  }, [modalMode, activeRecord, config, resource, createDefaults])

  useEffect(() => {
    if ((modalMode === 'create' || modalMode === 'edit') && formInitialValues) {
      form.setFieldsValue(formInitialValues)
    }
  }, [modalMode, formInitialValues, form])

  function closeModal() {
    if (busyRef.current) return
    modalRequestRef.current += 1
    setModalMode(null)
    setActiveRecord(null)
    if (modalMode === 'edit' || modalMode === 'create') form.resetFields()
    if (modalMode === 'workflow') void refresh()
    if (routeId) {
      navigate(
        { pathname: '..', search: location.search },
        { relative: 'path' },
      )
    }
  }

  useEffect(() => {
    if (!routeId || !canRead) return
    let cancelled = false
    const request = ++modalRequestRef.current
    api
      .get(`${detailEndpoint}/${routeId}`)
      .then((response) => {
        if (!cancelled && request === modalRequestRef.current && !busyRef.current) {
          const item = response.data as CrudRecord
          setActiveRecord(item)
          setModalMode('view')
        }
      })
      .catch((e) => {
        if (!cancelled) handleApiError(e)
      })
    return () => {
      cancelled = true
    }
  }, [routeId, detailEndpoint, handleApiError, canRead])

  /** Quyền thao tác không thay thế điều kiện trạng thái hoặc người sở hữu bản ghi. */
  function canChangeRow(row: CrudRecord) {
    if (
      resource === 'readings' &&
      (row.invoiced === true || row.hasFollowingMonth === true)
    ) return false
    return (
      (!config.editStates || config.editStates.includes(String(row.status))) &&
      (!config.rowOwned ||
        role === Role.ADMIN ||
        row[config.rowOwned] === me?.id) &&
      !(role === Role.STAFF && ['applications', 'summer'].includes(resource))
    )
  }
  function canEditRow(row: CrudRecord) {
    return (
      canUpdate &&
      canChangeRow(row) &&
      !(resource === 'maintenance' && role === Role.TECHNICIAN)
    )
  }
  function canDeleteRow(row: CrudRecord) {
    return (
      canDelete &&
      canChangeRow(row) &&
      !config.singleton &&
      !config.noDelete &&
      !(resource === 'users' && (row.locked === true || row.id === me?.id))
    )
  }
  async function refresh() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['crud'] }),
      queryClient.invalidateQueries({ queryKey: ['crud-lookup'] }),
      queryClient.invalidateQueries({ queryKey: ['notifications'] }),
      queryClient.invalidateQueries({ queryKey: ['me'] }),
    ])
  }
  function canResetPassword(row: CrudRecord) {
    return (
      ['users', 'students', 'adminStudents'].includes(resource) &&
      hasPermission(
        me,
        resource === 'users'
          ? Permission.USER_RESET_PASSWORD
          : Permission.STUDENT_RESET_PASSWORD,
      ) &&
      row.id !== me?.id
    )
  }
  function canConfirmInvoicePayment(row: CrudRecord) {
    // Trùng điều kiện allowedActions của GET /api/invoices/{id}: chỉ hóa đơn chưa thu.
    return (
      resource === 'invoices' &&
      canUpdate &&
      row.status === InvoiceState.UNPAID
    )
  }
  function showTemporaryPassword(result: unknown, username?: unknown) {
    if (
      !result ||
      typeof result !== 'object' ||
      !('temporaryPassword' in result)
    )
      return false
    const password = (result as CrudRecord).temporaryPassword
    if (typeof password !== 'string' || !password) return false
    // Chỉ hiện trong hộp thoại này; không đưa mật khẩu tạm vào cache danh sách hoặc storage.
    modal.info({
      title: 'Mật khẩu tạm (hiển thị một lần)',
      content: (
        <>
          {!!username && (
            <Typography.Paragraph>
              Tài khoản: {String(username)}
            </Typography.Paragraph>
          )}
          <Typography.Paragraph copyable>{password}</Typography.Paragraph>
          <Typography.Paragraph>
            Cung cấp mật khẩu này cho đúng chủ tài khoản. Người dùng phải đổi
            mật khẩu ở lần đăng nhập tiếp theo.
          </Typography.Paragraph>
        </>
      ),
      okText: 'Đã lưu mật khẩu tạm',
    })
    return true
  }
  function resetPassword(row: CrudRecord) {
    modal.confirm({
      title: `Đặt lại mật khẩu ${String(row.username ?? row.studentCode ?? row.fullName)}?`,
      content:
        'Các phiên đăng nhập cũ sẽ mất hiệu lực. Mật khẩu tạm chỉ hiển thị một lần và người dùng phải đổi mật khẩu khi đăng nhập lại.',
      okText: 'Đặt lại mật khẩu',
      cancelText: 'Hủy',
      onOk: async () => {
        try {
          // Màn tài khoản dùng API User; màn sinh viên kiểm tra quyền/phạm vi tại API Student.
          const endpoint = resource === 'users' ? '/admin/users' : '/students'
          const result = await writeCrudRecord(
            `${endpoint}/${row.id}/reset-password`,
            'post',
          )
          if (!showTemporaryPassword(result, row.username ?? row.studentCode))
            throw new Error('Thiếu mật khẩu tạm')
          await refresh()
        } catch (e) {
          handleApiError(e)
          throw e
        }
      },
    })
  }
  async function openEditor(row?: CrudRecord) {
    if (busyRef.current) return
    const request = ++modalRequestRef.current
    if (row ? !canEditRow(row) : !canCreate) return
    if (!row) {
      setActiveRecord(null)
      setModalMode('create')
      return
    }
    try {
      const selected =
        config.singleton ||
        config.useListRecordForEdit ||
        config.inlineDetail
          ? row
          : ((await api.get(`${detailEndpoint}/${row[idKey]}`))
              .data as CrudRecord)
      if (request !== modalRequestRef.current || busyRef.current) return
      setActiveRecord(selected)
      setModalMode('edit')
    } catch (e) {
      handleApiError(e)
    }
  }
  async function saveRecord(values: Record<string, unknown>) {
    if (busyRef.current || !['create', 'edit'].includes(modalMode ?? '')) return
    if (editing ? !canEditRow(editing) : !canCreate) return
    busyRef.current = true
    const request = modalRequestRef.current
    setBusy(true)
    try {
      const payload: Record<string, unknown> = {}
      for (const field of writeFields.filter(visible).filter(isWritableField)) {
        const value = values[field.name]
        // Chuỗi rỗng xóa nội dung tùy chọn; trường bỏ qua giữ nguyên giá trị khi sửa.
        if (
          value == null ||
          (value === '' &&
            (field.required ||
              !['text', 'textarea'].includes(field.kind ?? 'text')))
        )
          continue
        payload[field.name] =
          field.kind === 'datetime'
            ? new Date(String(value)).toISOString()
            : value
      }
      // Các DTO sửa chỉ số/thông báo cần giữ lại định danh/phạm vi đã chọn khi tạo.
      if (editing && config.keepCreateOnlyOnEdit && ['readings', 'notifications'].includes(resource))
        for (const field of config.fields.filter((f) => f.createOnly))
          if (editing[field.name] != null)
            payload[field.name] = editing[field.name]
      if (
        config.closeAtOnlyAfterOpen &&
        editing &&
        hasPeriodOpened(editing.openAt)
      ) {
        for (const key of Object.keys(payload))
          if (key !== 'closeAt') delete payload[key]
      }
      // ID nằm trong body lưu chung; hồ sơ cá nhân luôn lấy chủ sở hữu từ phiên đăng nhập.
      if (editing && !config.singleton) payload[idKey] = editing[idKey]
      let body: unknown = payload
      if (config.createFormat === 'multipart') {
        const multipart = new FormData()
        multipart.append(
          'data',
          new Blob([JSON.stringify(payload)], { type: 'application/json' }),
        )
        body = multipart
      }
      const result = await saveOrUpdate(writeEndpoint, body)
      if (request !== modalRequestRef.current) return
      if (
        !showTemporaryPassword(result, payload.username ?? payload.studentCode)
      )
        void message.success('Đã lưu')
      busyRef.current = false
      closeModal()
      await refresh()
    } catch (e) {
      if (request !== modalRequestRef.current) return
      form.setFields(
        validationFieldErrors(e).map((item) => ({
          name: item.field,
          errors: [item.message],
        })),
      )
      handleApiError(e)
    } finally {
      busyRef.current = false
      setBusy(false)
    }
  }
  function deleteRecord(row: CrudRecord) {
    if (!canDeleteRow(row)) return
    let reason = ''
    modal.confirm({
      title: `${config.deleteLabel ?? 'Xóa'} bản ghi này?`,
      content:
        config.deleteAction === 'lock' ? (
          <TextArea
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
            `${deleteEndpoint}/${row[idKey]}${config.deleteAction ? `/${config.deleteAction}` : ''}`,
            config.deleteAction ? 'post' : 'delete',
            config.deleteAction === 'lock' ? { reason } : undefined,
          )
          void message.success('Đã cập nhật')
          await refresh()
        } catch (e) {
          handleApiError(e)
          throw e
        }
      },
    })
  }
  function submitRecord(values: Record<string, unknown>) {
    if (busyRef.current || confirmationPendingRef.current) return
    const identityChanged =
      resource === 'users' &&
      editing &&
      (values.role !== editing.role ||
        (values.managementUnitId != null &&
          values.managementUnitId !== editing.managementUnitId))
    if (!identityChanged) {
      void saveRecord(values)
      return
    }
    const request = modalRequestRef.current
    confirmationPendingRef.current = true
    modal.confirm({
      title: 'Thay đổi vai trò hoặc đơn vị tài khoản?',
      content:
        'Phiên đăng nhập cũ của tài khoản này sẽ mất hiệu lực. Người dùng cần đăng nhập lại để nhận quyền và phạm vi mới.',
      okText: 'Lưu thay đổi',
      cancelText: 'Hủy',
      onCancel: () => { confirmationPendingRef.current = false },
      onOk: async () => {
        try {
          if (request === modalRequestRef.current) await saveRecord(values)
        } finally {
          confirmationPendingRef.current = false
        }
      },
    })
  }

  /** Thay đổi quyền đọc tổng hợp vẫn giữ quyền ghi trong đơn vị và thu hồi phiên cũ. */
  function changeCrossUnitAccess(row: CrudRecord) {
    if (
      resource !== 'users' ||
      row.role !== Role.STAFF ||
      !hasPermission(me, Permission.USER_CROSS_UNIT_ACCESS)
    )
      return
    const enabled = row.crossUnitAccess !== true
    modal.confirm({
      title: enabled
        ? 'Cấp quyền đọc liên đơn vị?'
        : 'Thu hồi quyền đọc liên đơn vị?',
      content:
        'Chỉ thay đổi phạm vi đọc; tài khoản vẫn ghi trong đơn vị được giao. Người dùng cần đăng nhập lại sau khi thay đổi.',
      okText: enabled ? 'Cấp quyền' : 'Thu hồi',
      cancelText: 'Hủy',
      onOk: async () => {
        try {
          await writeCrudRecord(
            `/admin/users/${row.id}/cross-unit-access`,
            'put',
            { enabled },
          )
          void message.success('Đã cập nhật quyền đọc tổng hợp')
          await refresh()
        } catch (failure) {
          handleApiError(failure)
          throw failure
        }
      },
    })
  }

  function unlockAccount(row: CrudRecord) {
    modal.confirm({
      title: `Mở khóa tài khoản ${String(row.username)}?`,
      content:
        'Tài khoản có thể đăng nhập lại. Các token đã bị thu hồi vẫn không dùng lại được.',
      okText: 'Mở khóa',
      cancelText: 'Hủy',
      onOk: async () => {
        try {
          await writeCrudRecord(`/admin/users/${row.id}/unlock`, 'post')
          void message.success('Đã mở khóa tài khoản')
          await refresh()
        } catch (failure) {
          handleApiError(failure)
          throw failure
        }
      },
    })
  }
  async function showDetail(row: CrudRecord) {
    if (busyRef.current) return
    const request = ++modalRequestRef.current
    if (resource === 'deposits' || resource === 'myDeposits') {
      setLedgerRecord(row)
      return
    }
    try {
      const selected =
        config.singleton ||
        config.useListRecordForEdit ||
        config.inlineDetail
          ? row
          : ((await api.get(`${detailEndpoint}/${row[idKey]}`))
                .data as CrudRecord)
      if (request !== modalRequestRef.current || busyRef.current) return
      setActiveRecord(selected)
      setModalMode('view')
    } catch (e) {
      handleApiError(e)
    }
  }
  /** Trường hiển thị trong form; giữ nguyên điều kiện ẩn theo nghiệp vụ của màn hiện tại. */
  const formFields = fields
    .filter(visible)
    .filter(
      (field) =>
        !(
          modalMode === 'create' &&
          resource === 'maintenance' &&
          role === Role.STUDENT &&
          field.name === 'roomId'
        ),
    )
  const sectionedFieldNames = new Set(
    config.formSections?.flatMap((section) => section.fields) ?? [],
  )
  const remainingFields = formFields.filter(
    (field) => !sectionedFieldNames.has(field.name),
  )

  const displayRecord = activeRecord
    ? { ...activeRecord, ...getRecordFormValues(activeRecord, config, resource) }
    : {}
  const detailColumns = [...new Map([...config.columns, ...(config.detailColumns ?? [])]
    .filter((column) => !crudSecretFieldNames.has(column.key))
    .filter((column) => !formFields.some((field) => field.name === column.key))
    .map((column) => [column.key, column])).values()]

  function renderField(field: CrudField) {
    const isFieldDisabled = !isWritableField(field)
    if (isFieldDisabled) return (
      <Form.Item key={field.name} label={field.label}>
        {renderViewFieldValue(field, displayRecord)}
      </Form.Item>
    )

    return field.kind === 'select' ? (
      <Form.Item
        key={field.name}
        label={field.label}
        required={!isFieldDisabled && field.required}
        extra={!isFieldDisabled ? field.hint : undefined}
      >
        <LookupInput
          field={field}
          record={activeRecord ?? undefined}
          disabled={busy || isFieldDisabled}
        />
      </Form.Item>
    ) : (
      <Form.Item
        key={field.name}
        name={field.name}
        label={field.label}
        className={
          field.kind === 'textarea' ? 'dms-form-span-full' : undefined
        }
        extra={!isFieldDisabled ? field.hint : undefined}
        valuePropName={field.kind === 'boolean' ? 'checked' : 'value'}
        rules={
          isFieldDisabled
            ? []
            : [
                {
                  required: field.required,
                  whitespace:
                    field.required &&
                    !['number', 'boolean', 'date', 'datetime'].includes(
                      field.kind ?? '',
                    ),
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
              ]
        }
      >
        {renderCrudInput(field, busy || isFieldDisabled)}
      </Form.Item>
    )
  }

  if (!canRead)
    return (
      <Result
        status="403"
        title="Không có quyền xem màn hình này"
        subTitle="Quyền thao tác được quản lý theo vai trò tại máy chủ."
      />
    )

  return (
    <div className={config.finance ? 'dms-finance-page' : undefined}>
      <PageHeader
        title={config.title}
        subtitle={config.subtitle ?? 'Tìm kiếm và quản lý dữ liệu'}
        extra={
          <Space>
            <Button
              icon={<ReloadOutlined />}
              loading={isFetching}
              onClick={() => {
                void refetch()
              }}
            >
              Làm mới
            </Button>
            {canCreate && (
              <Button
                type="primary"
                onClick={() => {
                  if (resource === 'maintenance') setMaintenanceCreateOpen(true)
                  else void openEditor()
                }}
              >
                {config.createLabel ?? 'Thêm mới'}
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
      <Card className={config.finance ? 'dms-finance-card' : undefined}>
        {!config.singleton && (
          <div
            className={
              'dms-toolbar' + (config.finance ? ' dms-finance-toolbar' : '')
            }
          >
            {!config.singleton && (
              <SearchInput
                aria-label={`Tìm kiếm ${config.title.toLowerCase()}`}
                placeholder={config.searchPlaceholder ?? 'Nhập từ khóa tìm kiếm'}
                allowClear
                enterButton="Tìm kiếm"
                key={`${resource}:${search}`}
                defaultValue={search}
                onSearch={(value) => {
                  changeQuery({ search: value.trim(), page: 1 })
                }}
              />
            )}
            {!!config.filters?.length && (
              <Form
                form={filterForm}
                layout="vertical"
                onFinish={(values) => {
                  const filters: Record<string, string> = {}
                  for (const field of config.filters ?? [])
                    if (values[field.name] != null && values[field.name] !== '')
                      filters[field.name] = String(values[field.name]).trim()
                  changeQuery({ filters, page: 1 })
                }}
              >
                <FilterBar>
                  {config.filters.map((field) => (
                    <Form.Item
                      key={field.name}
                      label={field.label}
                      name={field.kind === 'select' ? undefined : field.name}
                    >
                      {field.kind === 'select' ? (
                        <LookupInput field={{ ...field, required: false }} />
                      ) : (
                        renderCrudInput(field)
                      )}
                    </Form.Item>
                  ))}
                  <div className="dms-filter-actions">
                    <Button
                      type="primary"
                      htmlType="submit"
                      loading={isFetching}
                    >
                      Áp dụng bộ lọc
                    </Button>
                    <Button
                      onClick={() => {
                        filterForm.resetFields()
                        changeQuery({ filters: {}, search: '', page: 1 })
                      }}
                    >
                      Xóa bộ lọc
                    </Button>
                  </div>
                </FilterBar>
              </Form>
            )}
          </div>
        )}
        {error ? (
          <Alert
            type="error"
            showIcon
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
            locale={
              config.emptyTitle
                ? {
                    emptyText: (
                      <EmptyState
                        title={config.emptyTitle}
                        description={config.emptyHint}
                      />
                    ),
                  }
                : undefined
            }
            pagination={
              config.singleton
                ? false
                : {
                    current: page,
                    pageSize: size,
                    total: data?.totalElements,
                    showSizeChanger: true,
                    pageSizeOptions: CRUD_PAGE_SIZES,
                    showTotal: (count) => `Tổng ${count} bản ghi`,
                    onChange: (nextPage, nextSize) =>
                      changeQuery({
                        page: nextSize === size ? nextPage : 1,
                        size: nextSize,
                      }),
                  }
            }
            columns={[
              ...config.columns.map((column) => ({
                title: column.label,
                key: column.key,
                align:
                  column.align ??
                  (column.format === 'money' || column.format === 'number'
                    ? 'right'
                    : undefined),
                className: [
                  column.format === 'money' || column.format === 'number'
                    ? 'dms-table-col-number'
                    : '',
                  column.key === 'status' ? 'dms-table-col-status' : '',
                ]
                  .filter(Boolean)
                  .join(' ') || undefined,
                render: (_: unknown, row: CrudRecord) =>
                  renderColumnCell(resource, column, row),
              })),
              {
                title: 'Thao tác',
                key: 'actions',
                width: canWrite ? 150 : 64,
                render: (_: unknown, row: CrudRecord) => (
                  <Space size={4} wrap={false}>
                    <Tooltip title="Xem chi tiết">
                      <Button
                        size="small"
                        icon={<EyeOutlined />}
                        onClick={() => {
                          void showDetail(row)
                        }}
                        aria-label="Xem chi tiết"
                      />
                    </Tooltip>
                    {canEditRow(row) && (
                      <Tooltip title="Sửa">
                        <Button
                          size="small"
                          icon={<EditOutlined />}
                          onClick={() => {
                            void openEditor(row)
                          }}
                          aria-label="Sửa"
                        />
                      </Tooltip>
                    )}
                    {canDeleteRow(row) && (
                      <Tooltip title={config.deleteLabel ?? 'Xóa'}>
                        <Button
                          size="small"
                          danger
                          icon={<DeleteOutlined />}
                          onClick={() => deleteRecord(row)}
                          aria-label={config.deleteLabel ?? 'Xóa'}
                        />
                      </Tooltip>
                    )}
                    {canResetPassword(row) && (
                      <Tooltip title="Đặt lại mật khẩu">
                        <Button
                          size="small"
                          icon={<KeyOutlined />}
                          onClick={() => resetPassword(row)}
                          aria-label="Đặt lại mật khẩu"
                        />
                      </Tooltip>
                    )}
                    {resource === 'users' &&
                      hasPermission(me, Permission.USER_UNLOCK) &&
                      row.locked === true && (
                        <Tooltip title="Mở khóa tài khoản">
                          <Button
                            size="small"
                            icon={<UnlockOutlined />}
                            onClick={() => unlockAccount(row)}
                            aria-label="Mở khóa tài khoản"
                          />
                        </Tooltip>
                      )}
                    {resource === 'users' &&
                      row.role === Role.STAFF &&
                      hasPermission(me, Permission.USER_CROSS_UNIT_ACCESS) && (
                        <Tooltip
                          title={
                            row.crossUnitAccess
                              ? 'Thu hồi quyền đọc tổng hợp'
                              : 'Cấp quyền đọc tổng hợp'
                          }
                        >
                          <Button
                            size="small"
                            icon={<GlobalOutlined />}
                            onClick={() => changeCrossUnitAccess(row)}
                            aria-label="Quyền đọc tổng hợp"
                          />
                        </Tooltip>
                      )}
                    {canConfirmInvoicePayment(row) && (
                      <Tooltip title="Xác nhận đã thu tiền">
                        <Button
                          size="small"
                          type="primary"
                          icon={<DollarCircleOutlined />}
                          onClick={() => setPaymentInvoice(row)}
                          aria-label="Xác nhận đã thu tiền"
                        />
                      </Tooltip>
                    )}
                  </Space>
                ),
              },
            ]}
          />
        )}
      </Card>
      <Modal
        title={
          modalMode === 'workflow'
            ? crudDetailUiLabels.workflow
            : modalMode === 'view'
            ? `Chi tiết ${config.title.toLowerCase()}`
            : `${modalMode === 'edit' ? 'Sửa' : 'Thêm'} ${config.title.toLowerCase()}`
        }
        open={modalMode !== null}
        forceRender
        onCancel={closeModal}
        confirmLoading={busy}
        closable={!busy}
        keyboard={!busy}
        mask={{ closable: !busy }}
        cancelButtonProps={{ disabled: busy }}
        okText="Lưu"
        cancelText="Đóng"
        width={hasRecordDetailContent(resource) ? 1080 : 780}
        style={{ top: 24 }}
        styles={{ body: { maxHeight: 'calc(100dvh - 200px)', overflowY: 'auto', overflowX: 'hidden' } }}
        className={config.finance ? 'dms-finance-modal' : undefined}
        footer={
          <Space>
            <Button onClick={closeModal} disabled={busy}>Đóng</Button>
            {isViewMode && activeRecord && hasRecordDetailContent(resource) && (
              <Button onClick={() => setModalMode('workflow')}>{crudDetailUiLabels.workflow}</Button>
            )}
            {isViewMode && activeRecord && canEditRow(activeRecord) && (
              <Button type="primary" icon={<EditOutlined />} onClick={() => setModalMode("edit")}>
                Chuyển sang sửa
              </Button>
            )}
            {(modalMode === "create" || modalMode === "edit") && (
              <Button type="primary" loading={busy} onClick={() => form.submit()}>Lưu</Button>
            )}
          </Space>
        }
      >
        {activeRecord && !invoiceResource && !hasRecordDetailContent(resource) && (
          <div className="dms-record-context-card">
            <div className="dms-context-header">
              <div className="dms-context-badge-group">
                <span className="dms-context-tag-resource">{config.title}</span>
                <span className="dms-context-code">
                  {String(
                    activeRecord.code ??
                      activeRecord.contractCode ??
                      activeRecord.studentCode ??
                      activeRecord.roomCode ??
                      `#${activeRecord.id ?? ''}`,
                  )}
                </span>
                {Boolean(activeRecord.status) && (
                  <StatusBadge
                    entity={
                      ['summer', 'extensions', 'transfers'].includes(resource)
                        ? 'request'
                        : resource === 'contracts' || resource === 'myContracts'
                          ? 'contract'
                          : resource === 'maintenance' ||
                              resource === 'myMaintenance'
                            ? 'ticket'
                            : resource === 'invoices' ||
                                resource === 'myInvoices'
                              ? 'invoice'
                              : resource === 'storage'
                                ? 'storage'
                                : 'application'
                    }
                    status={String(activeRecord.status)}
                  />
                )}
              </div>
              {Boolean(activeRecord.submittedAt || activeRecord.createdAt) && (
                <span className="dms-context-timestamp">
                  Gửi lúc:{' '}
                  {formatDateTime(
                    String(resource === 'summer' ? activeRecord.createdAt : activeRecord.submittedAt ?? activeRecord.createdAt),
                  )}
                </span>
              )}
            </div>

            <div className="dms-context-meta-row">
              {Boolean(
                activeRecord.fullName ||
                  activeRecord.studentName ||
                  (activeRecord.student as any)?.fullName,
              ) && (
                <div className="dms-context-meta-item">
                  <span className="dms-context-meta-label">Sinh viên:</span>
                  <strong>
                    {String(
                      activeRecord.fullName ??
                        activeRecord.studentName ??
                        (activeRecord.student as any)?.fullName,
                    )}
                    {Boolean(
                      activeRecord.studentCode ||
                        (activeRecord.student as any)?.studentCode,
                    ) && (
                      <span
                        style={{
                          fontWeight: 400,
                          color: 'var(--color-text-muted)',
                        }}
                      >
                        {' '}(
                        {String(
                          activeRecord.studentCode ??
                            (activeRecord.student as any)?.studentCode,
                        )}
                        )
                      </span>
                    )}
                  </strong>
                </div>
              )}
              {Boolean(
                activeRecord.contractCode &&
                  activeRecord.code !== activeRecord.contractCode,
              ) && (
                <div className="dms-context-meta-item">
                  <span className="dms-context-meta-label">Hợp đồng:</span>
                  <strong>{String(activeRecord.contractCode)}</strong>
                </div>
              )}
              {Boolean(
                activeRecord.semesterName ||
                  (activeRecord.semester as any)?.name,
              ) && (
                <div className="dms-context-meta-item">
                  <span className="dms-context-meta-label">Học kỳ:</span>
                  <strong>
                    {String(
                      activeRecord.semesterName ??
                        (activeRecord.semester as any)?.name,
                    )}
                  </strong>
                </div>
              )}
              {Boolean(
                activeRecord.roomCode ||
                  (activeRecord.room as any)?.code,
              ) && (
                <div className="dms-context-meta-item">
                  <span className="dms-context-meta-label">Phòng:</span>
                  <strong>
                    {String(
                      activeRecord.roomCode ??
                        (activeRecord.room as any)?.code,
                    )}
                  </strong>
                </div>
              )}
            </div>
          </div>
        )}
        {activeRecord && hasRecordDetailContent(resource) && (
          <RecordDetailContent
            key={resource + ":" + String(activeRecord[idKey]) + ":" + modalMode}
            resource={resource}
            recordId={Number(activeRecord[idKey])}
            role={role}
            readOnly={modalMode !== "workflow"}
          />
        )}
        {activeRecord && modalMode !== 'workflow' && !hasRecordDetailContent(resource) && (
          <>
            <div className="dms-view-grid">
              {detailColumns.map((column) => (
                <div key={column.key} className="dms-view-field">
                  <div className="dms-view-label">{column.label}</div>
                  <div className="dms-view-value">{renderColumnCell(resource, column, activeRecord)}</div>
                </div>
              ))}
            </div>
            {['items', 'transactions', 'timeline'].map((key) =>
                !(key === "items" && invoiceResource) && Array.isArray(activeRecord[key]) ? (
                  <Card key={key} title={crudDetailUiLabels.collections[key as keyof typeof crudDetailUiLabels.collections]}>
                    {(activeRecord[key] as CrudRecord[]).map((item, index) => (
                      <Typography.Paragraph key={index}>{display(item)}</Typography.Paragraph>
                    ))}
                  </Card>
                ) : null,
              )}
          </>
        )}
        {modalMode !== 'workflow' && invoiceResource && activeRecord ? (
          <div className="dms-invoice-detail">
            <div className="dms-invoice-detail-head">
              <div className="dms-wrap">
                <span className="dms-detail-eyebrow">{config.title}</span>
                <h3 className="dms-detail-code">
                  {String(activeRecord.code ?? '—')}
                </h3>
                <div className="dms-invoice-meta">
                  <InvoiceStatus
                    status={String(activeRecord.status)}
                    dueDate={activeRecord.dueDate as string | null}
                  />
                  <span>{display(activeRecord.type)}</span>
                  {activeRecord.billingMonth ? (
                    <span>
                      {billingUiLabels.invoiceMonth}:{' '}
                      <strong>
                        {formatMonth(activeRecord.billingMonth as string)}
                      </strong>
                    </span>
                  ) : null}
                  <span>
                    {billingUiLabels.invoiceDue}:{' '}
                    <strong>{formatDate(activeRecord.dueDate as string)}</strong>
                  </span>
                  {activeRecord.paidAt ? (
                    <span>
                      {billingUiLabels.invoicePaidAt}:{' '}
                      <strong>
                        {formatDateTime(activeRecord.paidAt as string)}
                      </strong>
                    </span>
                  ) : null}
                </div>
              </div>
              <div className="dms-invoice-detail-amount">
                <span>{billingUiLabels.invoiceTotal}</span>
                <strong>
                  {formatMoney(Number(activeRecord.totalAmount ?? 0))}
                </strong>
              </div>
            </div>
            <Card title={billingUiLabels.invoiceItems}>
              {invoiceItems.length > 0 ? (
                <Table<CrudRecord>
                  dataSource={invoiceItems}
                  rowKey={(row) => String(row.id ?? JSON.stringify(row))}
                  pagination={false}
                  size="small"
                  scroll={{ x: 560 }}
                  columns={[
                    {
                      title: billingUiLabels.itemType,
                      key: 'itemType',
                      render: (_, row) => display(row.itemType),
                    },
                    {
                      title: billingUiLabels.quantity,
                      dataIndex: 'quantity',
                      align: 'right',
                      className: 'dms-table-col-number',
                      render: (value) => formatNumber(Number(value ?? 0)),
                    },
                    {
                      title: billingUiLabels.unitPrice,
                      dataIndex: 'unitPrice',
                      align: 'right',
                      className: 'dms-table-col-number',
                      render: (value) => formatMoney(Number(value ?? 0)),
                    },
                    {
                      title: billingUiLabels.amount,
                      dataIndex: 'amount',
                      align: 'right',
                      className: 'dms-table-col-number',
                      render: (value) => formatMoney(Number(value ?? 0)),
                    },
                    {
                      title: billingUiLabels.note,
                      dataIndex: 'note',
                      render: (value) => display(value),
                    },
                  ]}
                />
              ) : (
                <EmptyState title={billingUiLabels.invoiceNoItems} />
              )}
            </Card>
          </div>
        ) : null}
          <Form
            form={form}
            component={modalMode === 'create' || modalMode === 'edit' ? 'form' : 'div'}
            style={modalMode === null || modalMode === 'workflow' ? { display: 'none' } : undefined}
            key={resource}
            disabled={busy}
            layout="vertical"
            initialValues={formInitialValues}
            onFinish={(values) => {
              submitRecord(values)
            }}
            preserve={false}
          >
            {config.formSections ? (
              <div className="dms-form-sections">
                {config.formSections.map((section) => {
                  const sectionFields = formFields.filter((field) =>
                    section.fields.includes(field.name),
                  )
                  if (sectionFields.length === 0) return null
                  return (
                    <section
                      className="dms-form-section"
                      key={section.title}
                      aria-label={section.title}
                    >
                      <div className="dms-form-section-head">
                        <h3 className="dms-form-section-title">
                          {section.title}
                        </h3>
                        {section.note && (
                          <span className="dms-form-section-note">
                            {section.note}
                          </span>
                        )}
                      </div>
                      <FormGrid
                        className={
                          sectionFields.length === 1
                            ? 'dms-form-grid-single'
                            : undefined
                        }
                      >
                        {sectionFields.map(renderField)}
                      </FormGrid>
                    </section>
                  )
                })}
                {remainingFields.length > 0 && (
                  <section className="dms-form-section" aria-label="Khác">
                    <FormGrid
                      className={
                        remainingFields.length < 3
                          ? 'dms-form-grid-single'
                          : undefined
                      }
                    >
                      {remainingFields.map(renderField)}
                    </FormGrid>
                  </section>
                )}
              </div>
            ) : (
              <FormGrid
                className={
                  formFields.length < 3 ? 'dms-form-grid-single' : undefined
                }
              >
                {formFields.map(renderField)}
              </FormGrid>
            )}
          </Form>

      </Modal>
      {paymentInvoice && (
        <ConfirmPaymentModal
          invoiceId={Number(paymentInvoice.id)}
          invoiceCode={paymentInvoice.code == null ? undefined : String(paymentInvoice.code)}
          totalAmount={
            paymentInvoice.totalAmount == null
              ? undefined
              : Number(paymentInvoice.totalAmount)
          }
          open
          onClose={() => setPaymentInvoice(null)}
          onSuccess={() => {
            if (activeRecord?.[idKey] === paymentInvoice[idKey]) {
              void showDetail(paymentInvoice)
            }
            void refresh()
          }}
        />
      )}
      {ledgerRecord && (
        <DepositLedgerModal
          contractId={Number(ledgerRecord.contractId)}
          contractCode={
            ledgerRecord.contractCode == null
              ? undefined
              : String(ledgerRecord.contractCode)
          }
          studentName={
            ledgerRecord.studentName == null
              ? undefined
              : String(ledgerRecord.studentName)
          }
          open
          onClose={() => setLedgerRecord(null)}
          readOnly
        />
      )}
      {resource === 'maintenance' && (
        <CreateTicketModal
          open={maintenanceCreateOpen}
          onClose={() => setMaintenanceCreateOpen(false)}
          onCreated={() => {
            setMaintenanceCreateOpen(false)
            void refetch()
          }}
        />
      )}
    </div>
  )
}
