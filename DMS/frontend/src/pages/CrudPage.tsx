import {
  Input,
  InputNumber,
  SearchInput,
  Select,
  Switch,
  TextArea,
} from '../components/ui/FormControls'
import {
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
  EditOutlined,
  EyeOutlined,
  GlobalOutlined,
  KeyOutlined,
  UnlockOutlined,
} from '@ant-design/icons'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Alert,
  Flex,
  Form,
  Result,
  Space,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import { useCallback, useEffect, useState } from 'react'
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
  crudRequestParams,
  CRUD_PAGE_SIZES,
  normalizePage,
  readCrudQuery,
  readValue,
  toLocalDateTime,
  updateCrudQuery,
  validationFieldErrors,
  writeCrudRecord,
} from '../features/crud/api'
import { resources } from '../features/crud/resources'
import type { CrudField, CrudRecord } from '../features/crud/types'
import { api } from '../services/api/client'
import { hasPermission } from '../services/api/auth'
import { errorMessage } from '../utils/errorMessages'
import { formatDate, formatDateTime } from '../utils/format'
import { useApiErrorHandler } from '../hooks/useApiErrorHandler'

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
  [Role.STAFF]: 'Quản lý',
  [Role.ADMIN]: 'Quản trị',
  [Role.STUDENT]: 'Sinh viên',
  [Role.TECHNICIAN]: 'Kỹ thuật viên',
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
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value))
    return formatDateTime(value)
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value))
    return formatDate(value)
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
    // Danh mục tham chiếu ít đổi; refresh() vẫn invalidate sau mỗi lần ghi.
    staleTime: 5 * 60 * 1000,
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
function renderCrudInput(field: CrudField) {
  switch (field.kind) {
    case 'number':
      return (
        <InputNumber
          min={field.min ?? 0}
          max={field.max}
          style={{ width: '100%' }}
        />
      )
    case 'date':
      return <Input type="date" />
    case 'datetime':
      return <Input type="datetime-local" />
    case 'textarea':
      return <TextArea rows={4} maxLength={field.maxLength} showCount />
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
  const location = useLocation()
  const [searchParams, setSearchParams] = useSearchParams()
  const { id: routeId } = useParams()
  const query = readCrudQuery(searchParams, resource, config)
  const { search, page, size, filters: activeFilters } = query
  const handleApiError = useApiErrorHandler()
  const [modalMode, setModalMode] = useState<
    'create' | 'edit' | 'view' | null
  >(null)
  const [activeRecord, setActiveRecord] = useState<CrudRecord | null>(null)
  const isViewMode = modalMode === 'view'
  const editing =
    modalMode === 'edit'
      ? activeRecord
      : modalMode === 'create'
        ? null
        : modalMode === 'view'
          ? activeRecord
          : undefined
  const detail = activeRecord ?? undefined
  const [busy, setBusy] = useState(false)
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
  const idKey = config.idKey ?? 'id'
  const writeEndpoint =
    resource === 'students' && role === Role.ADMIN
      ? '/admin/students'
      : (config.writeEndpoint ?? config.endpoint)
  const detailEndpoint = config.detailEndpoint ?? config.endpoint
  function changeQuery(patch: Partial<typeof query>) {
    setSearchParams((previous) =>
      updateCrudQuery(previous, resource, { ...query, ...patch }),
    )
  }
  const filterSignature = JSON.stringify(activeFilters)
  useEffect(() => {
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
    queryKey: [
      'crud',
      me?.id,
      resource,
      page,
      size,
      config.localSearch ? '' : search,
      activeFilters,
    ],
    enabled: canRead,
    queryFn: async () =>
      normalizePage(
        (
          await api.get(config.endpoint, {
            params: crudRequestParams(query, config),
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
  const total = config.localSearch ? rows?.length : data?.totalElements
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
  const baseFields =
    modalMode === 'create'
      ? (config.createFields ?? config.fields).filter((field) => !field.editOnly)
      : (config.editFields ?? config.fields).filter((field) => !field.createOnly)
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
  const visible = (field: CrudField) =>
    !field.visibleWhen ||
    (field.visibleWhen.field === 'scope' ? scopeValue : typeValue) ===
      field.visibleWhen.value

  const populateForm = useCallback(
    (selected: CrudRecord) => {
      const initial = { ...selected }
      for (const field of config.fields) {
        let val = initial[field.name]
        if (val == null && field.name.endsWith('Id')) {
          const objKey = field.name.slice(0, -2)
          const objVal = initial[objKey]
          if (
            objVal &&
            typeof objVal === 'object' &&
            'id' in (objVal as Record<string, unknown>)
          ) {
            val = (objVal as Record<string, unknown>).id
            initial[field.name] = val
          }
        }
        if (val != null) {
          if (field.kind === 'datetime') {
            initial[field.name] = toLocalDateTime(val)
          } else if (field.kind === 'date') {
            if (typeof val === 'string' && val.includes('T')) {
              initial[field.name] = val.slice(0, 10)
            }
          } else if (
            field.kind === 'select' &&
            typeof val === 'object' &&
            !Array.isArray(val) &&
            'id' in (val as Record<string, unknown>)
          ) {
            initial[field.name] = (val as Record<string, unknown>).id
          }
        }
      }
      if (
        ['myApplications', 'applications'].includes(resource) &&
        Array.isArray(selected.priorities)
      ) {
        initial.priorityCategoryIds = selected.priorities.map(
          (p) => (p as CrudRecord).categoryId,
        )
      }
      form.setFieldsValue(initial)
    },
    [config.fields, resource, form],
  )

  function closeModal() {
    setModalMode(null)
    setActiveRecord(null)
    form.resetFields()
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
    api
      .get(`${detailEndpoint}/${routeId}`)
      .then((response) => {
        if (!cancelled) {
          const item = response.data as CrudRecord
          setActiveRecord(item)
          setModalMode('view')
          populateForm(item)
        }
      })
      .catch((e) => {
        if (!cancelled) handleApiError(e)
      })
    return () => {
      cancelled = true
    }
  }, [routeId, detailEndpoint, handleApiError, canRead, populateForm])

  /** Quyền thao tác không thay thế điều kiện trạng thái hoặc người sở hữu bản ghi. */
  function canChangeRow(row: CrudRecord) {
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
      resource !== 'configs' &&
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
    if (row ? !canEditRow(row) : !canCreate) return
    form.resetFields()
    if (!row) {
      setActiveRecord(null)
      setModalMode('create')
      form.setFieldsValue({
        active: true,
        role: Role.STAFF,
        type: 'ROOM_FEE',
        scope: 'UNIT',
        unitId: me?.managementUnit?.id,
      })
      return
    }
    try {
      const selected =
        config.singleton || config.localSearch || config.inlineDetail
          ? row
          : ((await api.get(`${detailEndpoint}/${row[idKey]}`))
              .data as CrudRecord)
      setActiveRecord(selected)
      setModalMode('edit')
      populateForm(selected)
    } catch (e) {
      handleApiError(e)
    }
  }
  async function saveRecord(values: Record<string, unknown>) {
    if (editing ? !canEditRow(editing) : !canCreate) return
    setBusy(true)
    try {
      const payload: Record<string, unknown> = {}
      for (const field of fields.filter(visible)) {
        const value = values[field.name]
        // Trường clearable: giá trị rỗng khi sửa gửi null để backend xóa dữ liệu.
        if (editing && field.clearable && (value == null || value === '')) {
          payload[field.name] = null
          continue
        }
        // Chuỗi rỗng xóa nội dung tùy chọn; không gửi null để đổi nghĩa PATCH của backend.
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
      if (editing && config.keepCreateOnlyOnEdit)
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
        config.writeMethod && editing
          ? config.writeMethod
          : editing
            ? 'patch'
            : 'post',
        body,
      )
      if (
        !showTemporaryPassword(result, payload.username ?? payload.studentCode)
      )
        void message.success('Đã lưu')
      closeModal()
      await refresh()
    } catch (e) {
      form.setFields(
        validationFieldErrors(e).map((item) => ({
          name: item.field,
          errors: [item.message],
        })),
      )
      handleApiError(e)
    } finally {
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
            `${writeEndpoint}/${row[idKey]}${config.deleteAction ? `/${config.deleteAction}` : ''}`,
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
    modal.confirm({
      title: 'Thay đổi vai trò hoặc đơn vị tài khoản?',
      content:
        'Phiên đăng nhập cũ của tài khoản này sẽ mất hiệu lực. Người dùng cần đăng nhập lại để nhận quyền và phạm vi mới.',
      okText: 'Lưu thay đổi',
      cancelText: 'Hủy',
      onOk: () => saveRecord(values),
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
    try {
      const selected =
        config.singleton || config.localSearch || config.inlineDetail
          ? row
          : resource === 'deposits'
            ? ((await api.get(`/contracts/${row.id}/deposit`))
                .data as CrudRecord)
            : ((await api.get(`${detailEndpoint}/${row[idKey]}`))
                .data as CrudRecord)
      setActiveRecord(selected)
      setModalMode('view')
      populateForm(selected)
    } catch (e) {
      handleApiError(e)
    }
  }
  /** KTV cập nhật tiến độ ticket được giao; không sửa nội dung báo hỏng của người tạo. */
  function updateMaintenanceProgress(action: string) {
    if (
      !detail ||
      !hasPermission(me, Permission.MAINTENANCE_UPDATE) ||
      !Array.isArray(detail.allowedActions) ||
      !detail.allowedActions.includes(action)
    )
      return
    const target = detail.id
    let note = ''
    modal.confirm({
      title: {
        start: 'Bắt đầu sửa chữa?',
        'add-note': 'Thêm ghi chú tiến độ',
        resolve: 'Hoàn thành sửa chữa',
      }[action],
      content:
        action === 'start' ? (
          'Cập nhật trạng thái ticket được giao.'
        ) : (
          <TextArea
            placeholder="Ghi chú (từ 5 đến 500 ký tự)"
            maxLength={500}
            onChange={(event) => {
              note = event.target.value
            }}
          />
        ),
      okText: 'Cập nhật',
      cancelText: 'Đóng',
      onOk: async () => {
        if (action !== 'start' && note.trim().length < 5) {
          void message.error('Nhập ghi chú ít nhất 5 ký tự')
          throw new Error('Thiếu ghi chú')
        }
        try {
          const updated = (await writeCrudRecord(
            `/maintenance-requests/${target}/${action === 'add-note' ? 'notes' : action}`,
            'post',
            action === 'start' ? undefined : { note: note.trim() },
          )) as CrudRecord
          setActiveRecord(updated)
          populateForm(updated)
          await refresh()
        } catch (failure) {
          handleApiError(failure)
          throw failure
        }
      },
    })
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
        {!config.singleton && (
          <div className="dms-toolbar">
            {!config.singleton && (
              <SearchInput
                aria-label={`Tìm kiếm ${config.title.toLowerCase()}`}
                placeholder="Nhập từ khóa tìm kiếm"
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
            pagination={
              config.singleton
                ? false
                : {
                    current: page,
                    pageSize: size,
                    total: config.localSearch
                      ? rows?.length
                      : data?.totalElements,
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
                minWidth: 150,
                render: (_: unknown, row: CrudRecord) => (
                  <span>{display(readValue(row, column.key))}</span>
                ),
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
                  </Space>
                ),
              },
            ]}
          />
        )}
      </Card>
      <Modal
        title={
          modalMode === 'view'
            ? `Chi tiết ${config.title.toLowerCase()}`
            : `${modalMode === 'edit' ? 'Sửa' : 'Thêm'} ${config.title.toLowerCase()}`
        }
        open={modalMode !== null}
        destroyOnHidden
        onCancel={closeModal}
        confirmLoading={busy}
        closable={!busy}
        mask={{ closable: !busy }}
        cancelButtonProps={{ disabled: busy }}
        okText="Lưu"
        cancelText="Đóng"
        width={780}
        footer={
          modalMode === 'view' ? (
            <Flex
              justify="space-between"
              align="center"
              style={{ width: '100%' }}
            >
              <Space>
                {activeRecord && canDeleteRow(activeRecord) && (
                  <Button
                    danger
                    icon={<DeleteOutlined />}
                    onClick={() => {
                      const rowToDelete = activeRecord
                      closeModal()
                      deleteRecord(rowToDelete)
                    }}
                  >
                    {config.deleteLabel ?? 'Xóa'}
                  </Button>
                )}
              </Space>
              <Space>
                <Button onClick={closeModal}>Đóng</Button>
                {activeRecord && canEditRow(activeRecord) && (
                  <Button
                    type="primary"
                    icon={<EditOutlined />}
                    onClick={() => {
                      setModalMode('edit')
                    }}
                  >
                    Chuyển sang sửa
                  </Button>
                )}
              </Space>
            </Flex>
          ) : (
            <Space>
              <Button onClick={closeModal} disabled={busy}>
                Đóng
              </Button>
              <Button
                type="primary"
                loading={busy}
                onClick={() => form.submit()}
              >
                Lưu
              </Button>
            </Space>
          )
        }
      >
        <Form
          form={form}
          layout="vertical"
          disabled={isViewMode}
          onFinish={(values) => {
            submitRecord(values)
          }}
          preserve={false}
        >
          <FormGrid
            className={
              fields.filter(visible).length < 3
                ? 'dms-form-grid-single'
                : undefined
            }
          >
            {fields
              .filter(visible)
              .filter(
                (field) =>
                  !(
                    resource === 'maintenance' &&
                    role === Role.STUDENT &&
                    field.name === 'roomId'
                  ),
              )
              .map((field) =>
                field.kind === 'select' ? (
                  <Form.Item
                    key={field.name}
                    label={field.label}
                    required={!isViewMode && field.required}
                  >
                    <LookupInput
                      field={field}
                      disabled={
                        isViewMode ||
                        (resource === 'users' &&
                          field.name === 'role' &&
                          editing?.id === me?.id)
                      }
                    />
                  </Form.Item>
                ) : (
                  <Form.Item
                    key={field.name}
                    name={field.name}
                    label={field.label}
                    className={
                      field.kind === 'textarea'
                        ? 'dms-form-span-full'
                        : undefined
                    }
                    valuePropName={
                      field.kind === 'boolean' ? 'checked' : 'value'
                    }
                    rules={
                      isViewMode
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
                    {renderCrudInput(field)}
                  </Form.Item>
                ),
              )}
          </FormGrid>
        </Form>
        {isViewMode && activeRecord && (
          <>
            {resource === 'maintenance' &&
              hasPermission(me, Permission.MAINTENANCE_UPDATE) &&
              Array.isArray(activeRecord.allowedActions) && (
                <Space wrap style={{ marginTop: 16 }}>
                  {['start', 'add-note', 'resolve']
                    .filter((action) =>
                      (activeRecord.allowedActions as string[]).includes(action),
                    )
                    .map((action) => (
                      <Button
                        key={action}
                        onClick={() => updateMaintenanceProgress(action)}
                      >
                        {
                          {
                            start: 'Bắt đầu',
                            'add-note': 'Ghi chú tiến độ',
                            resolve: 'Hoàn thành',
                          }[action]
                        }
                      </Button>
                    ))}
                </Space>
              )}
            {resource === 'rooms' && Array.isArray(activeRecord.beds) && (
              <BedCrudPanel
                roomId={Number(activeRecord.id)}
                beds={activeRecord.beds as CrudRecord[]}
                reload={async () => {
                  const updated = (
                    await api.get(`/rooms/${activeRecord.id}`)
                  ).data as CrudRecord
                  setActiveRecord(updated)
                  populateForm(updated)
                  await refresh()
                }}
              />
            )}
            {['items', 'transactions', 'timeline'].map(
              (key) =>
                Array.isArray(activeRecord[key]) && (
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
                    {(activeRecord[key] as CrudRecord[]).map((item, index) => (
                      <Typography.Paragraph key={index}>
                        {display(item)}
                      </Typography.Paragraph>
                    ))}
                  </Card>
                ),
            )}
          </>
        )}
      </Modal>
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
  const { modal } = App.useApp()
  const handleApiError = useApiErrorHandler()
  const { data: me } = useMe()
  const canRead = hasPermission(me, Permission.BED_READ)
  const canCreate = hasPermission(me, Permission.BED_CREATE)
  const canUpdate = hasPermission(me, Permission.BED_UPDATE)
  const canDelete = hasPermission(me, Permission.BED_DELETE)
  const [form] = Form.useForm()
  const [editing, setEditing] = useState<CrudRecord | null | undefined>()
  const [busy, setBusy] = useState(false)
  async function save(values: Record<string, unknown>) {
    if (editing ? !canUpdate : !canCreate) return
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
      form.setFields(
        validationFieldErrors(e).map((item) => ({
          name: item.field,
          errors: [item.message],
        })),
      )
      handleApiError(e)
    } finally {
      setBusy(false)
    }
  }
  if (!canRead) return null
  return (
    <Card
      title="Giường trong phòng"
      style={{ marginTop: 16 }}
      extra={
        canCreate && (
          <Button
            onClick={() => {
              form.resetFields()
              setEditing(null)
            }}
          >
            Thêm giường
          </Button>
        )
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
            key: 'actions',
            width: 100,
            render: (_, bed) => (
              <Space size={4} wrap={false}>
                {canUpdate && (
                  <Tooltip title="Sửa mã giường">
                    <Button
                      size="small"
                      icon={<EditOutlined />}
                      onClick={() => {
                        form.setFieldsValue({ code: bed.code })
                        setEditing(bed)
                      }}
                      aria-label="Sửa mã giường"
                    />
                  </Tooltip>
                )}
                {canDelete && (
                  <Tooltip title="Xóa giường">
                    <Button
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                      onClick={() =>
                        modal.confirm({
                          title: 'Xóa giường chưa có lịch sử?',
                          onOk: async () => {
                            try {
                              await writeCrudRecord(`/beds/${bed.id}`, 'delete')
                              await reload()
                            } catch (e) {
                              handleApiError(e)
                              throw e
                            }
                          },
                        })
                      }
                      aria-label="Xóa giường"
                    />
                  </Tooltip>
                )}
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
