import {
  CheckCircleOutlined,
  EditOutlined,
  InboxOutlined,
  LogoutOutlined,
  SwapOutlined,
  WalletOutlined,
} from '@ant-design/icons'
import { App, Flex } from 'antd'
import type { TableColumnsType } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import {
  ActionBar,
  Alert,
  Button,
  Card,
  ContractStatus as ContractStatusBadges,
  DatePicker,
  Descriptions,
  EmptyState,
  FormField,
  Modal,
  PageHeader,
  PageSkeleton,
  ResponsiveTable,
  StatusTimeline,
  TextArea,
  stepsFromFlow,
  type ActionDef,
} from '../../components'
import { Permission, Role } from '../../constants/authorization'
import {
  portalPath,
  STUDENT_REQUESTS_PATH,
} from '../../constants/navigation'
import {
  ContractStatus as ContractState,
  assignmentLabel,
  derivedLabels,
  roomGender,
  type DerivedLabel,
} from '../../constants/statusConfig'
import { hasPermission } from '../../services/api/auth'
import {
  ContractAction,
  contractActorLabel,
  contractAssignments,
  contractBedLabel,
  contractCodeOf,
  contractDepositInfo,
  contractPendingRefs,
  contractStudentInfo,
  type ContractAssignment,
  type ContractDetail,
  type ContractPendingRef,
} from '../../services/api/contract'
import { errorMessage } from '../../utils/errorMessages'
import {
  formatDate,
  formatDateTime,
  formatMoney,
} from '../../utils/format'
import { useMe } from '../account/useMe'
import ActivateContractModal from './ActivateContractModal'
import {
  useContractDetail,
  useCreateExtensionRequest,
} from './useContract'

const CONTRACT_FLOW: string[] = [
  ContractState.PENDING,
  ContractState.ACTIVE,
  ContractState.EXPIRED,
]

const PENDING_REF_LABELS: Record<ContractPendingRef['type'], string> = {
  extension: 'Yêu cầu gia hạn',
  transfer: 'Yêu cầu chuyển phòng',
  summer: 'Lựa chọn kỳ hè',
}

/** Nhãn lý do kết thúc phân phòng; mã lưu trong CSDL, giao diện không hiện enum tiếng Anh. */
const assignmentEndReasonLabels: Record<string, string> = {
  TRANSFER: 'Chuyển phòng',
  MOVE_OUT: 'Trả phòng',
  SUMMER_LEAVE: 'Tạm rời hè',
  RETURN_STORAGE: 'Nhận lại đồ',
  RELEASE_HOLD: 'Hủy giữ chỗ',
  CONTRACT_CANCELLED: 'Hủy hợp đồng',
}

function kindLabel(kind: string | null | undefined) {
  return kind === 'HOLD' || kind === 'STAY' ? assignmentLabel(kind) : '—'
}

function endReasonLabel(reason: string | null | undefined) {
  if (!reason) return '—'
  return assignmentEndReasonLabels[reason] ?? reason
}

function pendingRefPath(
  role: Role | undefined,
  ref: ContractPendingRef,
): string {
  const manager = role !== Role.STUDENT
  if (ref.type === 'extension')
    return manager
      ? `${portalPath(role, '/staff/extensions')}/${ref.id}`
      : STUDENT_REQUESTS_PATH
  if (ref.type === 'transfer')
    return manager
      ? `${portalPath(role, '/staff/transfers')}/${ref.id}`
      : `${STUDENT_REQUESTS_PATH}/transfers/${ref.id}`
  return manager
    ? `${portalPath(role, '/staff/summer')}/${ref.id}`
    : '/student/summer'
}

export interface ContractDetailViewProps {
  /** Cổng đang xem; không truyền thì lấy từ /auth/me. */
  role?: Role
  /** Route có thể truyền thẳng ID hợp đồng. */
  contractId?: number
}

interface CreateExtensionModalProps {
  open: boolean
  contract: ContractDetail
  onClose: () => void
}

function CreateExtensionRequestModal({
  open,
  contract,
  onClose,
}: CreateExtensionModalProps) {
  const { message } = App.useApp()
  const create = useCreateExtensionRequest(contract.id)
  const currentEnd = contract.endDate ? dayjs(contract.endDate) : null
  const [newEndDate, setNewEndDate] = useState<Dayjs | null>(null)
  const [reason, setReason] = useState('')
  const [errors, setErrors] = useState<{
    newEndDate?: string
    reason?: string
  }>({})

  function resetForm() {
    setNewEndDate(null)
    setReason('')
    setErrors({})
  }

  function validate() {
    const next: { newEndDate?: string; reason?: string } = {}
    if (!newEndDate || !newEndDate.isValid())
      next.newEndDate = 'Chọn ngày kết thúc mới.'
    else if (currentEnd && !newEndDate.isAfter(currentEnd, 'day'))
      next.newEndDate = `Ngày kết thúc mới phải sau ${formatDate(currentEnd)}.`
    const trimmed = reason.trim()
    if (trimmed.length < 5) next.reason = 'Lý do cần ít nhất 5 ký tự.'
    else if (trimmed.length > 500) next.reason = 'Lý do tối đa 500 ký tự.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function submit() {
    if (!newEndDate || !validate()) return
    try {
      await create.mutateAsync({
        newEndDate: newEndDate.format('YYYY-MM-DD'),
        reason: reason.trim(),
      })
      message.success('Đã gửi yêu cầu gia hạn hợp đồng.')
      onClose()
    } catch {
      // useApiErrorHandler đã hiện câu tiếng Việt; giữ hộp thoại để sửa ngày/lý do.
    }
  }

  return (
    <Modal
      title={`Gửi yêu cầu gia hạn ${contractCodeOf(contract)}`}
      open={open}
      onCancel={() => {
        if (!create.isPending) onClose()
      }}
      footer={[
        <Button key="cancel" onClick={onClose} disabled={create.isPending}>
          Hủy
        </Button>,
        <Button
          key="submit"
          type="primary"
          loading={create.isPending}
          onClick={() => void submit()}
        >
          Gửi yêu cầu
        </Button>,
      ]}
      width={640}
      mask={{ closable: false }}
      keyboard={!create.isPending}
      closable={!create.isPending}
      afterOpenChange={(visible) => {
        if (visible) resetForm()
      }}
    >
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        title="Mỗi hợp đồng chỉ có một yêu cầu gia hạn chờ xử lý"
        description={`Ngày kết thúc hiện tại: ${formatDate(contract.endDate)}. Staff sẽ kiểm tra chồng khoảng lưu trú trước khi duyệt.`}
      />
      <FormField
        label="Ngày kết thúc mới"
        required
        error={errors.newEndDate}
      >
        {(control) => (
          <DatePicker
            {...control}
            style={{ width: '100%' }}
            value={newEndDate}
            disabledDate={(value) =>
              currentEnd ? !value.isAfter(currentEnd, 'day') : false
            }
            onChange={(value) => {
              setNewEndDate((value as Dayjs | null) ?? null)
              setErrors((current) => ({
                ...current,
                newEndDate: undefined,
              }))
            }}
          />
        )}
      </FormField>
      <div style={{ marginTop: 16 }}>
        <FormField
          label="Lý do gia hạn"
          required
          error={errors.reason}
          hint="5–500 ký tự; nêu rõ nhu cầu ở tiếp hoặc lý do chưa thể trả phòng."
        >
          {(control) => (
            <TextArea
              {...control}
              rows={4}
              maxLength={500}
              showCount
              value={reason}
              placeholder="Ví dụ: còn một học kỳ thực tập tại trường, đề nghị gia hạn đến hết học kỳ..."
              onChange={(event) => {
                setReason(event.target.value)
                setErrors((current) => ({ ...current, reason: undefined }))
              }}
            />
          )}
        </FormField>
      </div>
    </Modal>
  )
}

/**
 * Chi tiết hợp đồng dùng chung cho Staff/Admin/Sinh viên (API 4.5, UI 22A): thời hạn, nhãn suy ra,
 * cọc, phân phòng hiện tại, yêu cầu chờ và lịch sử. Hành động lấy từ `allowedActions`, kích hoạt
 * mở hộp thoại thu cọc; sinh viên gửi yêu cầu gia hạn và theo dõi trạng thái.
 */
export default function ContractDetailView({
  role,
  contractId,
}: ContractDetailViewProps = {}) {
  const params = useParams()
  const routeId = Number(params.id ?? params.contractId)
  const id =
    contractId != null && Number.isSafeInteger(contractId) && contractId > 0
      ? contractId
      : Number.isSafeInteger(routeId) && routeId > 0
        ? routeId
        : 0
  const { data: me } = useMe()
  const navigate = useNavigate()
  const contractQuery = useContractDetail(id)
  const detail = contractQuery.data
  const [activateOpen, setActivateOpen] = useState(false)
  const [extensionOpen, setExtensionOpen] = useState(false)

  const actorRole = role ?? me?.role
  const manager = actorRole === Role.STAFF || actorRole === Role.ADMIN
  const listPath =
    actorRole === Role.STUDENT
      ? '/student/contracts'
      : portalPath(actorRole, '/staff/contracts')

  if (contractQuery.isLoading) return <PageSkeleton />

  if (!detail) {
    return (
      <>
        <PageHeader
          title="Không tìm thấy hợp đồng"
          breadcrumb={[{ label: 'Hợp đồng', to: listPath }]}
        />
        <EmptyState
          title="Không tìm thấy hợp đồng"
          description={
            id > 0
              ? errorMessage(contractQuery.error)
              : 'Đường dẫn hợp đồng không hợp lệ.'
          }
          action={
            <Button type="primary" onClick={() => navigate(listPath)}>
              Quay lại danh sách
            </Button>
          }
        />
      </>
    )
  }

  const allowedActions = detail.allowedActions ?? []
  const student = contractStudentInfo(detail)
  const deposit = contractDepositInfo(detail)
  const assignments = contractAssignments(detail)
  const pendingRefs = contractPendingRefs(detail)
  const timeline = detail.timeline ?? []
  const labels = (detail.labels ?? []).filter(
    (label): label is DerivedLabel => Object.hasOwn(derivedLabels, label),
  )
  const managerBase = portalPath(actorRole, '/staff/contracts')
  const storagePath = portalPath(actorRole, '/staff/storage')
  const depositPath = portalPath(actorRole, '/staff/deposits')

  const timelineDetails: Record<
    string,
    { at: string; reason?: string | null }
  > = {}
  for (const entry of timeline)
    timelineDetails[entry.toStatus] = {
      at: entry.at,
      reason: entry.reason,
    }
  const steps = stepsFromFlow(
    'contract',
    CONTRACT_FLOW,
    detail.status,
    timelineDetails,
    timeline.at(-1)?.fromStatus ?? undefined,
  )

  const actions: Record<string, ActionDef> = {}
  if (manager && hasPermission(me, Permission.CONTRACT_UPDATE)) {
    actions[ContractAction.ACTIVATE] = {
      label: 'Kích hoạt',
      icon: <CheckCircleOutlined />,
      onAction: () => setActivateOpen(true),
    }
  }
  if (manager && hasPermission(me, Permission.MOVE_OUT_CREATE)) {
    actions[ContractAction.MOVE_OUT] = {
      label: 'Trả phòng',
      danger: true,
      icon: <LogoutOutlined />,
      onAction: () => navigate(`${managerBase}/${detail.id}/move-out`),
    }
  }
  if (manager && hasPermission(me, Permission.STORAGE_CREATE)) {
    actions[ContractAction.CREATE_STORAGE_RECORD] = {
      label: 'Gửi đồ',
      icon: <InboxOutlined />,
      onAction: () => navigate(`${storagePath}?contractId=${detail.id}`),
    }
  }
  if (manager && hasPermission(me, Permission.DEPOSIT_CREATE)) {
    actions[ContractAction.TOP_UP] = {
      label: 'Nộp bù cọc',
      icon: <WalletOutlined />,
      onAction: () => navigate(depositPath),
    }
  }
  if (hasPermission(me, Permission.CONTRACT_EXTENSION_CREATE)) {
    actions[ContractAction.CREATE_EXTENSION_REQUEST] = {
      label: 'Yêu cầu gia hạn',
      icon: <EditOutlined />,
      onAction: () => setExtensionOpen(true),
    }
  }
  if (!manager && hasPermission(me, Permission.ROOM_TRANSFER_CREATE)) {
    actions[ContractAction.CREATE_TRANSFER_REQUEST] = {
      label: 'Yêu cầu chuyển phòng',
      icon: <SwapOutlined />,
      onAction: () => navigate(STUDENT_REQUESTS_PATH),
    }
  }

  const assignmentColumns: TableColumnsType<ContractAssignment> = [
    {
      title: 'Giường',
      key: 'bed',
      render: (_value, row) => contractBedLabel(row),
    },
    {
      title: 'Loại',
      dataIndex: 'kind',
      render: (value: string | null) => kindLabel(value),
    },
    {
      title: 'Bắt đầu',
      key: 'startedAt',
      render: (_value, row) =>
        formatDateTime(row.startedAt ?? row.stayStartedAt),
    },
    {
      title: 'Kết thúc',
      dataIndex: 'endedAt',
      render: (value: string | null) =>
        value ? formatDateTime(value) : 'Đang hiệu lực',
    },
    {
      title: 'Lý do kết thúc',
      dataIndex: 'endReason',
      render: (value: string | null) => endReasonLabel(value),
    },
    {
      title: 'Giá học kỳ',
      dataIndex: 'priceMain',
      align: 'right',
      render: (value: number | null) =>
        value != null ? formatMoney(value) : '—',
    },
    {
      title: 'Giá hè',
      dataIndex: 'priceSummer',
      align: 'right',
      render: (value: number | null) =>
        value != null ? formatMoney(value) : '—',
    },
  ]

  const currentAssignment = detail.currentAssignment ?? null
  const depositMissing =
    deposit.required == null &&
    deposit.balance == null &&
    deposit.topUpRequired == null
  const canTopUp =
    manager &&
    hasPermission(me, Permission.DEPOSIT_CREATE) &&
    allowedActions.includes(ContractAction.TOP_UP)

  return (
    <>
      <PageHeader
        title={`Hợp đồng ${contractCodeOf(detail)}`}
        status={
          <ContractStatusBadges status={detail.status} labels={labels} />
        }
        subtitle={[
          student.fullName
            ? `${student.fullName}${student.studentCode ? ` · ${student.studentCode}` : ''}`
            : student.studentId != null
              ? `Sinh viên #${student.studentId}`
              : null,
          `${formatDate(detail.startDate)} – ${formatDate(detail.endDate)}`,
        ]
          .filter(Boolean)
          .join(' · ')}
        breadcrumb={[
          { label: 'Hợp đồng', to: listPath },
          { label: contractCodeOf(detail) },
        ]}
        extra={
          <Flex gap={8} wrap justify="flex-end">
            <Button onClick={() => navigate(listPath)}>Danh sách</Button>
            <ActionBar allowedActions={allowedActions} actions={actions} />
          </Flex>
        }
      />

      {detail.status === ContractState.PENDING && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          title="Hợp đồng đang chờ kích hoạt"
          description={
            detail.activationDeadline
              ? `Cần thu cọc và kích hoạt trước ${formatDateTime(detail.activationDeadline)}; quá hạn hệ thống có thể tự hủy giữ chỗ.`
              : 'Cần thu cọc và xác nhận giấy tờ để chuyển hợp đồng sang Đang hiệu lực.'
          }
        />
      )}

      {detail.openStorageRecordId != null && (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          title="Sinh viên đang gửi đồ"
          description={
            <Link to={`${storagePath}?contractId=${detail.id}`}>
              Mở kho gửi đồ để xem phiếu #{detail.openStorageRecordId}
            </Link>
          }
        />
      )}

      <div className="dms-split-view">
        <div>
          <Card title="Thông tin hợp đồng">
            <Descriptions
              column={{ xs: 1, sm: 2 }}
              size="small"
              items={[
                {
                  key: 'student',
                  label: 'Sinh viên',
                  children: student.fullName
                    ? `${student.fullName}${student.studentCode ? ` (${student.studentCode})` : ''}`
                    : student.studentId != null
                      ? `Sinh viên #${student.studentId}`
                      : '—',
                },
                {
                  key: 'gender',
                  label: 'Giới tính',
                  children: student.gender
                    ? (roomGender[student.gender] ?? student.gender)
                    : '—',
                },
                {
                  key: 'startDate',
                  label: 'Ngày bắt đầu',
                  children: formatDate(detail.startDate),
                },
                {
                  key: 'endDate',
                  label: 'Ngày kết thúc',
                  children: formatDate(detail.endDate),
                },
                {
                  key: 'activationDeadline',
                  label: 'Hạn kích hoạt',
                  children: formatDateTime(detail.activationDeadline),
                },
                {
                  key: 'activatedAt',
                  label: 'Kích hoạt lúc',
                  children: formatDateTime(detail.activatedAt),
                },
                {
                  key: 'activatedBy',
                  label: 'Người kích hoạt',
                  children: contractActorLabel(detail.activatedBy),
                },
                {
                  key: 'daysRemaining',
                  label: 'Số ngày còn lại',
                  children:
                    detail.daysRemaining != null
                      ? `${detail.daysRemaining} ngày`
                      : '—',
                },
                ...(detail.statusReason
                  ? [
                      {
                        key: 'statusReason',
                        label: 'Lý do trạng thái',
                        children: detail.statusReason,
                      },
                    ]
                  : []),
                ...(detail.moveOut
                  ? [
                      {
                        key: 'moveOut',
                        label: 'Đã trả phòng',
                        children: formatDate(detail.moveOut.moveOutDate),
                      },
                    ]
                  : []),
              ]}
            />
          </Card>

          <Card
            title="Tiền cọc"
            style={{ marginTop: 16 }}
            extra={
              canTopUp && deposit.topUpRequired != null && deposit.topUpRequired > 0 ? (
                <Button size="small" onClick={() => navigate(depositPath)}>
                  Nộp bù cọc
                </Button>
              ) : undefined
            }
          >
            {depositMissing ? (
              <EmptyState
                title="Chưa có dữ liệu tiền cọc"
                description="Hợp đồng chưa kích hoạt hoặc backend chưa trả thông tin sổ cọc."
              />
            ) : (
              <>
                <Descriptions
                  column={{ xs: 1, sm: 2 }}
                  size="small"
                  items={[
                    {
                      key: 'required',
                      label: 'Mức cọc bắt buộc',
                      children:
                        deposit.required != null
                          ? formatMoney(deposit.required)
                          : '—',
                    },
                    {
                      key: 'balance',
                      label: 'Số dư hiện tại',
                      children:
                        deposit.balance != null
                          ? formatMoney(deposit.balance)
                          : '—',
                    },
                    {
                      key: 'topUp',
                      label: 'Cần bù',
                      children:
                        deposit.topUpRequired != null
                          ? formatMoney(deposit.topUpRequired)
                          : '—',
                    },
                  ]}
                />
                {deposit.topUpOverdue && (
                  <Alert
                    type="error"
                    showIcon
                    style={{ marginTop: 12 }}
                    title="Đã quá hạn bù cọc"
                    description="Sinh viên cần nộp bù cọc; xác nhận tại màn Tiền cọc sau khi nhận tiền."
                  />
                )}
                {!deposit.topUpOverdue &&
                  deposit.topUpRequired != null &&
                  deposit.topUpRequired > 0 && (
                    <Alert
                      type="warning"
                      showIcon
                      style={{ marginTop: 12 }}
                      title="Còn thiếu tiền cọc"
                      description={`Cần bù ${formatMoney(deposit.topUpRequired)} để đủ mức cọc của tòa.`}
                    />
                  )}
              </>
            )}
          </Card>

          <Card title="Phân phòng hiện tại" style={{ marginTop: 16 }}>
            {currentAssignment ? (
              <Descriptions
                column={{ xs: 1, sm: 2 }}
                size="small"
                items={[
                  {
                    key: 'bed',
                    label: 'Giường',
                    children: contractBedLabel(currentAssignment),
                  },
                  {
                    key: 'kind',
                    label: 'Trạng thái chỗ',
                    children: kindLabel(currentAssignment.kind),
                  },
                  {
                    key: 'startedAt',
                    label: 'Bắt đầu',
                    children: formatDateTime(
                      currentAssignment.startedAt ??
                        currentAssignment.stayStartedAt,
                    ),
                  },
                  {
                    key: 'priceMain',
                    label: 'Giá học kỳ',
                    children:
                      currentAssignment.priceMain != null
                        ? formatMoney(currentAssignment.priceMain)
                        : '—',
                  },
                  {
                    key: 'priceSummer',
                    label: 'Giá hè',
                    children:
                      currentAssignment.priceSummer != null
                        ? formatMoney(currentAssignment.priceSummer)
                        : '—',
                  },
                ]}
              />
            ) : (
              <EmptyState
                title="Chưa có phân phòng mở"
                description="Hợp đồng PENDING thường có giường giữ chỗ; nếu chưa thấy, tải lại sau khi Staff phân giường."
              />
            )}
          </Card>

          <Card title="Yêu cầu đang chờ" style={{ marginTop: 16 }}>
            {pendingRefs.length === 0 ? (
              <EmptyState
                title="Không có yêu cầu chờ"
                description="Yêu cầu gia hạn/chuyển phòng/lựa chọn hè đang chờ sẽ xuất hiện tại đây."
              />
            ) : (
              <Flex vertical gap={10}>
                {pendingRefs.map((ref) => (
                  <Flex key={`${ref.type}-${ref.id}`} gap={8} wrap>
                    <span>{PENDING_REF_LABELS[ref.type]}</span>
                    <Link to={pendingRefPath(actorRole, ref)}>
                      #{ref.id} — xem chi tiết
                    </Link>
                  </Flex>
                ))}
              </Flex>
            )}
          </Card>
        </div>

        <div>
          <Card title="Tiến trình hợp đồng">
            <StatusTimeline steps={steps} />
          </Card>

          <Card title="Lịch sử phân phòng" style={{ marginTop: 16 }}>
            <ResponsiveTable<ContractAssignment>
              columns={assignmentColumns}
              data={assignments}
              rowKey={(row) =>
                String(
                  row.id ??
                    `${row.bedId ?? row.bed?.id ?? ''}-${row.startedAt ?? ''}`,
                )
              }
              emptyTitle="Chưa có lịch sử phân phòng"
              emptyDescription="Lịch sử giữ chỗ/đang ở sẽ xuất hiện sau khi Staff phân giường hoặc chuyển phòng."
            />
          </Card>
        </div>
      </div>

      <ActivateContractModal
        open={activateOpen}
        contract={detail}
        onClose={() => setActivateOpen(false)}
      />
      <CreateExtensionRequestModal
        open={extensionOpen}
        contract={detail}
        onClose={() => setExtensionOpen(false)}
      />
    </>
  )
}
