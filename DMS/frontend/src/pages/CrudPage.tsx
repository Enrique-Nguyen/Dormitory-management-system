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
  DetailLayout,
  FilterBar,
  FormGrid,
  FormSection,
  ListToolbar,
  StatusFilter,
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
  DownloadOutlined,
  UploadOutlined,
  DollarCircleOutlined,
  EditOutlined,
  EyeOutlined,
  GlobalOutlined,
  KeyOutlined,
  MoreOutlined,
  PlusOutlined,
  SaveOutlined,
  UnlockOutlined,
} from '@ant-design/icons'
import { useQueries, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Dropdown,
  Form,
  Result,
  Space,
  Tag,
  Tooltip,
  Typography,
  type MenuProps,
} from 'antd'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import RecordDetailContent, { hasRecordDetailContent } from '../features/crud/RecordDetailContent'
import {
  useBlocker,
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from 'react-router'
import { useMe } from '../features/account/useMe'
import {
  getLookupRecords,
  getRecordFormValues,
  isNumericField,
  isFieldVisible,
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
import { resources, billingUiLabels, crudDetailUiLabels, crudReferenceLabels, crudSecretFieldNames, crudStatusEntities, excelUiLabels, studentImportLabels } from '../features/crud/resources'
import StudentImportPanel from '../features/account/StudentImportPanel'
import CrudExcelPanel from '../features/crud/CrudExcelPanel'
import ReferenceSelect, { ReferenceLabel } from '../features/crud/ReferenceSelect'
import { downloadWorkbook, exportFilteredRecords } from '../features/crud/excel'
import { BillingAction, crudValueLabels, getStatus } from '../constants/statusConfig'
import type { CrudColumn, CrudField, CrudFormSection, CrudRecord } from '../features/crud/types'
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

function UserCell({ name, code, sub }: { name: string; code?: string; sub?: string }) {
  const cleanName = String(name || '').trim()
  const words = cleanName.split(/\s+/).filter(Boolean)
  const initials = words.length >= 2
    ? (words[0][0] + words[words.length - 1][0]).toUpperCase()
    : cleanName.slice(0, 2).toUpperCase() || '—'
  return (
    <div className="dms-user-cell">
      <span className="dms-user-avatar">{initials}</span>
      <div className="dms-user-info">
        <span className="dms-user-name">{cleanName}</span>
        {(code || sub) && <span className="dms-user-sub font-mono text-xs">{sub ?? code}</span>}
      </div>
    </div>
  )
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
  if (column.key === 'status' && crudStatusEntities[resource])
    return <StatusBadge entity={crudStatusEntities[resource]} status={String(row.status)} />
  const raw = readValue(row, column.key)

  // Cột Mã: font-mono text-xs
  const isCode =
    column.key === 'code' ||
    column.key === 'studentCode' ||
    column.key === 'roomCode' ||
    column.key === 'contractCode' ||
    column.key === 'invoiceCode' ||
    column.key === 'ticketCode' ||
    column.key === 'username' ||
    (typeof column.key === 'string' && column.key.toLowerCase().endsWith('code'))
  if (isCode && raw != null && raw !== '') {
    return <span className="font-mono text-xs dms-cell-code">{String(raw)}</span>
  }

  // Cột Tên / Nhân sự / Sinh viên: UserCell
  const isUserCol =
    column.key === 'fullName' ||
    column.key === 'student.fullName' ||
    column.key === 'studentName' ||
    column.key === 'user.fullName' ||
    column.key === 'requester' ||
    column.key === 'creator' ||
    column.key === 'proposer' ||
    column.key === 'student' ||
    (resource === 'users' && (column.key === 'fullName' || column.key === 'name')) ||
    (resource === 'students' && (column.key === 'fullName' || column.key === 'name')) ||
    (resource === 'adminStudents' && (column.key === 'fullName' || column.key === 'name'))

  if (isUserCol && raw != null && raw !== '') {
    const code =
      (row.studentCode as string) ??
      (row.code as string) ??
      (row.username as string) ??
      (readValue(row, 'student.studentCode') as string)
    return <UserCell name={String(raw)} code={code && code !== raw ? code : undefined} />
  }
  if (resource === 'maintenance' && column.key === 'category')
    return <ReferenceLabel endpoint="/catalogs/maintenance-categories" value={raw} />
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
  if (field.name === 'amenityIds') return undefined
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

function renderViewFieldValue(field: CrudField, record: CrudRecord, resource?: string) {
  const value = readValue(record, field.name)
  if (field.name === 'amenityIds') return <span className="dms-view-value">{display(record.amenities)}</span>
  if (field.lookup) {
    const values = Array.isArray(value) ? value : [value]
    return <span className="dms-view-value">{values.map((item) => {
      const label = referenceLabel(field, record, item)
      return label != null ? display(label) : item == null ? "—" : "#" + String(item)
    }).join(", ") || "—"}</span>
  }
  if (value == null || value === "") return <span className="dms-view-empty">—</span>

  // Trạng thái theo entity chuẩn DMS
  if (field.name === 'status' && resource && crudStatusEntities[resource]) {
    return (
      <StatusBadge
        entity={crudStatusEntities[resource]}
        status={String(value)}
      />
    )
  }

  if (field.kind === "boolean" || typeof value === "boolean") {
    return <Tag color={value ? "success" : "default"} style={{ marginInlineEnd: 0 }}>{value ? 'Có' : 'Không'}</Tag>
  }

  // Danh mục tùy chọn options
  if (field.options) {
    const matched = field.options.find(
      (opt) => String(opt.value) === String(value),
    )
    if (matched) {
      const label = crudValueLabels[matched.label] ?? matched.label
      const isStatusField = /status|trangThai|state/i.test(field.name)
      return (
        <Tag color={isStatusField ? "blue" : undefined} style={{ marginInlineEnd: 0, fontWeight: 500 }}>
          {label}
        </Tag>
      )
    }
  }

  // Ngày tháng
  if (field.kind === 'date') {
    return <span className="dms-view-value">{formatDate(String(value))}</span>
  }
  if (field.kind === 'datetime') {
    return <span className="dms-view-value">{formatDateTime(String(value))}</span>
  }

  // Số lượng & tiền tệ: căn font monospace đẹp mắt
  if (field.kind === 'number' || typeof value === 'number') {
    const num = Number(value)
    const isMoney = /amount|price|cost|fee|total|deposit|balance/i.test(
      field.name,
    )
    return (
      <span className="dms-view-value dms-view-value-mono">
        {isMoney ? formatMoney(num) : formatNumber(num)}
      </span>
    )
  }

  // Đoạn văn dài textarea hoặc các trường ghi chú, lý do, mô tả
  if (field.kind === 'textarea' || /reason|note|description|remark|content/i.test(field.name)) {
    return (
      <div className="dms-view-long-text">
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
      <Typography.Text copyable={{ tooltips: ['Sao chép', 'Đã chép'] }} className="dms-view-value">
        {String(value)}
      </Typography.Text>
    )
  }

  return <span className="dms-view-value">{display(value)}</span>
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
  const form = Form.useFormInstance()
  const selected = Form.useWatch(field.name, { form, preserve: true })
  const parent = Form.useWatch(field.lookupDependsOn?.field ?? field.name, { form, preserve: true })
  const remote = !!field.lookup && (field.lookup.startsWith('/catalogs/') || field.lookup === '/floors' || field.lookup === '/storage-locations' || !!field.lookupValue)
  const missingParent = !!field.lookupDependsOn?.required && (parent == null || parent === '')
  const selectionRequired = !!field.required || !!(record?.[field.name] != null && field.lookupDependsOn && parent != null
    && String(parent) !== String(record[field.lookupDependsOn.field] ?? ''))
  const allowClear = !field.required && (!record || record[field.name] == null || field.multiple === true)
  const lookupFilters = {
    ...field.lookupFilters,
    ...(field.lookupDependsOn && parent != null && parent !== ''
      ? { [field.lookupDependsOn.parameter ?? field.lookupDependsOn.field]: parent as string | number | boolean }
      : {}),
  }
  const { data, isLoading, error } = useQuery({
    queryKey: ['crud-lookup', field.lookup, lookupFilters],
    queryFn: () => getLookupRecords(field.lookup!, lookupFilters),
    // Chi tiết đã có ID/metadata: không tải danh mục khác mà vai trò có thể không được xem.
    enabled: !!field.lookup && !remote && !missingParent && !disabled,
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
          required: !disabled && selectionRequired,
          message: `Chọn ${field.label.toLowerCase()}`,
        },
      ]}
    >
      {remote ? (
        <ReferenceSelect
          aria-label={field.label}
          getPopupContainer={(trigger) => trigger.parentElement ?? document.body}
          popupClassName="dms-crud-select-popup"
          endpoint={field.lookup!}
          filters={lookupFilters}
          valueField={field.lookupValue}
          labelField={field.lookupLabel}
          multiple={field.multiple}
          allowClear={allowClear}
          disabled={disabled || missingParent}
          placeholder={missingParent ? 'Chọn danh mục cấp trên trước' : undefined}
          initialOptions={(Array.isArray(selected) ? selected : selected == null ? [] : [selected]).map((value: string | number) => ({
            value,
            label: String(referenceLabel(field, record ?? {}, value) ?? value),
          }))}
        />
      ) : (
        <Select
          aria-label={field.label}
          getPopupContainer={(trigger) => trigger.parentElement ?? document.body}
          popupClassName="dms-crud-select-popup"
          showSearch
          optionFilterProp="label"
          mode={field.multiple ? 'multiple' : undefined}
          allowClear={allowClear}
          loading={isLoading}
          disabled={disabled || missingParent}
          options={options}
          placeholder={error ? 'Không tải được danh sách, hãy thử lại' : 'Chọn…'}
        />
      )}
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
  const [modalMode, setModalMode] = useState<'create' | 'edit' | 'view' | null>(
    null,
  )
  const [activeRecord, setActiveRecord] = useState<CrudRecord | null>(null)
  const [maintenanceCreateOpen, setMaintenanceCreateOpen] = useState(false)
  const [excelOpen, setExcelOpen] = useState(false)
  const [excelBlocked, setExcelBlocked] = useState(false)
  const [studentImportBlocked, setStudentImportBlocked] = useState(false)
  const excelBlocker = useBlocker(({ currentLocation, nextLocation }) =>
    (excelBlocked || studentImportBlocked) && currentLocation.pathname !== nextLocation.pathname)
  useEffect(() => {
    if (excelBlocker.state !== 'blocked') return
    void message.warning(excelUiLabels.leaveBlocked)
    excelBlocker.reset()
  }, [excelBlocker, message])
  const [exporting, setExporting] = useState(false)
  const exportingRef = useRef(false)
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
  function clearDependentFields(changed: Record<string, unknown>, definitions: CrudField[], target: typeof form) {
    for (const field of definitions) {
      if (field.lookupDependsOn && Object.hasOwn(changed, field.lookupDependsOn.field))
        target.setFieldValue(field.name, undefined)
    }
  }
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
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])

  function changeQuery(patch: Partial<typeof query>) {
    setSelectedRowKeys([])
    setSearchParams((previous) =>
      updateCrudQuery(previous, resource, { ...query, ...patch }),
    )
  }

  function handleBulkDelete() {
    if (!canDelete || selectedRowKeys.length === 0) return
    let reason = ''
    modal.confirm({
      title: `Xóa ${selectedRowKeys.length} bản ghi đã chọn?`,
      content:
        config.deleteAction === 'lock' ? (
          <TextArea
            placeholder="Lý do khóa (ít nhất 5 ký tự)"
            onChange={(event) => {
              reason = event.target.value
            }}
          />
        ) : (
          `Bạn có chắc chắn muốn xóa ${selectedRowKeys.length} bản ghi đã chọn? Thao tác sẽ được kiểm tra quyền và lịch sử dữ liệu tại máy chủ.`
        ),
      okText: config.deleteLabel ?? `Xóa (${selectedRowKeys.length})`,
      cancelText: 'Hủy',
      okButtonProps: { danger: true },
      onOk: async () => {
        if (config.deleteAction === 'lock' && reason.trim().length < 5) {
          void message.error('Nhập lý do ít nhất 5 ký tự')
          throw new Error('Thiếu lý do')
        }
        try {
          for (const key of selectedRowKeys) {
            await writeCrudRecord(
              `${deleteEndpoint}/${key}${config.deleteAction ? `/${config.deleteAction}` : ''}`,
              config.deleteAction ? 'post' : 'delete',
              config.deleteAction === 'lock' ? { reason } : undefined,
            )
          }
          void message.success(`Đã xóa ${selectedRowKeys.length} bản ghi`)
          setSelectedRowKeys([])
          await refresh()
        } catch (e) {
          handleApiError(e)
          setSelectedRowKeys([])
          await refresh()
        }
      },
    })
  }
  const filterSignature = JSON.stringify(activeFilters)
  useEffect(() => {
    if (!config.filters?.length || config.singleton) return
    filterForm.resetFields()
    const initial: Record<string, string | number> = {
      ...JSON.parse(filterSignature),
    }
    for (const field of config.filters ?? []) {
      if (initial[field.name] && isNumericField(field))
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
  const statusFilter = config.filters?.find((field) =>
    crudDetailUiLabels.statusFilterNames.includes(field.name) && field.options?.length,
  )
  const statusOptions = [
    { value: '', label: crudDetailUiLabels.all },
    ...(statusFilter?.options ?? []).map((option) => ({
      value: String(option.value),
      label: statusFilter?.name === 'status' && crudStatusEntities[resource]
        ? getStatus(crudStatusEntities[resource], String(option.value)).label
        : crudValueLabels[option.label] ?? option.label,
    })),
  ]
  const statusCounts = useQueries({
    queries: statusOptions.map((option) => {
      const filters = { ...activeFilters }
      if (statusFilter) {
        delete filters[statusFilter.name]
        if (option.value !== '') filters[statusFilter.name] = option.value
      }
      return {
        queryKey: ['crud', me?.id, resource, 'status-count', search, filters],
        enabled: canRead && !config.singleton && !!statusFilter,
        staleTime: 30_000,
        queryFn: () => searchByPage(config.endpoint, createSearchRequest({
          ...query, filters, page: 1, size: 1,
        }, config)),
      }
    }),
  })
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
          ['hometownId', 'emergencyContact'].includes(field.name)
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
  const visible = useCallback(
    (field: CrudField) =>
      !field.visibleWhen ||
      isFieldVisible(
        field,
        isViewMode
          ? activeRecord?.[field.visibleWhen.field]
          : (field.visibleWhen.field === 'scope' ? scopeValue : typeValue) ??
              activeRecord?.[field.visibleWhen.field] ??
              config.createDefaults?.[field.visibleWhen.field],
      ),
    [isViewMode, activeRecord, scopeValue, typeValue, config.createDefaults],
  )

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
    if (modalMode === 'view' && hasRecordDetailContent(resource)) void refresh()
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
      (!config.deleteStates ||
        config.deleteStates.includes(String(row.status))) &&
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
  const studentAccountImport = resource === 'adminStudents' && hasPermission(me, Permission.STUDENT_CREATE)
  const canImportExcel = !config.singleton && !config.readOnly && (canCreate || canUpdate)
  const excelFields = [...new Map([
    ...config.fields, ...(config.createFields ?? []), ...(config.editFields ?? []),
  ].filter((field) => !field.sensitive && !crudSecretFieldNames.has(field.name))
    .map((field) => [field.name, field])).values()]
  const excelConfig = { ...config, fields: excelFields }

  async function exportExcel() {
    if (exportingRef.current) return
    exportingRef.current = true
    setExporting(true)
    try {
      const records = config.singleton
        ? normalizePage((await api.get(config.endpoint)).data).content
        : await exportFilteredRecords(config, query)
      const exportColumns = [...new Map([
        ...(!config.singleton ? [{ key: idKey, label: excelUiLabels.id }] : []),
        ...config.columns,
      ].filter((column) => !crudSecretFieldNames.has(column.key))
        .map((column) => [column.key, column])).values()]
      await downloadWorkbook(config.title, exportColumns, records)
      void message.success(records.length ? excelUiLabels.exportSuccess : excelUiLabels.exportEmpty)
    } catch (e) {
      handleApiError(e)
    } finally {
      exportingRef.current = false
      setExporting(false)
    }
  }

  async function saveExcelRow(values: Record<string, unknown>) {
    const hasId = values[idKey] != null && values[idKey] !== ''
    if (hasId ? !canUpdate : !canCreate)
      throw new Error(hasId ? excelUiLabels.noUpdate : excelUiLabels.noCreate)
    if (!hasId && ['students', 'adminStudents'].includes(resource))
      throw new Error(excelUiLabels.studentsImport)
    let existing: CrudRecord | undefined
    if (hasId) {
      existing = config.useListRecordForEdit || config.inlineDetail
        ? (await getLookupRecords(config.endpoint)).find((record) => String(record[idKey]) === String(values[idKey]))
        : (await api.get(`${detailEndpoint}/${encodeURIComponent(String(values[idKey]))}`)).data as CrudRecord
      if (!existing) throw new Error(excelUiLabels.recordMissing)
      if (!canEditRow(existing)) throw new Error(excelUiLabels.stateLocked)
    }
    const allowedFields = (hasId
      ? (config.editFields ?? config.fields).filter((field) => !field.createOnly)
      : (config.createFields ?? config.fields).filter((field) => !field.editOnly))
      .filter((field) => !field.sensitive && !crudSecretFieldNames.has(field.name))
      .filter((field) => !(resource === 'users' && field.name === 'role' && (existing?.id === me?.id || existing?.role === Role.STUDENT)))
      .filter((field) => !(hasId && config.closeAtOnlyAfterOpen && hasPeriodOpened(existing?.openAt) && field.name !== 'closeAt'))
    const payload: Record<string, unknown> = hasId ? { [idKey]: values[idKey] } : { ...config.createDefaults }
    const current = existing ? getRecordFormValues(existing, config, resource) : {}
    for (const field of excelFields) {
      const value = values[field.name]
      if (value == null || value === '') continue
      if (!allowedFields.some((candidate) => candidate.name === field.name)) {
        const previous = field.kind === 'datetime' && current[field.name] != null
          ? new Date(String(current[field.name])).toISOString() : current[field.name]
        if (JSON.stringify(value) !== JSON.stringify(previous))
          throw new Error(`${excelUiLabels.immutable}: ${field.label}`)
        continue
      }
      if (field.visibleWhen && !isFieldVisible(field, values[field.visibleWhen.field] ?? current[field.visibleWhen.field] ?? config.createDefaults?.[field.visibleWhen.field]))
        continue
      payload[field.name] = value
    }
    if (existing && config.keepCreateOnlyOnEdit)
      for (const field of config.fields.filter((candidate) => candidate.createOnly))
        if (existing[field.name] != null) payload[field.name] = existing[field.name]
    if (hasId && Object.keys(payload).length === 1) throw new Error(excelUiLabels.noChanges)
    if (config.createFormat === 'multipart') {
      const multipart = new FormData()
      multipart.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
      return writeCrudRecord(`${writeEndpoint}/save-or-update`, 'post', multipart)
    }
    return saveOrUpdate(writeEndpoint, payload)
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
  /** Chỉ hiện xác nhận thu tiền khi GET chi tiết hóa đơn trả action này trong allowedActions. */
  function canConfirmInvoicePayment(row: CrudRecord) {
    return (
      resource === 'invoices' &&
      Array.isArray(row.allowedActions) &&
      row.allowedActions.includes(BillingAction.CONFIRM_PAYMENT)
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
  const formFields = useMemo(
    () =>
      fields
        .filter(visible)
        .filter(
          (field) =>
            !(
              modalMode === 'create' &&
              resource === 'maintenance' &&
              role === Role.STUDENT &&
              field.name === 'roomId'
            ),
        ),
    [fields, modalMode, resource, role, visible],
  )
  const formSections: CrudFormSection[] = useMemo(() => {
    if (config.formSections && config.formSections.length > 0) {
      return config.formSections
    }
    // Tự động phân chia 2 nhóm chuẩn V5: Thông tin chính và Thông tin bổ sung
    const mainFieldNames: string[] = []
    const additionalFieldNames: string[] = []
    for (const f of formFields) {
      const isLongOrMeta =
        f.kind === 'textarea' ||
        /note|reason|description|remark|content|address/i.test(f.name)
      if (isLongOrMeta) {
        additionalFieldNames.push(f.name)
      } else {
        mainFieldNames.push(f.name)
      }
    }
    const sections: CrudFormSection[] = [
      {
        title: 'Thông tin chính',
        fields: mainFieldNames.length > 0 ? mainFieldNames : formFields.map((f) => f.name),
      },
    ]
    if (additionalFieldNames.length > 0 && mainFieldNames.length > 0) {
      sections.push({
        title: crudDetailUiLabels.additionalInformation,
        fields: additionalFieldNames,
      })
    }
    return sections
  }, [config.formSections, formFields])

  const sectionedFieldNames = new Set(
    formSections.flatMap((section) => section.fields),
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
  const viewItems = [
    ...formFields.map((field) => ({
      key: field.name,
      label: field.label,
      fullWidth:
        field.kind === 'textarea' ||
        /note|reason|description|remark|content|address/i.test(field.name),
      content: renderViewFieldValue(field, displayRecord, resource),
    })),
    ...detailColumns.map((column) => ({
      key: column.key,
      label: column.label,
      fullWidth: false,
      content: renderColumnCell(resource, column, displayRecord),
    })),
  ]

  interface ViewSection extends CrudFormSection {
    items: typeof viewItems
  }

  const viewSections: ViewSection[] = formSections.map((section) => ({
    ...section,
    items: viewItems.filter((item) => section.fields.includes(item.key)),
  }))
  const additionalItems = viewItems.filter((item) => !sectionedFieldNames.has(item.key))
  if (additionalItems.length > 0) viewSections.push({
    title: crudDetailUiLabels.additionalInformation,
    fields: additionalItems.map((item) => item.key),
    items: additionalItems,
  })

  function renderField(field: CrudField) {
    const isFieldDisabled = !isWritableField(field)
    if (isFieldDisabled) return (
      <Form.Item key={field.name} label={field.label}>
        {renderViewFieldValue(field, displayRecord, resource)}
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

  const modalTitleText =
    modalMode === 'view'
      ? `Chi tiết ${config.title.toLowerCase()}`
      : modalMode === 'edit'
      ? `Chỉnh sửa ${config.title.toLowerCase()}`
      : `Thêm ${config.title.toLowerCase()}`

  const ModalHeaderIcon =
    modalMode === 'view'
      ? EyeOutlined
      : modalMode === 'edit'
      ? EditOutlined
      : PlusOutlined

  const modalSubtitle =
    activeRecord && (modalMode === 'view' || modalMode === 'edit')
      ? String(
          activeRecord.code ??
            activeRecord.contractCode ??
            activeRecord.studentCode ??
            activeRecord.roomCode ??
            (activeRecord[idKey] ? `#${activeRecord[idKey]}` : ''),
        )
      : undefined

  if (!canRead)
    return (
      <Result
        status="403"
        title="Không có quyền xem màn hình này"
        subTitle="Quyền thao tác được quản lý theo vai trò tại máy chủ."
      />
    )

  return (
    <div className={`dms-crud-page${config.finance ? ' dms-finance-page' : ''}`}>

      {studentAccountImport && excelOpen && (
        <Card title={studentImportLabels.panelTitle} style={{ marginBottom: 16 }}>
          <StudentImportPanel onBlockedChange={setStudentImportBlocked} />
        </Card>
      )}
      <Card className={`dms-crud-frame min-w-0 rounded-md border border-border bg-bg p-2 sm:p-3${config.finance ? ' dms-finance-card' : ''}`}>
        <ListToolbar
          key={resource}
          filterLabel={crudDetailUiLabels.filters}
          closeFiltersLabel={crudDetailUiLabels.closeFilters}
          helpLabel={crudDetailUiLabels.help}
          help={(config.note || (config.readOnly && !config.singleton)) && (
            <>
              {config.note && <p>{config.note}</p>}
              {config.readOnly && !config.singleton && <p>{excelUiLabels.readonlyHint}</p>}
            </>
          )}
          filterCount={Object.values(activeFilters).filter((value) => value !== '').length}
          search={!config.singleton && (
            <SearchInput
              aria-label={`Tìm kiếm ${config.title.toLowerCase()}`}
              placeholder={config.searchPlaceholder ?? `Tìm theo mã / tên ${config.title.toLowerCase()}…`}
              allowClear
              enterButton="Tìm kiếm"
              key={`${resource}:${search}`}
              defaultValue={search}
              onSearch={(value) => {
                changeQuery({ search: value.trim(), page: 1 })
              }}
            />
          )}
          selectedCount={selectedRowKeys.length}
          onBulkDelete={canDelete && selectedRowKeys.length > 0 ? handleBulkDelete : undefined}
          primaryAction={
            canCreate && (
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => {
                  if (resource === 'maintenance') setMaintenanceCreateOpen(true)
                  else void openEditor()
                }}
                className="dms-btn-primary"
              >
                {config.createLabel ?? `+ Thêm ${config.title.toLowerCase()}`}
              </Button>
            )
          }
          actions={
            <Space wrap size={8}>
              {studentAccountImport && (
                <Button icon={<UploadOutlined />} aria-expanded={excelOpen} onClick={() => setExcelOpen(true)}>
                  {studentImportLabels.tab}
                </Button>
              )}
              {canImportExcel && (
                <CrudExcelPanel key={resource} resource={resource} config={excelConfig}
                  triggerLabel={studentAccountImport ? studentImportLabels.update : excelUiLabels.import}
                  canCreate={canCreate && !['students', 'adminStudents'].includes(resource)} canUpdate={canUpdate}
                  onSave={saveExcelRow} onComplete={refresh} onBlockedChange={setExcelBlocked} />
              )}
              <span style={{ display: 'inline-flex', position: 'relative' }}>
              <Dropdown
                getPopupContainer={(trigger) => trigger.parentElement!}
                styles={{ root: { top: '100%', right: 0, bottom: 'auto', left: 'auto' } }}
                menu={{
                  items: [
                    {
                      key: 'export',
                      icon: <DownloadOutlined />,
                      label: excelUiLabels.export,
                      disabled: exporting,
                      onClick: () => void exportExcel(),
                    },
                    ...(studentAccountImport ? [{
                      key: 'student-import',
                      icon: <UploadOutlined />,
                      label: studentImportLabels.tab,
                      onClick: () => setExcelOpen(true),
                    }] : []),
                    ...(canDelete && selectedRowKeys.length > 0 ? [{
                      key: 'bulk-delete',
                      icon: <DeleteOutlined />,
                      danger: true,
                      label: `Xóa ${selectedRowKeys.length} bản ghi đã chọn`,
                      onClick: handleBulkDelete,
                    }] : []),
                  ],
                }}
                trigger={['click']}
              >
                <Tooltip title="Thao tác khác">
                  <Button icon={<MoreOutlined />} aria-label="Thao tác khác" className="dms-toolbar-more-btn" />
                </Tooltip>
              </Dropdown>
              </span>
            </Space>
          }
          filters={!config.singleton && !!config.filters?.length && (
            <Form
              form={filterForm}
              layout="vertical"
              onValuesChange={(changed) => clearDependentFields(changed, config.filters ?? [], filterForm)}
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
        />
        {!config.singleton && statusFilter && (
          <StatusFilter
            label={statusFilter.label}
            value={activeFilters[statusFilter.name] ?? ''}
            options={statusOptions.map((option, index) => ({
              ...option,
              count: statusCounts[index]?.data?.totalElements,
              loading: statusCounts[index]?.isPending,
            }))}
            onChange={(value) => {
              const filters = { ...activeFilters }
              if (value === '') delete filters[statusFilter.name]
              else filters[statusFilter.name] = value
              changeQuery({ filters, page: 1 })
            }}
          />
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
            rowSelection={
              config.singleton || !canDelete
                ? undefined
                : {
                    selectedRowKeys,
                    onChange: (keys) => setSelectedRowKeys(keys),
                  }
            }
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
                render: (_: unknown, row: CrudRecord) => {
                  const hasSpecialActions =
                    canResetPassword(row) ||
                    (resource === 'users' &&
                      hasPermission(me, Permission.USER_UNLOCK) &&
                      row.locked === true) ||
                    (resource === 'users' &&
                      row.role === Role.STAFF &&
                      hasPermission(me, Permission.USER_CROSS_UNIT_ACCESS))

                  const moreItems: MenuProps['items'] = [
                    ...(canResetPassword(row)
                      ? [
                          {
                            key: 'reset-pwd',
                            icon: <KeyOutlined />,
                            label: 'Đặt lại mật khẩu',
                            onClick: () => resetPassword(row),
                          },
                        ]
                      : []),
                    ...(resource === 'users' &&
                    hasPermission(me, Permission.USER_UNLOCK) &&
                    row.locked === true
                      ? [
                          {
                            key: 'unlock',
                            icon: <UnlockOutlined />,
                            label: 'Mở khóa tài khoản',
                            onClick: () => unlockAccount(row),
                          },
                        ]
                      : []),
                    ...(resource === 'users' &&
                    row.role === Role.STAFF &&
                    hasPermission(me, Permission.USER_CROSS_UNIT_ACCESS)
                      ? [
                          {
                            key: 'cross-unit',
                            icon: <GlobalOutlined />,
                            label: row.crossUnitAccess
                              ? 'Thu hồi quyền đọc tổng hợp'
                              : 'Cấp quyền đọc tổng hợp',
                            onClick: () => changeCrossUnitAccess(row),
                          },
                        ]
                      : []),
                  ]

                  return (
                    <Space size={4} wrap={false} className="dms-row-actions">
                      <Tooltip title="Xem chi tiết toàn bộ thông tin (chỉ đọc, không sửa)">
                        <Button
                          size="small"
                          icon={<EyeOutlined />}
                          onClick={() => {
                            void showDetail(row)
                          }}
                          aria-label="Xem chi tiết toàn bộ thông tin (chỉ đọc, không sửa)"
                          className="dms-action-btn dms-action-view"
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
                            className="dms-action-btn dms-action-edit"
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
                            className="dms-action-btn dms-action-delete"
                          />
                        </Tooltip>
                      )}
                      {hasSpecialActions && (
                        <Dropdown menu={{ items: moreItems }} trigger={['click']}>
                          <Tooltip title="Thao tác khác">
                            <Button
                              size="small"
                              icon={<MoreOutlined />}
                              aria-label="Thao tác khác"
                              className="dms-action-btn dms-action-more"
                            />
                          </Tooltip>
                        </Dropdown>
                      )}
                    </Space>
                  )
                },
              },
            ]}
          />
        )}
      </Card>
      <Modal
        headerIcon={<ModalHeaderIcon />}
        title={modalTitleText}
        subtitle={modalSubtitle}
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
        width={960}
        className={`dms-crud-modal${config.finance ? ' dms-finance-modal' : ''}`}
        footer={
          <div className="dms-modal-footer-v5">
            <div className="dms-modal-footer-right">
              <Button onClick={closeModal} disabled={busy} className="dms-btn-cancel">
                Đóng
              </Button>
              {isViewMode && activeRecord && canConfirmInvoicePayment(activeRecord) && (
                <Button
                  type="primary"
                  icon={<DollarCircleOutlined />}
                  onClick={() => setPaymentInvoice(activeRecord)}
                  disabled={busy}
                >
                  Xác nhận đã thu tiền
                </Button>
              )}
              {isViewMode && activeRecord && canEditRow(activeRecord) && (
                <Button
                  type="primary"
                  icon={<EditOutlined />}
                  onClick={() => setModalMode('edit')}
                  className="dms-btn-edit"
                >
                  Chuyển sang sửa
                </Button>
              )}
              {(modalMode === 'create' || modalMode === 'edit') && (
                <Button
                  type="primary"
                  loading={busy}
                  icon={<SaveOutlined />}
                  onClick={() => form.submit()}
                  className="dms-btn-save"
                >
                  Lưu
                </Button>
              )}
            </div>
          </div>
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
        {activeRecord && isViewMode && hasRecordDetailContent(resource) && (
          <RecordDetailContent
            key={resource + ":" + String(activeRecord[idKey])}
            resource={resource}
            recordId={Number(activeRecord[idKey])}
            role={role}
            readOnly={false}
          />
        )}
        {activeRecord && isViewMode && !hasRecordDetailContent(resource) && (
          <DetailLayout className="dms-record-view">
            {/* Section 1: Thông tin chính */}
            {viewSections.slice(0, 1).filter((section) => section.items.length > 0).map((section) => (
              <FormSection key={section.title} title={section.title} description={section.note} defaultOpen={true}>
                <div className="dms-view-grid">
                  {section.items.map((item) => (
                    <div key={item.key} className={`dms-view-field${item.fullWidth ? ' dms-view-field-full' : ''}`}>
                      <div className="dms-view-label">{item.label}</div>
                      <div className="dms-view-value">{item.content}</div>
                    </div>
                  ))}
                </div>
              </FormSection>
            ))}

            {/* Section 2: Điều kiện / Bảng con / Danh sách chi tiết */}
            {['items', 'transactions', 'timeline'].map((key) =>
              !(key === 'items' && invoiceResource) && Array.isArray(activeRecord[key]) && activeRecord[key].length > 0 ? (
                <FormSection
                  key={key}
                  title={crudDetailUiLabels.collections[key as keyof typeof crudDetailUiLabels.collections] ?? key}
                  defaultOpen={true}
                >
                  <div className="dms-subcollection-list">
                    {(activeRecord[key] as CrudRecord[]).map((item, index) => (
                      <div key={index} className="dms-subcollection-item">
                        {display(item)}
                      </div>
                    ))}
                  </div>
                </FormSection>
              ) : null,
            )}

            {/* Section 3: Phân loại / Thông tin bổ sung */}
            {viewSections.slice(1).filter((section) => section.items.length > 0).map((section) => (
              <FormSection key={section.title} title={section.title} description={section.note} defaultOpen={true}>
                <div className="dms-view-grid">
                  {section.items.map((item) => (
                    <div key={item.key} className={`dms-view-field${item.fullWidth ? ' dms-view-field-full' : ''}`}>
                      <div className="dms-view-label">{item.label}</div>
                      <div className="dms-view-value">{item.content}</div>
                    </div>
                  ))}
                </div>
              </FormSection>
            ))}
          </DetailLayout>
        )}
        {invoiceResource && activeRecord ? (
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
            <FormSection title={billingUiLabels.invoiceItems} defaultOpen={true}>
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
                      align: 'left',
                      className: 'dms-table-col-number',
                      render: (value) => formatNumber(Number(value ?? 0)),
                    },
                    {
                      title: billingUiLabels.unitPrice,
                      dataIndex: 'unitPrice',
                      align: 'left',
                      className: 'dms-table-col-number',
                      render: (value) => formatMoney(Number(value ?? 0)),
                    },
                    {
                      title: billingUiLabels.amount,
                      dataIndex: 'amount',
                      align: 'left',
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
            </FormSection>
          </div>
        ) : null}
          <Form
            form={form}
            component={modalMode === 'create' || modalMode === 'edit' ? 'form' : 'div'}
            style={modalMode !== 'create' && modalMode !== 'edit' ? { display: 'none' } : undefined}
            key={resource}
            disabled={busy}
            layout="vertical"
            initialValues={formInitialValues}
            onValuesChange={(changed) => clearDependentFields(changed, fields, form)}
            onFinish={(values) => {
              submitRecord(values)
            }}
            preserve={false}
          >
            <DetailLayout>
              {formSections.map((section) => {
                const sectionFields = formFields.filter((field) => section.fields.includes(field.name))
                if (sectionFields.length === 0) return null
                return (
                  <FormSection key={section.title} title={section.title} description={section.note} defaultOpen={true}>
                    <FormGrid>
                      {sectionFields.map(renderField)}
                    </FormGrid>
                  </FormSection>
                )
              })}
              {remainingFields.length > 0 && (
                <FormSection title={crudDetailUiLabels.additionalInformation} defaultOpen={true}>
                  <FormGrid>{remainingFields.map(renderField)}</FormGrid>
                </FormSection>
              )}
            </DetailLayout>
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
          readOnly={resource === 'myDeposits'}
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
