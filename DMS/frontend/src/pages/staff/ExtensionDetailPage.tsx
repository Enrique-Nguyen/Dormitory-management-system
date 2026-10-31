import {
  CheckOutlined,
  CloseOutlined,
  StopOutlined,
} from '@ant-design/icons'
import { Flex } from 'antd'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import {
  ActionBar,
  Alert,
  Button,
  Card,
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
import {
  portalPath,
  STAFF_REQUESTS_PATH,
  STUDENT_REQUESTS_PATH,
} from '../../constants/navigation'
import { hasPermission } from '../../services/api/auth'
import {
  ExtensionAction,
  ExtensionStatus,
  extensionOldEndDate,
  extensionOverlapList,
  extensionStudentInfo,
  type ExtensionOverlap,
} from '../../services/api/contract'
import { errorMessage } from '../../utils/errorMessages'
import { formatDate, formatDateTime } from '../../utils/format'
import { useMe } from '../../features/account/useMe'
import { usePortalRole } from '../../hooks/usePortalRole'
import ExtensionReviewModal, {
  type ExtensionReviewMode,
} from '../../features/contract/ExtensionReviewModal'
import {
  useCancelExtension,
  useContractDetail,
  useExtensionRequest,
} from '../../features/contract/useContract'

const EXTENSION_FLOW: string[] = [
  ExtensionStatus.PENDING,
  ExtensionStatus.APPROVED,
]

function overlapLine(overlap: ExtensionOverlap) {
  const code = overlap.contractCode ?? `#${overlap.contractId ?? ''}`
  const period =
    overlap.startDate || overlap.endDate
      ? ` (${formatDate(overlap.startDate)} – ${formatDate(overlap.endDate)})`
      : ''
  return `${code}${period}`
}

export interface ExtensionDetailPageProps {
  /** Cổng đang xem; Admin dùng lại màn Staff với role ADMIN. */
  role?: Role
  extensionId?: number
  embedded?: boolean
  readOnly?: boolean
}

/**
 * Chi tiết yêu cầu gia hạn (API 4.11, UI 22A): ngày kết thúc cũ/mới, cảnh báo chồng khoảng lưu
 * trú, lý do và lịch sử. Duyệt/từ chối mở hộp thoại xem xét; nút chỉ hiện theo `allowedActions`.
 */
export default function ExtensionDetailPage({
  role,
  extensionId,
  embedded = false,
  readOnly = false,
}: ExtensionDetailPageProps = {}) {
  const params = useParams()
  const id = extensionId ?? Number(params.id ?? params.extensionId)
  const { data: me } = useMe()
  const navigate = useNavigate()
  const portalRole = usePortalRole()
  const request = useExtensionRequest(id)
  const detail = request.data
  const contractQuery = useContractDetail(detail?.contractId ?? 0)
  const cancel = useCancelExtension(id)
  const [reviewMode, setReviewMode] = useState<ExtensionReviewMode | null>(null)

  const actorRole = role ?? portalRole
  const studentOnly = actorRole === Role.STUDENT
  const listPath = studentOnly
    ? STUDENT_REQUESTS_PATH
    : portalPath(actorRole, STAFF_REQUESTS_PATH)

  if (request.isLoading) return <PageSkeleton />

  if (!detail) {
    return (
      <>
        <PageHeader
          title="Không tìm thấy yêu cầu gia hạn"
          breadcrumb={embedded ? undefined : [{ label: 'Yêu cầu chờ xử lý', to: listPath }]}
        />
        <EmptyState
          title="Không tìm thấy yêu cầu gia hạn"
          description={
            Number.isSafeInteger(id) && id > 0
              ? errorMessage(request.error)
              : 'Đường dẫn yêu cầu không hợp lệ.'
          }
          action={
            !embedded && (
              <Button type="primary" onClick={() => navigate(listPath)}>
                Quay lại danh sách
              </Button>
            )
          }
        />
      </>
    )
  }

  const allowedActions = detail.allowedActions ?? []
  const student = extensionStudentInfo(detail)
  const oldEndDate =
    extensionOldEndDate(detail) ?? contractQuery.data?.endDate ?? null
  const overlaps = extensionOverlapList(detail)
  const timeline = detail.timeline ?? []
  const pending = detail.status === ExtensionStatus.PENDING

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
    'request',
    EXTENSION_FLOW,
    detail.status,
    timelineDetails,
    timeline.at(-1)?.fromStatus ?? undefined,
  )

  const contractId = detail.contractId ?? null
  const contractPath =
    contractId == null
      ? null
      : studentOnly
        ? `/student/contracts/${contractId}`
        : `${portalPath(actorRole, '/staff/contracts')}/${contractId}`

  const actions: Record<string, ActionDef> = {}
  if (
    !readOnly &&
    !studentOnly &&
    hasPermission(me, Permission.CONTRACT_EXTENSION_UPDATE)
  ) {
    actions[ExtensionAction.APPROVE] = {
      label: 'Duyệt gia hạn',
      icon: <CheckOutlined />,
      onAction: () => setReviewMode('approve'),
    }
    actions[ExtensionAction.REJECT] = {
      label: 'Từ chối',
      danger: true,
      icon: <CloseOutlined />,
      onAction: () => setReviewMode('reject'),
    }
  }
  if (!readOnly && hasPermission(me, Permission.CONTRACT_EXTENSION_DELETE)) {
    actions[ExtensionAction.CANCEL] = {
      label: 'Hủy yêu cầu',
      danger: true,
      icon: <StopOutlined />,
      loading: cancel.isPending,
      confirm: {
        title: 'Hủy yêu cầu gia hạn?',
        description:
          'Yêu cầu chuyển sang Đã hủy. Hợp đồng giữ nguyên ngày kết thúc hiện tại.',
        okText: 'Hủy yêu cầu',
        danger: true,
      },
      onAction: async () => {
        await cancel.mutateAsync()
      },
    }
  }

  return (
    <>
      <PageHeader
        title={`Yêu cầu gia hạn #${detail.id}`}
        status={<StatusBadge entity="request" status={detail.status} />}
        subtitle={
          student.fullName
            ? `${student.fullName}${student.studentCode ? ` · ${student.studentCode}` : ''}`
            : student.studentId != null
              ? `Sinh viên #${student.studentId}`
              : undefined
        }
        breadcrumb={embedded ? undefined : [
          { label: 'Yêu cầu chờ xử lý', to: listPath },
          { label: `Gia hạn #${detail.id}` },
        ]}
        extra={(!embedded || !readOnly) && (
          <Flex gap={8} wrap justify="flex-end">
            {!embedded && <Button onClick={() => navigate(listPath)}>Danh sách</Button>}
            {!readOnly && <ActionBar allowedActions={allowedActions} actions={actions} />}
          </Flex>
        )}
      />

      {pending && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          title="Yêu cầu đang chờ duyệt"
          description="Duyệt sẽ cập nhật ngày kết thúc hợp đồng; từ chối bắt buộc ghi lý do để sinh viên biết."
        />
      )}

      {overlaps.length > 0 && (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
          title="Khoảng gia hạn chồng hợp đồng khác"
          description={
            <>
              Không thể duyệt khi thời gian lưu trú mới trùng:
              <ul style={{ margin: '8px 0 0', paddingInlineStart: 20 }}>
                {overlaps.map((overlap, index) => (
                  <li key={`${overlap.contractId ?? 'overlap'}-${index}`}>
                    {overlapLine(overlap)}
                  </li>
                ))}
              </ul>
            </>
          }
        />
      )}

      <div className="dms-split-view">
        <Card title="Thông tin yêu cầu">
          <Descriptions
            column={1}
            size="small"
            items={[
              {
                key: 'contract',
                label: 'Hợp đồng',
                children:
                  !embedded && contractPath ? (
                    <Link to={contractPath}>
                      {detail.contractCode ?? `Hợp đồng #${contractId}`}
                    </Link>
                  ) : (
                    detail.contractCode ?? (contractId != null ? `Hợp đồng #${contractId}` : '—')
                  ),
              },
              {
                key: 'oldEndDate',
                label: 'Ngày kết thúc hiện tại',
                children: formatDate(oldEndDate),
              },
              {
                key: 'newEndDate',
                label: 'Ngày kết thúc mới',
                children: formatDate(detail.newEndDate),
              },
              {
                key: 'reason',
                label: 'Lý do gia hạn',
                children: detail.reason ?? '—',
              },
              {
                key: 'submittedAt',
                label: 'Gửi lúc',
                children: formatDateTime(
                  detail.submittedAt ?? detail.createdAt,
                ),
              },
              {
                key: 'decidedAt',
                label: 'Quyết định lúc',
                children: formatDateTime(detail.decidedAt),
              },
              {
                key: 'decidedBy',
                label: 'Người quyết định',
                children:
                  detail.decidedByName ??
                  (detail.decidedBy != null
                    ? `Người dùng #${detail.decidedBy}`
                    : '—'),
              },
              {
                key: 'decisionReason',
                label:
                  detail.status === ExtensionStatus.REJECTED
                    ? 'Lý do từ chối'
                    : 'Ghi chú quyết định',
                children: detail.decisionReason ?? '—',
              },
            ]}
          />
        </Card>

        <Card title="Tiến trình yêu cầu">
          <StatusTimeline steps={steps} />
          {!pending && (
            <Alert
              type="info"
              showIcon
              style={{ marginTop: 16 }}
              title="Yêu cầu đã được xử lý"
              description={
                detail.status === ExtensionStatus.APPROVED
                  ? 'Ngày kết thúc hợp đồng đã được cập nhật theo quyết định.'
                  : 'Hợp đồng giữ nguyên ngày kết thúc; sinh viên có thể gửi yêu cầu mới khi còn đủ điều kiện.'
              }
            />
          )}
        </Card>
      </div>

      {!readOnly && reviewMode && hasPermission(me, Permission.CONTRACT_EXTENSION_UPDATE) && allowedActions.includes(reviewMode === 'approve' ? ExtensionAction.APPROVE : ExtensionAction.REJECT) && (
        <ExtensionReviewModal
          open
          detail={detail}
          mode={reviewMode}
          currentEndDate={oldEndDate}
          onClose={() => setReviewMode(null)}
        />
      )}
    </>
  )
}
