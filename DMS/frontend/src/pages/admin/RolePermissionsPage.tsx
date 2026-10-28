import { Checkbox, SearchInput, Select } from '../../components/ui/FormControls'
import { Alert, Button, FilterBar, Tag } from '../../components/ui/Surface'
import Table from '../../components/ui/DataTable'
import { ReloadOutlined, SaveOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  App,
  Flex,
  Space,
  Tooltip,
  Typography,
  type TableColumnsType,
} from 'antd'
import { useEffect, useState } from 'react'
import { useBlocker, useSearchParams } from 'react-router'
import PageHeader from '../../components/layout/PageHeader'
import { Action, ErrorCode, Permission, Role } from '../../constants/authorization'
import {
  roleLabel,
  rolePermissionActionLabels as actionLabels,
  rolePermissionLabels,
} from '../../constants/navigation'
import { meQueryKey, useMe } from '../../features/account/useMe'
import { validationFieldErrors } from '../../features/crud/api'
import { hasPermission } from '../../services/api/auth'
import { api } from '../../services/api/client'
import { errorCodeOf, errorMessage } from '../../utils/errorMessages'

interface MatrixPrivilege {
  code: string
  action: string
  label: string
  granted: boolean
  defaultGranted: boolean
  assignable: boolean
  required: boolean
}

interface MatrixScreen {
  code: string
  label: string
  group: string
  privileges: MatrixPrivilege[]
}

interface RolePermissionMatrix {
  role: Role
  roleLabel: string
  version: number
  readOnly: boolean
  screens: MatrixScreen[]
}

interface PermissionDraft {
  role: Role
  version: number
  permissions: string[]
}

const matrixQueryKey = (role: Role) => ['role-permission-matrix', role] as const
const crudActions = [Action.READ, Action.CREATE, Action.UPDATE, Action.DELETE]

function knownPrivilege(privilege: MatrixPrivilege): boolean {
  return Object.hasOwn(Permission, privilege.code)
}

function grantedCodes(matrix: RolePermissionMatrix): string[] {
  return matrix.screens.flatMap((screen) =>
    screen.privileges
      .filter((privilege) => privilege.granted && knownPrivilege(privilege))
      .map((privilege) => privilege.code),
  )
}

/** Thao tác ghi luôn đi kèm quyền đọc; bỏ quyền đọc sẽ bỏ các thao tác của cùng màn. */
function changePrivilegeSelection(
  screen: MatrixScreen,
  selection: Set<string>,
  privilege: MatrixPrivilege,
  checked: boolean,
): Set<string> {
  const next = new Set(selection)
  if (!privilege.assignable || privilege.required || !knownPrivilege(privilege))
    return next
  if (checked) {
    next.add(privilege.code)
    const read = screen.privileges.find((entry) => entry.action === Action.READ)
    if (read && knownPrivilege(read) && (read.assignable || read.required))
      next.add(read.code)
  } else if (privilege.action === Action.READ) {
    screen.privileges.forEach((entry) => {
      if (!entry.required) next.delete(entry.code)
    })
  } else next.delete(privilege.code)
  return next
}

/** Bảng quyền theo màn hình/thao tác. Danh mục và giới hạn vai trò đều do backend cung cấp. */
export default function RolePermissionsPage() {
  const { message, modal } = App.useApp()
  const queryClient = useQueryClient()
  const { data: me } = useMe()
  const [params, setParams] = useSearchParams()
  const requestedRole = params.get('role')
  const role =
    requestedRole && Object.hasOwn(Role, requestedRole)
      ? (requestedRole as Role)
      : Role.ADMIN
  const [draft, setDraft] = useState<PermissionDraft | null>(null)
  const [search, setSearch] = useState('')
  const [group, setGroup] = useState<string | undefined>()
  const [saveFeedback, setSaveFeedback] = useState<string | null>(null)
  const matrixQuery = useQuery({
    queryKey: matrixQueryKey(role),
    queryFn: async () =>
      (await api.get<RolePermissionMatrix>(`/admin/role-permissions/${role}`))
        .data,
    retry: false,
    // Không thay bảng đang tick bằng một lần tải nền; version khi lưu vẫn phát hiện xung đột.
    refetchOnWindowFocus: false,
  })
  const matrix = matrixQuery.data
  const currentDraft = draft?.role === role ? draft : null
  const selection = new Set(
    currentDraft?.permissions ?? (matrix ? grantedCodes(matrix) : []),
  )
  const baseline = new Set(matrix ? grantedCodes(matrix) : [])
  const dirty =
    !!matrix &&
    (selection.size !== baseline.size ||
      [...selection].some((code) => !baseline.has(code)))
  const canUpdate =
    !matrix?.readOnly && hasPermission(me, Permission.ROLE_PERMISSION_UPDATE)

  const saveMutation = useMutation({
    mutationFn: async (input: PermissionDraft) =>
      (
        await api.put<RolePermissionMatrix>(
          `/admin/role-permissions/${input.role}`,
          {
            version: input.version,
            permissions: [...input.permissions].sort(),
          },
        )
      ).data,
    retry: false,
    onSuccess: (saved) => {
      queryClient.setQueryData(matrixQueryKey(saved.role), saved)
      setDraft(null)
      setSaveFeedback(null)
      void queryClient.invalidateQueries({ queryKey: meQueryKey })
      message.success(`Đã lưu quyền của vai trò ${saved.roleLabel}.`)
    },
    onError: (failure) => {
      const feedback = validationFieldErrors(failure)
        .map((field) => field.message)
        .join(' ')
      setSaveFeedback(feedback || errorMessage(failure))
      if (errorCodeOf(failure) === ErrorCode.PERMISSION_MATRIX_STALE) {
        // Bản cũ không được gửi lại: tải bản mới rồi để người dùng chủ động tick và lưu.
        setDraft(null)
        void matrixQuery.refetch()
      }
      message.error(errorMessage(failure))
    },
  })
  const busy = saveMutation.isPending
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      dirty && !busy && currentLocation.pathname !== nextLocation.pathname,
  )

  useEffect(() => {
    if (blocker.state !== 'blocked') return
    const confirmation = modal.confirm({
      title: 'Bỏ các quyền chưa lưu?',
      content:
        'Các ô vừa tick chưa được lưu. Rời màn hình sẽ bỏ các thay đổi này.',
      okText: 'Bỏ thay đổi và rời màn hình',
      cancelText: 'Tiếp tục chỉnh sửa',
      onOk: () => {
        setDraft(null)
        blocker.proceed()
      },
      onCancel: () => blocker.reset(),
    })
    return () => confirmation.destroy()
  }, [blocker, modal])

  useEffect(() => {
    if (!dirty) return
    const warnBeforeClosing = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      event.returnValue = ''
    }
    window.addEventListener('beforeunload', warnBeforeClosing)
    return () => window.removeEventListener('beforeunload', warnBeforeClosing)
  }, [dirty])

  const updateSelection = (next: Set<string>) => {
    if (!matrix || !canUpdate || busy) return
    setDraft({
      role,
      version: currentDraft?.version ?? matrix.version,
      permissions: [...next],
    })
    setSaveFeedback(null)
  }
  const togglePrivilege = (
    screen: MatrixScreen,
    privilege: MatrixPrivilege,
    checked: boolean,
  ) =>
    updateSelection(
      changePrivilegeSelection(screen, selection, privilege, checked),
    )
  const selectRole = (nextRole: Role) => {
    if (nextRole === role || busy) return
    const switchRole = () => {
      setDraft(null)
      setSaveFeedback(null)
      const nextParams = new URLSearchParams(params)
      nextParams.set('role', nextRole)
      setParams(nextParams, { replace: true })
    }
    if (dirty)
      modal.confirm({
        title: 'Bỏ các quyền chưa lưu?',
        content:
          'Các ô vừa tick chưa được lưu. Chuyển vai trò sẽ bỏ các thay đổi này.',
        okText: 'Bỏ thay đổi và chuyển',
        cancelText: 'Tiếp tục chỉnh sửa',
        onOk: switchRole,
      })
    else switchRole()
  }
  const toggleAction = (action: string, checked: boolean) => {
    let next = new Set(selection)
    matrix?.screens.forEach((screen) => {
      const privilege = screen.privileges.find(
        (entry) => entry.action === action,
      )
      if (privilege)
        next = changePrivilegeSelection(screen, next, privilege, checked)
    })
    updateSelection(next)
  }
  const selectAll = (checked: boolean) => {
    let next = new Set(selection)
    matrix?.screens.forEach((screen) => {
      screen.privileges.forEach((privilege) => {
        next = changePrivilegeSelection(screen, next, privilege, checked)
      })
    })
    updateSelection(next)
  }
  const restoreDefaults = () => {
    if (!matrix) return
    updateSelection(
      new Set(
        matrix.screens.flatMap((screen) =>
          screen.privileges
            .filter(
              (privilege) =>
                (privilege.defaultGranted || privilege.required) &&
                knownPrivilege(privilege),
            )
            .map((privilege) => privilege.code),
        ),
      ),
    )
  }
  const renderPrivilege = (
    screen: MatrixScreen,
    privilege: MatrixPrivilege,
  ) => {
    const disabled =
      !canUpdate ||
      busy ||
      !privilege.assignable ||
      privilege.required ||
      !knownPrivilege(privilege)
    const reason = matrix?.readOnly
      ? 'Quản trị viên luôn có toàn quyền.'
      : privilege.required
        ? 'Quyền bắt buộc để truy cập hồ sơ và kết thúc phiên đăng nhập.'
        : !privilege.assignable
          ? 'Thao tác này không thuộc phạm vi nghiệp vụ của vai trò.'
          : undefined
    return (
      <Tooltip key={privilege.code} title={reason}>
        <Checkbox
          aria-label={`${screen.label}: ${privilege.label}`}
          checked={selection.has(privilege.code)}
          disabled={disabled}
          onChange={(event) =>
            togglePrivilege(screen, privilege, event.target.checked)
          }
        />
      </Tooltip>
    )
  }
  const columns: TableColumnsType<MatrixScreen> = [
    {
      title: 'Màn hình / Nghiệp vụ',
      key: 'screen',
      width: 260,
      fixed: 'left',
      render: (_, screen) => (
        <Space orientation="vertical" size={4}>
          <Typography.Text strong>{screen.label}</Typography.Text>
          <Tag>{screen.group}</Tag>
        </Space>
      ),
    },
    ...crudActions.map((action) => {
      const editable =
        matrix?.screens
          .flatMap((screen) => screen.privileges)
          .filter(
            (entry) =>
              entry.action === action &&
              (matrix.readOnly || (entry.assignable && !entry.required)) &&
              knownPrivilege(entry),
          ) ?? []
      const selected = editable.filter((entry) =>
        selection.has(entry.code),
      ).length
      return {
        key: action,
        width: action === Action.READ ? 150 : 100,
        align: 'center' as const,
        title: (
          <Space orientation="vertical" size={4}>
            <span>{actionLabels[action]}</span>
            <Checkbox
              aria-label={`Chọn tất cả quyền ${actionLabels[action]}`}
              checked={editable.length > 0 && selected === editable.length}
              indeterminate={selected > 0 && selected < editable.length}
              disabled={!canUpdate || busy || editable.length === 0}
              onChange={(event) => toggleAction(action, event.target.checked)}
            />
          </Space>
        ),
        render: (_: unknown, screen: MatrixScreen) => {
          const privilege = screen.privileges.find(
            (entry) => entry.action === action,
          )
          return privilege ? (
            renderPrivilege(screen, privilege)
          ) : (
            <Typography.Text type="secondary">—</Typography.Text>
          )
        },
      }
    }),
    {
      title: 'Thao tác khác',
      key: 'other',
      width: 260,
      render: (_, screen) => (
        <Space orientation="vertical" size={8}>
          {screen.privileges
            .filter(
              (privilege) =>
                !crudActions.includes(
                  privilege.action as (typeof crudActions)[number],
                ),
            )
            .map((privilege) => (
              <Flex key={privilege.code} gap={8} align="center">
                {renderPrivilege(screen, privilege)}
                <span>{privilege.label}</span>
              </Flex>
            ))}
        </Space>
      ),
    },
  ]
  const normalizedSearch = search.trim().toLocaleLowerCase('vi')
  const screens =
    matrix?.screens.filter(
      (screen) =>
        (!group || screen.group === group) &&
        `${screen.label} ${screen.code} ${screen.group}`
          .toLocaleLowerCase('vi')
          .includes(normalizedSearch),
    ) ?? []
  const groups = [
    ...new Set(matrix?.screens.map((screen) => screen.group) ?? []),
  ]

  return (
    <>
      <PageHeader
        title="Phân quyền theo vai trò"
        subtitle="Chọn vai trò và tick quyền xem, tìm kiếm, thêm, sửa, xóa theo từng màn hình."
      />
      <Space orientation="vertical" size={16} style={{ width: '100%' }}>
        <Alert
          type="info"
          showIcon
          title="ADMIN luôn có toàn quyền. Ô không phù hợp với vai trò sẽ được khóa."
          description="Quyền đã lưu áp dụng ngay cho API, kể cả phiên đang đăng nhập; menu và nút được làm mới trong tối đa 30 giây. Phạm vi đơn vị, chủ sở hữu và trạng thái nghiệp vụ vẫn được kiểm tra."
        />
        <FilterBar className="dms-matrix-toolbar">
          <Typography.Text strong>
            {rolePermissionLabels.roleSelector}:
          </Typography.Text>
          <Select
            aria-label={rolePermissionLabels.roleSelector}
            value={role}
            disabled={busy}
            onChange={selectRole}
            options={Object.values(Role).map((value) => ({
              value,
              label: roleLabel[value],
            }))}
          />
          <SearchInput
            aria-label="Tìm màn hình phân quyền"
            placeholder="Tìm màn hình / nghiệp vụ"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            allowClear
            className="dms-matrix-search"
          />
          <Select
            aria-label="Lọc nhóm nghiệp vụ"
            placeholder="Tất cả nhóm nghiệp vụ"
            value={group}
            onChange={setGroup}
            allowClear
            options={groups.map((value) => ({ value, label: value }))}
          />
        </FilterBar>
        {matrixQuery.error ? (
          <Alert
            type="error"
            showIcon
            title={errorMessage(matrixQuery.error)}
            action={
              <Button
                onClick={() => matrixQuery.refetch()}
                icon={<ReloadOutlined />}
              >
                Thử lại
              </Button>
            }
          />
        ) : (
          <>
            {saveFeedback && (
              <Alert type="warning" showIcon title={saveFeedback} />
            )}
            <Flex justify="space-between" gap={12} wrap align="center">
              <Space wrap>
                <Typography.Text>
                  {rolePermissionLabels.matrixTitle} — {roleLabel[role]}:{' '}
                  {selection.size} {rolePermissionLabels.grantedCount}
                </Typography.Text>
                {matrix?.readOnly && (
                  <Tag color="blue">{rolePermissionLabels.adminFullAccess}</Tag>
                )}
                {dirty && <Tag color="orange">Có thay đổi chưa lưu</Tag>}
              </Space>
              <Space wrap>
                <Button
                  disabled={!matrix || !canUpdate || busy}
                  onClick={() => selectAll(true)}
                >
                  Chọn toàn bộ quyền phù hợp
                </Button>
                <Button
                  disabled={!matrix || !canUpdate || busy}
                  onClick={() => selectAll(false)}
                >
                  Bỏ chọn
                </Button>
                <Button
                  disabled={!matrix || !canUpdate || busy}
                  onClick={restoreDefaults}
                >
                  Khôi phục mặc định
                </Button>
              </Space>
            </Flex>
            <Table<MatrixScreen>
              rowKey="code"
              columns={columns}
              dataSource={screens}
              loading={matrixQuery.isPending}
              pagination={false}
              scroll={{ x: 1100 }}
              locale={{ emptyText: 'Không có màn hình phù hợp.' }}
            />
            <Flex justify="space-between" gap={12} wrap align="center">
              <Typography.Text type="secondary">
                Khôi phục mặc định chỉ thay đổi các ô tick. Bấm Lưu quyền để áp
                dụng.
              </Typography.Text>
              <Space>
                <Button
                  disabled={!dirty || busy}
                  onClick={() => {
                    setDraft(null)
                    setSaveFeedback(null)
                  }}
                >
                  Hủy thay đổi
                </Button>
                <Button
                  type="primary"
                  icon={<SaveOutlined />}
                  loading={busy}
                  disabled={!dirty || !canUpdate || !matrix}
                  onClick={() => {
                    if (matrix)
                      saveMutation.mutate({
                        role,
                        version: currentDraft?.version ?? matrix.version,
                        permissions: [...selection],
                      })
                  }}
                >
                  Lưu quyền
                </Button>
              </Space>
            </Flex>
          </>
        )}
      </Space>
    </>
  )
}
