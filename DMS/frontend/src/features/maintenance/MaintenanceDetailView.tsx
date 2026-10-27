import {
  ArrowLeftOutlined,
  AuditOutlined,
  CheckCircleOutlined,
  CloseOutlined,
  EditOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  StopOutlined,
  ToolOutlined,
  UserSwitchOutlined,
} from '@ant-design/icons'
import { App } from 'antd'
import type { TableColumnsType } from 'antd'
import { useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import {
  ActionBar,
  Button,
  Card,
  DataTable,
  Descriptions,
  EmptyState,
  PageHeader,
  PageSkeleton,
  StatusBadge,
  StatusTimeline,
  stepsFromFlow,
  type ActionDef,
} from '../../components'
import { Permission, Role } from '../../constants/authorization'
import { STUDENT_MAINTENANCE_PATH } from '../../constants/navigation'
import { getStatus } from '../../constants/statusConfig'
import { useMe } from '../account/useMe'
import { hasPermission } from '../../services/api/auth'
import type {
  MaintenanceDeduction,
  MaintenanceStudentRef,
  MaintenanceTimelineEntry,
} from '../../services/api/maintenance'
import { errorMessage } from '../../utils/errorMessages'
import { formatDate, formatDateTime, formatMoney } from '../../utils/format'
import AssignTechnicianModal from './AssignTechnicianModal'
import ConcludeTicketModal from './ConcludeTicketModal'
import MaintenancePhotoGallery from './MaintenancePhotoGallery'
import TicketNoteModal from './TicketNoteModal'
import {
  MaintenanceAction,
  maintenanceActionLabels,
  maintenanceCauseLabels,
  maintenanceLabel,
  maintenanceTicketFlow,
  maintenanceValueLabel,
} from './constants'
import {
  useAddTicketNote,
  useAcceptTicket,
  useCancelTicket,
  useMaintenanceDetail,
  useRejectTicket,
  useReopenTicket,
  useResolveTicket,
  useStartTicket,
} from './useMaintenance'

type NoteAction =
  | typeof MaintenanceAction.ADD_NOTE
  | typeof MaintenanceAction.RESOLVE

function listDestination(role: Role | undefined) {
  if (role === Role.STUDENT)
    return { label: 'Báo hỏng', path: STUDENT_MAINTENANCE_PATH }
  if (role === Role.TECHNICIAN)
    return { label: 'Việc của tôi', path: '/technician/tasks' }
  if (role === Role.ADMIN)
    return { label: 'Sửa chữa', path: '/admin/maintenance' }
  return { label: 'Sửa chữa', path: '/staff/maintenance' }
}

function ticketStatusLabel(status: string | null) {
  return status ? getStatus('ticket', status).label : '—'
}

function locationLabel(detail: {
  roomId: number | null
  buildingId: number | null
  floor: number | null
}) {
  const parts: string[] = []
  if (detail.roomId) parts.push(`Phòng #${detail.roomId}`)
  if (detail.buildingId) parts.push(`Tòa #${detail.buildingId}`)
  if (detail.floor != null) parts.push(`Tầng ${detail.floor}`)
  return parts.length > 0 ? parts.join(' · ') : 'Chưa xác định'
}

const deductionColumns: TableColumnsType<MaintenanceDeduction> = [
  {
    title: 'Sinh viên',
    dataIndex: 'student',
    render: (student: MaintenanceStudentRef | null) =>
      student ? `${student.fullName} (${student.studentCode})` : '—',
  },
  {
    title: 'Hợp đồng',
    dataIndex: 'contractId',
    render: (value: number) => `#${value}`,
  },
  {
    title: 'Số tiền trừ',
    dataIndex: 'amount',
    align: 'right',
    render: (value: number) => formatMoney(value),
  },
  {
    title: 'Hạn nộp bù',
    dataIndex: 'topUpDueDate',
    render: (value: string | null) => formatDate(value),
  },
]

/**
 * Trang chi tiết ticket dùng chung cho Sinh viên, Quản lý và Kỹ thuật viên.
 * Chỉ hiện hành động backend trả trong allowedActions; quyền cập nhật/xóa được kiểm tra
 * thêm từ /auth/me. Không tự dựng lại bảng chuyển trạng thái.
 */
export default function MaintenanceDetailView({ role }: { role?: Role } = {}) {
  const id = Number(useParams().id)
  const ticket = useMaintenanceDetail(id)
  const { data: me } = useMe()
  const actorRole = role ?? me?.role
  const navigate = useNavigate()
  const { message } = App.useApp()

  const [assignOpen, setAssignOpen] = useState(false)
  const [noteAction, setNoteAction] = useState<NoteAction | null>(null)
  const [concludeOpen, setConcludeOpen] = useState(false)

  const reject = useRejectTicket(id)
  const accept = useAcceptTicket(id)
  const reopen = useReopenTicket(id)
  const cancel = useCancelTicket(id)
  const startWork = useStartTicket(id)
  const addNote = useAddTicketNote(id)
  const resolve = useResolveTicket(id)

  const list = listDestination(actorRole)

  if (ticket.isLoading) return <PageSkeleton />

  const detail = ticket.data
  if (!detail) {
    const invalidId = !Number.isSafeInteger(id) || id <= 0
    return (
      <>
        <PageHeader
          title="Không tìm thấy ticket"
          breadcrumb={[{ label: list.label, to: list.path }]}
        />
        <EmptyState
          title="Không tìm thấy yêu cầu sửa chữa"
          description={
            invalidId
              ? 'Đường dẫn ticket không hợp lệ.'
              : errorMessage(ticket.error)
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

  const allowedActions = detail.allowedActions ?? []
  const photos = detail.photos ?? []
  const timeline = detail.timeline ?? []
  const deductions = detail.deductions ?? []
  const concluded = detail.concluded ?? detail.cause != null
  const canUpdate = hasPermission(me, Permission.MAINTENANCE_UPDATE)
  const canDelete = hasPermission(me, Permission.MAINTENANCE_DELETE)
  const canConclude =
    canUpdate &&
    (actorRole === Role.STAFF || actorRole === Role.ADMIN) &&
    detail.roomId != null &&
    !concluded &&
    allowedActions.includes(MaintenanceAction.CONCLUDE)

  const lastTimeline = timeline.at(-1)
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
    'ticket',
    maintenanceTicketFlow,
    detail.status,
    timelineDetails,
    lastTimeline?.fromStatus ?? undefined,
  )

  const personLabel = (actorId: number | null) =>
    actorId == null
      ? 'Hệ thống'
      : actorId === me?.id
        ? 'Bạn'
        : `Người dùng #${actorId}`

  const timelineColumns: TableColumnsType<MaintenanceTimelineEntry> = [
    {
      title: 'Thời điểm',
      dataIndex: 'at',
      render: (value: string) => formatDateTime(value),
    },
    {
      title: 'Từ trạng thái',
      dataIndex: 'fromStatus',
      render: (value: string | null) => ticketStatusLabel(value),
    },
    {
      title: 'Đến trạng thái',
      dataIndex: 'toStatus',
      render: (value: string) => (
        <StatusBadge entity="ticket" status={value} />
      ),
    },
    {
      title: 'Người thực hiện',
      dataIndex: 'actorId',
      render: (value: number | null) => personLabel(value),
    },
    {
      title: 'Ghi chú / lý do',
      dataIndex: 'reason',
      render: (value: string | null) => value ?? '—',
    },
  ]

  const actions: Record<string, ActionDef> = {}
  if (canUpdate) {
    actions[MaintenanceAction.ASSIGN] = {
      label: maintenanceActionLabels[MaintenanceAction.ASSIGN],
      icon: <UserSwitchOutlined />,
      onAction: () => setAssignOpen(true),
    }
    actions[MaintenanceAction.REJECT] = {
      label: maintenanceActionLabels[MaintenanceAction.REJECT],
      danger: true,
      icon: <CloseOutlined />,
      loading: reject.isPending,
      confirm: {
        title: 'Từ chối yêu cầu sửa chữa',
        description:
          'Ticket chuyển sang Bị từ chối và giữ nguyên lịch sử. Người báo sẽ thấy lý do.',
        okText: 'Từ chối',
        reasonRequired: true,
      },
      onAction: async (reason) => {
        await reject.mutateAsync(reason ?? '')
        void message.success('Đã từ chối ticket.')
      },
    }
    actions[MaintenanceAction.START] = {
      label: maintenanceActionLabels[MaintenanceAction.START],
      icon: <PlayCircleOutlined />,
      loading: startWork.isPending,
      confirm: {
        title: 'Bắt đầu sửa chữa?',
        description:
          'Ticket chuyển sang Đang sửa để ghi nhận tiến độ và kết quả xử lý.',
        okText: 'Bắt đầu',
      },
      onAction: async () => {
        await startWork.mutateAsync()
        void message.success('Đã bắt đầu sửa chữa.')
      },
    }
    actions[MaintenanceAction.ADD_NOTE] = {
      label: maintenanceActionLabels[MaintenanceAction.ADD_NOTE],
      icon: <EditOutlined />,
      onAction: () => setNoteAction(MaintenanceAction.ADD_NOTE),
    }
    actions[MaintenanceAction.RESOLVE] = {
      label: maintenanceActionLabels[MaintenanceAction.RESOLVE],
      primary: true,
      icon: <ToolOutlined />,
      onAction: () => setNoteAction(MaintenanceAction.RESOLVE),
    }
    actions[MaintenanceAction.ACCEPT] = {
      label: maintenanceActionLabels[MaintenanceAction.ACCEPT],
      primary: true,
      icon: <CheckCircleOutlined />,
      loading: accept.isPending,
      confirm: {
        title: 'Nghiệm thu ticket',
        description:
          'Ticket chuyển sang Đã đóng. Chỉ nghiệm thu khi sự cố đã được khắc phục.',
        okText: 'Nghiệm thu',
        // Backend chỉ bắt buộc lý do khi Staff/Admin nghiệm thu thay người báo.
        reasonRequired: detail.requesterId !== me?.id,
      },
      onAction: async (reason) => {
        await accept.mutateAsync(reason)
        void message.success('Đã nghiệm thu và đóng ticket.')
      },
    }
    actions[MaintenanceAction.REOPEN] = {
      label: maintenanceActionLabels[MaintenanceAction.REOPEN],
      danger: true,
      icon: <ReloadOutlined />,
      loading: reopen.isPending,
      confirm: {
        title: 'Mở lại ticket',
        description:
          'Ticket trở lại Đang sửa để xử lý tiếp. Nêu rõ vấn đề còn tồn tại.',
        okText: 'Mở lại',
        reasonRequired: true,
      },
      onAction: async (reason) => {
        await reopen.mutateAsync(reason ?? '')
        void message.success('Đã mở lại ticket để xử lý tiếp.')
      },
    }
    if (canConclude) {
      actions[MaintenanceAction.CONCLUDE] = {
        label: maintenanceActionLabels[MaintenanceAction.CONCLUDE],
        icon: <AuditOutlined />,
        onAction: () => setConcludeOpen(true),
      }
    }
  }
  if (canDelete) {
    actions[MaintenanceAction.CANCEL] = {
      label: maintenanceActionLabels[MaintenanceAction.CANCEL],
      danger: true,
      icon: <StopOutlined />,
      loading: cancel.isPending,
      confirm: {
        title: 'Hủy ticket sửa chữa',
        description:
          'Ticket chuyển sang Đã hủy và không thể tiếp tục xử lý. Lịch sử vẫn được giữ lại.',
        okText: 'Hủy ticket',
        danger: true,
      },
      onAction: async () => {
        await cancel.mutateAsync()
        void message.success('Đã hủy ticket.')
      },
    }
  }

  const noteTitle =
    noteAction === MaintenanceAction.ADD_NOTE
      ? 'Thêm ghi chú tiến độ'
      : 'Xác nhận hoàn thành sửa chữa'
  const noteLabel =
    noteAction === MaintenanceAction.ADD_NOTE
      ? 'Ghi chú tiến độ'
      : 'Kết quả sửa chữa'
  const notePlaceholder =
    noteAction === MaintenanceAction.ADD_NOTE
      ? 'Mô tả công việc đã làm, linh kiện thay thế, thời gian dự kiến...'
      : 'Mô tả kết quả sửa chữa và tình trạng hiện tại để người báo nghiệm thu...'
  const noteSubmit =
    noteAction === MaintenanceAction.ADD_NOTE
      ? async (value: string) => {
          await addNote.mutateAsync(value)
          void message.success('Đã thêm ghi chú tiến độ.')
        }
      : async (value: string) => {
          await resolve.mutateAsync(value)
          void message.success('Đã ghi nhận hoàn thành, chờ nghiệm thu.')
        }

  const conclusionItems = [
    {
      key: 'cause',
      label: 'Nguyên nhân',
      children: maintenanceLabel(maintenanceCauseLabels, detail.cause),
    },
    ...(detail.repairCost != null
      ? [
          {
            key: 'repairCost',
            label: 'Chi phí sửa chữa',
            children: formatMoney(detail.repairCost),
          },
        ]
      : []),
    ...(detail.costNote
      ? [
          {
            key: 'costNote',
            label: 'Ghi chú kết luận',
            children: detail.costNote,
          },
        ]
      : []),
  ]

  return (
    <>
      <PageHeader
        title={`Yêu cầu sửa chữa ${detail.code || `#${detail.id}`}`}
        status={<StatusBadge entity="ticket" status={detail.status} />}
        subtitle={[
          maintenanceValueLabel(detail.category),
          maintenanceValueLabel(detail.severity),
          locationLabel(detail),
        ].join(' · ')}
        breadcrumb={[
          { label: list.label, to: list.path },
          { label: detail.code || `#${detail.id}` },
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
            <ActionBar allowedActions={allowedActions} actions={actions} />
          </div>
        }
      />

      <div className="dms-split-view">
        <div>
          <Card title="Thông tin ticket">
            <Descriptions
              column={{ xs: 1, sm: 2 }}
              items={[
                {
                  key: 'code',
                  label: 'Mã ticket',
                  children: detail.code || `#${detail.id}`,
                },
                {
                  key: 'category',
                  label: 'Hạng mục',
                  children: maintenanceValueLabel(detail.category),
                },
                {
                  key: 'severity',
                  label: 'Mức độ',
                  children: maintenanceValueLabel(detail.severity),
                },
                {
                  key: 'location',
                  label: 'Vị trí',
                  children: locationLabel(detail),
                },
                {
                  key: 'requester',
                  label: 'Người báo',
                  children:
                    detail.requesterId === me?.id
                      ? 'Bạn'
                      : `Người dùng #${detail.requesterId}`,
                },
                {
                  key: 'assignedTo',
                  label: 'KTV phụ trách',
                  children:
                    detail.assignedTo == null
                      ? 'Chưa phân công'
                      : detail.assignedTo === me?.id
                        ? 'Bạn'
                        : `KTV #${detail.assignedTo}`,
                },
                {
                  key: 'createdAt',
                  label: 'Thời điểm tạo',
                  children: formatDateTime(detail.createdAt),
                },
              ]}
            />
          </Card>
          <Card title="Mô tả sự cố">
            <p style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
              {detail.description || '—'}
            </p>
          </Card>
          <Card title="Ảnh hiện trường">
            <MaintenancePhotoGallery photos={photos} />
          </Card>
        </div>

        <div>
          <Card title="Tiến trình xử lý">
            <StatusTimeline steps={steps} />
          </Card>
          <Card title="Lịch sử xử lý">
            <DataTable<MaintenanceTimelineEntry>
              dataSource={timeline}
              columns={timelineColumns}
              rowKey={(row) =>
                `${row.at}-${row.toStatus}-${row.actorId ?? ''}-${row.reason ?? ''}`
              }
              pagination={false}
              size="small"
            />
          </Card>
          {concluded ? (
            <Card title="Kết luận xử lý">
              <Descriptions column={1} size="small" items={conclusionItems} />
              <div style={{ marginTop: 'var(--space-4)' }}>
                <div
                  style={{
                    fontWeight: 600,
                    marginBottom: 'var(--space-2)',
                  }}
                >
                  Khoản trừ cọc
                </div>
                {deductions.length > 0 ? (
                  <DataTable<MaintenanceDeduction>
                    dataSource={deductions}
                    columns={deductionColumns}
                    rowKey={(row) => row.contractId}
                    pagination={false}
                    size="small"
                  />
                ) : (
                  <p className="dms-page-subtitle" style={{ margin: 0 }}>
                    Không trừ cọc.
                  </p>
                )}
              </div>
            </Card>
          ) : null}
        </div>
      </div>

      <AssignTechnicianModal
        ticketId={id}
        open={assignOpen}
        onClose={() => setAssignOpen(false)}
      />
      {noteAction ? (
        <TicketNoteModal
          open
          title={noteTitle}
          label={noteLabel}
          placeholder={notePlaceholder}
          okText={
            noteAction === MaintenanceAction.ADD_NOTE
              ? 'Lưu ghi chú'
              : 'Xác nhận hoàn thành'
          }
          loading={addNote.isPending || resolve.isPending}
          onSubmit={noteSubmit}
          onClose={() => setNoteAction(null)}
        />
      ) : null}
      <ConcludeTicketModal
        ticketId={id}
        open={concludeOpen}
        onClose={() => setConcludeOpen(false)}
      />
    </>
  )
}
