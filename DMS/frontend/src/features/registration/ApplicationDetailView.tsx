import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  CloseOutlined,
  HomeOutlined,
  RollbackOutlined,
  SendOutlined,
  StopOutlined,
} from '@ant-design/icons'
import { App } from 'antd'
import type { TableColumnsType } from 'antd'
import { useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import {
  ActionBar,
  Alert,
  BedPicker,
  Button,
  Card,
  DataTable,
  Descriptions,
  EmptyState,
  PageHeader,
  PageSkeleton,
  StatusBadge,
  StatusTimeline,
  Tag,
  stepsFromFlow,
  type ActionDef,
} from '../../components'
import { Permission, Role } from '../../constants/authorization'
import { portalPath } from '../../constants/navigation'
import { crudValueLabels } from '../../constants/statusConfig'
import { hasPermission } from '../../services/api/auth'
import {
  ApplicationAction,
  ApplicationStatus,
  RegistrationErrorCode,
  applicationHoldBed,
  type ApplicationBedRef,
  type ApplicationPriority,
  type ApplicationTimelineEntry,
} from '../../services/api/registration'
import { errorCodeOf, errorMessage } from '../../utils/errorMessages'
import {
  daysLeft,
  formatBedCode,
  formatDateTime,
  formatMoney,
} from '../../utils/format'
import { useMe } from '../account/useMe'
import { useAvailableBeds } from '../room'
import VerifyPriorityModal from './VerifyPriorityModal'
import {
  lookupName,
  useApplicationCatalog,
  useApplicationDetail,
  useApproveApplication,
  useAssignApplicationBed,
  useAssignCatalog,
  useCancelApplication,
  useRejectApplication,
  useReleaseApplicationHold,
  useSubmitApplication,
} from './useRegistration'

/** Luồng chuẩn của đơn; REJECTED/CANCELLED là nhánh kết thúc (SRS mục 10). */
const applicationFlow = [
  ApplicationStatus.DRAFT,
  ApplicationStatus.SUBMITTED,
  ApplicationStatus.APPROVED,
  ApplicationStatus.ASSIGNED,
]

function listDestination(role: Role | undefined) {
  if (role === Role.STUDENT)
    return { label: 'Đơn đăng ký của tôi', path: '/student/applications' }
  return { label: 'Đơn đăng ký', path: portalPath(role, '/staff/applications') }
}

/** Hợp đồng chi tiết đã có màn riêng cho cả hai cổng; route do main agent đăng ký. */
function contractPath(role: Role | undefined, id: number) {
  return role === Role.STUDENT
    ? `/student/contracts/${id}`
    : portalPath(role, `/staff/contracts/${id}`)
}

/** Mã giường đầy đủ: ưu tiên mã backend ghép, nếu chỉ có mã ngắn thì ghép tòa-phòng-giường. */
function bedLabel(bed: ApplicationBedRef | null | undefined): string | null {
  if (!bed) return null
  if (bed.code?.includes('-')) return bed.code
  if (bed.buildingCode && bed.roomCode)
    return formatBedCode(bed.buildingCode, bed.roomCode, bed.code)
  if (bed.roomCode) return `${bed.roomCode}-${bed.code}`
  return bed.code || null
}

function bedRoomLabel(bed: ApplicationBedRef | null | undefined): string | null {
  if (!bed) return null
  if (bed.buildingCode && bed.roomCode) return `${bed.buildingCode}-${bed.roomCode}`
  return bed.roomCode ?? null
}

function priorityVerifiedLabel(verified: boolean | null) {
  if (verified === true) return <Tag color="green">Đã xác nhận</Tag>
  if (verified === false) return <Tag color="red">Không đúng</Tag>
  return <Tag>Chưa xác nhận</Tag>
}

/** Người xác nhận diện ưu tiên: backend trả ID ở `verifiedBy` và tên ở `verifiedByName`. */
function priorityVerifierLabel(priority: ApplicationPriority): string {
  const name = priority.verifiedByName?.trim()
  if (name) return name
  return priority.verifiedBy != null ? `#${priority.verifiedBy}` : '—'
}

export interface ApplicationDetailViewProps {
  /** Cổng đang xem; mặc định lấy từ /auth/me. */
  role?: Role
}

/**
 * Chi tiết đơn đăng ký dùng chung cho Sinh viên và Quản lý (UI 14.2, 21A):
 * thông tin đơn/diện ưu tiên/giữ chỗ, timeline và hành động duyệt, từ chối, phân giường,
 * hủy giữ chỗ, xác nhận diện ưu tiên, nộp/hủy đơn. Chỉ hiện hành động backend trả trong
 * `allowedActions` và người dùng có quyền; không tự dựng lại bảng chuyển trạng thái.
 */
export default function ApplicationDetailView({
  role,
}: ApplicationDetailViewProps = {}) {
  const id = Number(useParams().id)
  const application = useApplicationDetail(id)
  const { data: me } = useMe()
  const actorRole = role ?? me?.role
  const navigate = useNavigate()
  const { message } = App.useApp()

  const detail = application.data
  const allowedActions = detail?.allowedActions ?? []
  const canUpdate = hasPermission(me, Permission.ROOM_APPLICATION_UPDATE)
  const canDelete = hasPermission(me, Permission.ROOM_APPLICATION_DELETE)
  const canAssign =
    canUpdate && allowedActions.includes(ApplicationAction.ASSIGN)
  const canVerify =
    canUpdate && allowedActions.includes(ApplicationAction.VERIFY_PRIORITY)

  const [filter, setFilter] = useState<{
    buildingId?: number
    roomTypeId?: number
  }>({})
  const [picked, setPicked] = useState<number | undefined>()
  const [verifyPriority, setVerifyPriority] =
    useState<ApplicationPriority | null>(null)

  // Chỉ gọi API giường/danh mục khi đơn đang chờ phân giường; cổng Sinh viên không phát sinh request.
  const beds = useAvailableBeds({ applicationId: id, ...filter }, canAssign)
  const assignCatalog = useAssignCatalog(canAssign)
  const needsCatalog =
    detail != null &&
    ((detail.periodId != null && !detail.period?.name) ||
      (detail.preferredUnitId != null &&
        !detail.preferredUnit?.name &&
        !detail.preferredUnit?.code) ||
      (detail.preferredRoomTypeId != null && !detail.preferredRoomType?.name))
  const catalog = useApplicationCatalog(needsCatalog)

  const submit = useSubmitApplication(id)
  const approve = useApproveApplication(id)
  const reject = useRejectApplication(id)
  const cancel = useCancelApplication(id)
  const assignBed = useAssignApplicationBed(id)
  const releaseHold = useReleaseApplicationHold(id)

  // Giường đã chọn không còn trong danh sách mới (vừa bị lấy, đổi bộ lọc) thì coi như chưa chọn.
  const selected = useMemo(() => {
    for (const { room, freeBeds } of beds.data ?? []) {
      const bed = freeBeds.find((item) => item.id === picked)
      if (bed)
        return {
          id: bed.id,
          label: formatBedCode(room.buildingCode, room.code, bed.code),
          price: room.priceMain,
        }
    }
    return undefined
  }, [beds.data, picked])

  const list = listDestination(actorRole)

  if (application.isLoading) return <PageSkeleton />

  if (!detail) {
    const invalidId = !Number.isSafeInteger(id) || id <= 0
    return (
      <>
        <PageHeader
          title="Không tìm thấy đơn đăng ký"
          breadcrumb={[{ label: list.label, to: list.path }]}
        />
        <EmptyState
          title="Không tìm thấy đơn đăng ký"
          description={
            invalidId
              ? 'Đường dẫn đơn không hợp lệ.'
              : errorMessage(application.error)
          }
          action={
            <Button type="primary" onClick={() => navigate(list.path)}>
              Quay lại danh sách
            </Button>
          }
        />
      </>
    )
  }

  const student = detail.student
  const priorities = detail.priorities ?? []
  const timeline = detail.timeline ?? []
  const periodName = lookupName(
    catalog.periods.data,
    detail.periodId,
    detail.period?.name,
  )
  const unitName = lookupName(
    catalog.units.data,
    detail.preferredUnitId,
    detail.preferredUnit?.name ?? detail.preferredUnit?.code,
  )
  const roomTypeName = lookupName(
    catalog.roomTypes.data,
    detail.preferredRoomTypeId,
    detail.preferredRoomType?.name,
  )

  const hold = detail.hold ?? null
  const holdBed = applicationHoldBed(hold)
  const holdBedLabel = bedLabel(holdBed)
  const holdRoom = bedRoomLabel(holdBed)
  const holdContractId = hold?.contractId ?? detail.contractId ?? null
  const holdDeadline = hold?.activationDeadline ?? detail.activationDeadline ?? null
  const remainingDays = daysLeft(holdDeadline)
  const showHold =
    hold != null ||
    holdContractId != null ||
    detail.status === ApplicationStatus.APPROVED ||
    detail.status === ApplicationStatus.ASSIGNED

  const lastTimeline = timeline.at(-1)
  const timelineDetails: Record<
    string,
    { at: string; reason?: string | null }
  > = {}
  for (const entry of timeline)
    timelineDetails[entry.toStatus] = { at: entry.at, reason: entry.reason }
  const steps = stepsFromFlow(
    'application',
    applicationFlow,
    detail.status,
    timelineDetails,
    lastTimeline?.fromStatus ?? undefined,
  )

  const personLabel = (entry: ApplicationTimelineEntry) => {
    if (entry.actor?.fullName) return entry.actor.fullName
    if (entry.actorId == null) return 'Hệ thống'
    if (entry.actorId === me?.id) return 'Bạn'
    return `Người dùng #${entry.actorId}`
  }

  const deadlineTag =
    remainingDays == null ? null : remainingDays < 0 ? (
      <Tag color="red">Quá hạn {-remainingDays} ngày</Tag>
    ) : remainingDays === 0 ? (
      <Tag color="orange">Hết hạn hôm nay</Tag>
    ) : (
      <Tag color="orange">Còn {remainingDays} ngày</Tag>
    )

  const actions: Record<string, ActionDef> = {}
  if (canUpdate) {
    actions[ApplicationAction.SUBMIT] = {
      label: 'Nộp đơn',
      icon: <SendOutlined />,
      loading: submit.isPending,
      confirm: {
        title: 'Nộp đơn đăng ký',
        description:
          'Sau khi nộp, đơn không sửa được nữa; chỉ có thể hủy. Ban quản lý sẽ duyệt và phân giường theo thứ tự ưu tiên.',
        okText: 'Nộp đơn',
      },
      onAction: async () => {
        await submit.mutateAsync()
        void message.success('Đã nộp đơn đăng ký.')
      },
    }
    actions[ApplicationAction.APPROVE] = {
      label: 'Duyệt đơn',
      icon: <CheckCircleOutlined />,
      loading: approve.isPending,
      confirm: {
        title: 'Duyệt đơn đăng ký',
        description:
          'Đơn chuyển sang Đã duyệt và chờ phân giường. Sinh viên nhận thông báo kết quả.',
        okText: 'Duyệt đơn',
      },
      onAction: async () => {
        await approve.mutateAsync()
        void message.success('Đã duyệt đơn đăng ký.')
      },
    }
    actions[ApplicationAction.REJECT] = {
      label: 'Từ chối',
      danger: true,
      icon: <CloseOutlined />,
      loading: reject.isPending,
      confirm: {
        title: 'Từ chối đơn đăng ký',
        description:
          'Đơn chuyển sang Bị từ chối và giữ nguyên lịch sử. Sinh viên sẽ thấy lý do.',
        okText: 'Từ chối',
        reasonRequired: true,
      },
      onAction: async (reason) => {
        await reject.mutateAsync(reason ?? '')
        void message.success('Đã từ chối đơn đăng ký.')
      },
    }
    actions[ApplicationAction.ASSIGN] = {
      label: selected ? `Phân giường ${selected.label}` : 'Chọn giường để phân',
      icon: <HomeOutlined />,
      loading: assignBed.isPending,
      disabled: !selected,
      confirm: {
        title: 'Xác nhận phân giường',
        description: selected ? (
          <>
            Phân giường <strong>{selected.label}</strong> cho{' '}
            {student?.fullName ?? 'sinh viên'} (giá phòng{' '}
            {formatMoney(selected.price)}/học kỳ). Hệ thống tạo giữ chỗ và hợp
            đồng chờ kích hoạt; đơn chuyển sang Đã giữ chỗ.
          </>
        ) : (
          'Chọn một giường trống ở bảng bên cạnh trước khi xác nhận.'
        ),
        okText: 'Phân giường',
      },
      onAction: async () => {
        if (!selected) return
        try {
          await assignBed.mutateAsync(selected.id)
          setPicked(undefined)
          void message.success(
            'Đã phân giường và tạo hợp đồng chờ kích hoạt.',
          )
        } catch (error) {
          // Giường vừa bị lấy: bỏ lựa chọn để người dùng chọn lại; dữ liệu đã được tải lại.
          if (errorCodeOf(error) === RegistrationErrorCode.BED_TAKEN)
            setPicked(undefined)
          throw error
        }
      },
    }
    actions[ApplicationAction.RELEASE_HOLD] = {
      label: 'Hủy giữ chỗ',
      danger: true,
      icon: <RollbackOutlined />,
      loading: releaseHold.isPending,
      confirm: {
        title: 'Hủy giữ chỗ của đơn',
        description:
          'Đơn trở lại Đã duyệt, giường được trả và hợp đồng chờ kích hoạt bị hủy. Sinh viên cần được phân giường lại.',
        okText: 'Hủy giữ chỗ',
        reasonRequired: true,
      },
      onAction: async (reason) => {
        await releaseHold.mutateAsync(reason ?? '')
        void message.success('Đã hủy giữ chỗ, đơn trở lại Đã duyệt.')
      },
    }
  }
  if (canDelete) {
    actions[ApplicationAction.CANCEL] = {
      label: 'Hủy đơn',
      danger: true,
      icon: <StopOutlined />,
      loading: cancel.isPending,
      confirm: {
        title: 'Hủy đơn đăng ký',
        description:
          detail.status === ApplicationStatus.ASSIGNED
            ? 'Đơn đang giữ chỗ: hệ thống đóng giữ chỗ và hủy hợp đồng chờ kích hoạt cùng lúc. Lịch sử đơn vẫn được giữ lại.'
            : 'Đơn chuyển sang Đã hủy và không thể tiếp tục xử lý. Lịch sử đơn vẫn được giữ lại.',
        okText: 'Hủy đơn',
        danger: true,
      },
      onAction: async () => {
        await cancel.mutateAsync()
        void message.success('Đã hủy đơn đăng ký.')
      },
    }
  }

  const priorityColumns: TableColumnsType<ApplicationPriority> = [
    {
      title: 'Diện ưu tiên',
      dataIndex: 'name',
    },
    {
      title: 'Trọng số',
      dataIndex: 'weight',
      align: 'center',
      width: 100,
    },
    {
      title: 'Xác nhận',
      dataIndex: 'verified',
      width: 150,
      render: (value: boolean | null) => priorityVerifiedLabel(value),
    },
    {
      title: 'Ghi chú',
      dataIndex: 'note',
      render: (value: string | null | undefined) => value || '—',
    },
    {
      title: 'Người xác nhận',
      key: 'verifiedBy',
      render: (_value, row) => priorityVerifierLabel(row),
    },
  ]
  if (canVerify)
    priorityColumns.push({
      title: 'Thao tác',
      key: 'actions',
      width: 140,
      render: (_value, row) => (
        <Button size="small" onClick={() => setVerifyPriority(row)}>
          {row.verified == null ? 'Xác nhận' : 'Cập nhật'}
        </Button>
      ),
    })

  const timelineColumns: TableColumnsType<ApplicationTimelineEntry> = [
    {
      title: 'Thời điểm',
      dataIndex: 'at',
      render: (value: string) => formatDateTime(value),
    },
    {
      title: 'Từ trạng thái',
      dataIndex: 'fromStatus',
      render: (value: string | null) =>
        value ? (
          <StatusBadge entity="application" status={value} />
        ) : (
          '—'
        ),
    },
    {
      title: 'Đến trạng thái',
      dataIndex: 'toStatus',
      render: (value: string) => (
        <StatusBadge entity="application" status={value} />
      ),
    },
    {
      title: 'Người thực hiện',
      dataIndex: 'actorId',
      render: (_value: number | null, row) => personLabel(row),
    },
    {
      title: 'Ghi chú / lý do',
      dataIndex: 'reason',
      render: (value: string | null) => value || '—',
    },
  ]

  const buildingOptions = (assignCatalog.buildings.data ?? []).map(
    (record) => ({
      value: Number(record.id),
      label: String(record.name ?? record.id),
    }),
  )
  const roomTypeOptions = (assignCatalog.roomTypes.data ?? []).map(
    (record) => ({
      value: Number(record.id),
      label: String(record.name ?? record.id),
    }),
  )

  return (
    <>
      <PageHeader
        title={`Đơn đăng ký #${detail.id}`}
        status={<StatusBadge entity="application" status={detail.status} />}
        subtitle={
          student
            ? `${student.fullName} (${student.studentCode}) · ${periodName}`
            : periodName
        }
        breadcrumb={[
          { label: list.label, to: list.path },
          { label: `#${detail.id}` },
        ]}
        extra={
          <div
            style={{
              display: 'flex',
              flexWrap: 'wrap',
              justifyContent: 'flex-end',
              alignItems: 'center',
              gap: 'var(--space-2)',
            }}
          >
            <Button
              icon={<ArrowLeftOutlined />}
              onClick={() => navigate(list.path)}
            >
              Quay lại danh sách
            </Button>
            <ActionBar
              allowedActions={allowedActions}
              actions={actions}
            />
          </div>
        }
      />

      {detail.rejectReason ? (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 'var(--space-4)' }}
          title="Lý do từ chối"
          description={detail.rejectReason}
        />
      ) : null}

      <div className="dms-split-view">
        <div>
          <Card title="Sinh viên">
            {student ? (
              <Descriptions
                column={{ xs: 1, sm: 2 }}
                items={[
                  {
                    key: 'studentCode',
                    label: 'Mã sinh viên',
                    children: student.studentCode,
                  },
                  {
                    key: 'fullName',
                    label: 'Họ tên',
                    children: student.fullName,
                  },
                  {
                    key: 'gender',
                    label: 'Giới tính',
                    children: student.gender
                      ? (crudValueLabels[student.gender] ?? student.gender)
                      : '—',
                  },
                ]}
              />
            ) : (
              <p className="dms-page-subtitle" style={{ margin: 0 }}>
                Không có thông tin sinh viên trong đơn.
              </p>
            )}
          </Card>

          <Card title="Thông tin đơn">
            <Descriptions
              column={{ xs: 1, sm: 2 }}
              items={[
                {
                  key: 'period',
                  label: 'Đợt đăng ký',
                  children: periodName,
                },
                {
                  key: 'unit',
                  label: 'Đơn vị nguyện vọng',
                  children: unitName,
                },
                {
                  key: 'roomType',
                  label: 'Loại phòng mong muốn',
                  children: roomTypeName,
                },
                {
                  key: 'priorityScore',
                  label: 'Điểm ưu tiên',
                  children: detail.priorityScore ?? '—',
                },
                {
                  key: 'submittedAt',
                  label: 'Ngày nộp',
                  children: formatDateTime(detail.submittedAt),
                },
              ]}
            />
          </Card>

          {detail.note ? (
            <Card title="Ghi chú của sinh viên">
              <p style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
                {detail.note}
              </p>
            </Card>
          ) : null}

          {showHold ? (
            <Card title="Giường đang giữ">
              {hold || holdContractId ? (
                <>
                  <Descriptions
                    column={1}
                    size="small"
                    items={[
                      {
                        key: 'bed',
                        label: 'Giường',
                        children: holdBedLabel ?? '—',
                      },
                      {
                        key: 'room',
                        label: 'Phòng',
                        children: holdRoom ?? '—',
                      },
                      {
                        key: 'contract',
                        label: 'Hợp đồng',
                        children:
                          hold?.contractCode ??
                          (holdContractId ? `#${holdContractId}` : '—'),
                      },
                      {
                        key: 'deadline',
                        label: 'Hạn kích hoạt',
                        children: (
                          <span
                            style={{
                              display: 'inline-flex',
                              flexWrap: 'wrap',
                              alignItems: 'center',
                              gap: 'var(--space-2)',
                            }}
                          >
                            {formatDateTime(holdDeadline)}
                            {deadlineTag}
                          </span>
                        ),
                      },
                    ]}
                  />
                  {holdContractId ? (
                    <div style={{ marginTop: 'var(--space-4)' }}>
                      <Button
                        onClick={() =>
                          navigate(contractPath(actorRole, holdContractId))
                        }
                      >
                        Xem hợp đồng
                      </Button>
                    </div>
                  ) : null}
                </>
              ) : (
                <p className="dms-page-subtitle" style={{ margin: 0 }}>
                  {actorRole === Role.STUDENT
                    ? 'Đơn đã duyệt và đang chờ Ban quản lý phân giường.'
                    : 'Đơn đã duyệt nhưng chưa phân giường. Chọn giường ở bảng bên cạnh để giữ chỗ.'}
                </p>
              )}
            </Card>
          ) : null}
        </div>

        <div>
          {canAssign ? (
            <>
              {beds.isError ? (
                <Alert
                  type="error"
                  showIcon
                  style={{ marginBottom: 'var(--space-4)' }}
                  title="Không tải được danh sách giường trống"
                  description={errorMessage(beds.error)}
                  action={
                    <Button size="small" onClick={() => void beds.refetch()}>
                      Thử lại
                    </Button>
                  }
                />
              ) : null}
              <BedPicker
                title="Chọn giường để phân"
                rooms={beds.data ?? []}
                loading={beds.isLoading}
                selectedBedId={selected?.id}
                onSelect={setPicked}
                buildings={buildingOptions}
                roomTypes={roomTypeOptions}
                buildingId={filter.buildingId}
                roomTypeId={filter.roomTypeId}
                onFilterChange={setFilter}
              />
            </>
          ) : null}

          <Card title="Diện ưu tiên">
            {priorities.length === 0 ? (
              <EmptyState
                title="Chưa có diện ưu tiên"
                description="Đơn không đăng ký diện ưu tiên nào; điểm ưu tiên bằng 0."
              />
            ) : (
              <DataTable<ApplicationPriority>
                dataSource={priorities}
                columns={priorityColumns}
                rowKey={(row) => row.categoryId}
                pagination={false}
                size="small"
              />
            )}
          </Card>

          <Card title="Tiến trình xử lý">
            <StatusTimeline steps={steps} />
          </Card>

          <Card title="Lịch sử xử lý">
            <DataTable<ApplicationTimelineEntry>
              dataSource={timeline}
              columns={timelineColumns}
              rowKey={(row) =>
                `${row.at}-${row.toStatus}-${row.actorId ?? ''}-${row.reason ?? ''}`
              }
              pagination={false}
              size="small"
            />
          </Card>
        </div>
      </div>

      {verifyPriority ? (
        <VerifyPriorityModal
          applicationId={id}
          priority={verifyPriority}
          open
          onClose={() => setVerifyPriority(null)}
        />
      ) : null}
    </>
  )
}
