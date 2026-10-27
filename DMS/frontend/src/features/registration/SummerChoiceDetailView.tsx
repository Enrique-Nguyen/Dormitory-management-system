import {
  CheckOutlined,
  CloseOutlined,
  EditOutlined,
  InboxOutlined,
  LogoutOutlined,
} from '@ant-design/icons'
import {
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { App, Flex } from 'antd'
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
import { portalPath } from '../../constants/navigation'
import { crudValueLabels } from '../../constants/statusConfig'
import { useApiErrorHandler } from '../../hooks/useApiErrorHandler'
import { hasPermission } from '../../services/api/auth'
import { contractStudentInfo } from '../../services/api/contract'
import {
  SummerChoice,
  SummerChoiceAction,
  SummerChoiceStatus,
  approveSummerChoice,
  getSummerChoice,
  rejectSummerChoice,
  summerChoiceContractCode,
  summerChoiceSemesterLabel,
  summerChoiceStudent,
} from '../../services/api/summer'
import { errorMessage } from '../../utils/errorMessages'
import { formatDate, formatDateTime } from '../../utils/format'
import { useMe } from '../account/useMe'
import { invalidateContractQueries, useContractDetail } from '../contract/useContract'
import { getLookupRecords } from '../crud/api'

const summerChoiceKeys = {
  detail: (id: number) => ['summer-choice', id] as const,
}

const SUMMER_FLOW: string[] = [
  SummerChoiceStatus.PENDING,
  SummerChoiceStatus.APPROVED,
]

function useSummerChoiceDetail(id: number) {
  return useQuery({
    queryKey: summerChoiceKeys.detail(id),
    queryFn: () => getSummerChoice(id),
    enabled: Number.isSafeInteger(id) && id > 0,
  })
}

function useReviewSummerChoice(id: number) {
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()
  return useMutation({
    mutationFn: (input: {
      action: typeof SummerChoiceAction.APPROVE | typeof SummerChoiceAction.REJECT
      reason?: string
    }) =>
      input.action === SummerChoiceAction.APPROVE
        ? approveSummerChoice(id)
        : rejectSummerChoice(id, input.reason ?? ''),
    onSuccess: (detail) => {
      invalidateContractQueries(queryClient)
      void queryClient.invalidateQueries({
        queryKey: summerChoiceKeys.detail(id),
      })
      return detail
    },
    onError,
  })
}

function choiceExplanation(choice: string) {
  if (choice === SummerChoice.STAY)
    return {
      type: 'success' as const,
      title: 'Ở hè — duyệt sẽ kéo dài hợp đồng',
      description:
        'Chỗ ở hiện tại được giữ nguyên; hệ thống gia hạn hợp đồng qua học kỳ hè và ghi nhận một lần gia hạn. Sinh viên đóng phí hè theo loại phòng.',
    }
  if (choice === SummerChoice.TEMP_LEAVE)
    return {
      type: 'info' as const,
      title: 'Tạm rời hè — duyệt lựa chọn, sau đó gửi đồ',
      description:
        'Duyệt chưa trả giường. Staff tạo phiếu gửi đồ để đóng phân phòng; hợp đồng và tiền cọc được giữ để sinh viên quay lại đầu năm học.',
    }
  if (choice === SummerChoice.LEAVE)
    return {
      type: 'warning' as const,
      title: 'Rời ký túc xá — duyệt lựa chọn, sau đó trả phòng',
      description:
        'Duyệt chưa kết thúc hợp đồng. Staff tiếp tục làm thủ tục trả phòng để chốt công nợ và hoàn cọc theo số dư.',
    }
  return {
    type: 'info' as const,
    title: 'Lựa chọn kỳ hè',
    description: 'Kiểm tra phương án sinh viên chọn trước khi quyết định.',
  }
}

export interface SummerChoiceDetailViewProps {
  role?: Role
  summerChoiceId?: number
}

/**
 * Chi tiết lựa chọn kỳ hè (API 4.13–4.15, UI 22B): phương án Ở hè/Tạm rời hè/Rời KTX, hợp đồng,
 * sinh viên, học kỳ và tác động khi duyệt. Staff duyệt/từ chối; STAY kéo dài hợp đồng, TEMP_LEAVE
 * và LEAVE chỉ ghi nhận để làm bước gửi đồ/trả phòng.
 */
export default function SummerChoiceDetailView({
  role,
  summerChoiceId,
}: SummerChoiceDetailViewProps = {}) {
  const params = useParams()
  const routeId = Number(params.id ?? params.summerChoiceId)
  const id =
    summerChoiceId != null &&
    Number.isSafeInteger(summerChoiceId) &&
    summerChoiceId > 0
      ? summerChoiceId
      : Number.isSafeInteger(routeId) && routeId > 0
        ? routeId
        : 0
  const { data: me } = useMe()
  const navigate = useNavigate()
  const { message } = App.useApp()
  const detailQuery = useSummerChoiceDetail(id)
  const detail = detailQuery.data
  const contractQuery = useContractDetail(detail?.contractId ?? 0)
  const semesters = useQuery({
    queryKey: ['crud-lookup', '/semesters'],
    queryFn: () => getLookupRecords('/semesters'),
    staleTime: 5 * 60 * 1000,
    enabled:
      detail != null &&
      detail.semesterId != null &&
      !detail.semesterName &&
      !detail.semester?.name,
  })
  const review = useReviewSummerChoice(id)
  const [submittingAction, setSubmittingAction] = useState<
    'approve' | 'reject' | null
  >(null)

  const actorRole = role ?? me?.role
  const studentOnly = actorRole === Role.STUDENT
  const listPath = studentOnly
    ? '/student/summer'
    : portalPath(actorRole, '/staff/summer')

  if (detailQuery.isLoading) return <PageSkeleton />

  if (!detail) {
    return (
      <>
        <PageHeader
          title="Không tìm thấy lựa chọn kỳ hè"
          breadcrumb={[{ label: 'Kỳ hè', to: listPath }]}
        />
        <EmptyState
          title="Không tìm thấy lựa chọn kỳ hè"
          description={
            id > 0
              ? errorMessage(detailQuery.error)
              : 'Đường dẫn lựa chọn không hợp lệ.'
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
  const contract = detail.contract ?? contractQuery.data ?? null
  const contractStudent = contractQuery.data
    ? contractStudentInfo(contractQuery.data)
    : null
  const summerStudent = summerChoiceStudent(detail)
  const student = {
    fullName: summerStudent.fullName ?? contractStudent?.fullName ?? null,
    studentCode:
      summerStudent.studentCode ?? contractStudent?.studentCode ?? null,
    studentId: summerStudent.studentId ?? contractStudent?.studentId ?? null,
  }
  const contractId = detail.contractId ?? contract?.id ?? null
  const contractCode =
    detail.contractCode ?? contract?.code ?? summerChoiceContractCode(detail)
  const contractStatus = contract?.status ?? detail.contractStatus ?? null
  const semesterRecord = semesters.data?.find(
    (record) => Number(record.id) === detail.semesterId,
  )
  const semesterLabel =
    detail.semesterName || detail.semester?.name
      ? summerChoiceSemesterLabel(detail)
      : semesterRecord
        ? `${String(semesterRecord.name ?? detail.semesterId ?? '')}${
            semesterRecord.academicYear
              ? ` · ${String(semesterRecord.academicYear)}`
              : ''
          }`
        : summerChoiceSemesterLabel(detail)
  const contractPath =
    contractId == null
      ? null
      : studentOnly
        ? `/student/contracts/${contractId}`
        : `${portalPath(actorRole, '/staff/contracts')}/${contractId}`
  const choiceLabel =
    crudValueLabels[detail.choice] ?? detail.choice ?? 'Chưa chọn'
  const explanation = choiceExplanation(detail.choice)
  const timeline = detail.timeline ?? []
  const pending = detail.status === SummerChoiceStatus.PENDING

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
    SUMMER_FLOW,
    detail.status,
    timelineDetails,
    timeline.at(-1)?.fromStatus ?? undefined,
  )

  const actions: Record<string, ActionDef> = {}
  if (
    !studentOnly &&
    hasPermission(me, Permission.SUMMER_REGISTRATION_UPDATE)
  ) {
    actions[SummerChoiceAction.APPROVE] = {
      label: 'Duyệt lựa chọn',
      icon: <CheckOutlined />,
      loading: review.isPending && submittingAction === 'approve',
      confirm: {
        title: `Duyệt phương án ${choiceLabel}?`,
        description: explanation.description,
        okText: 'Duyệt',
      },
      onAction: async () => {
        setSubmittingAction('approve')
        try {
          await review.mutateAsync({ action: SummerChoiceAction.APPROVE })
          message.success('Đã duyệt lựa chọn kỳ hè.')
        } finally {
          setSubmittingAction(null)
        }
      },
    }
    actions[SummerChoiceAction.REJECT] = {
      label: 'Từ chối',
      danger: true,
      icon: <CloseOutlined />,
      loading: review.isPending && submittingAction === 'reject',
      confirm: {
        title: 'Từ chối lựa chọn kỳ hè',
        description:
          'Sinh viên thấy lý do từ chối và có thể gửi lại lựa chọn khi còn trong thời hạn.',
        okText: 'Từ chối',
        reasonRequired: true,
      },
      onAction: async (reason) => {
        setSubmittingAction('reject')
        try {
          await review.mutateAsync({
            action: SummerChoiceAction.REJECT,
            reason,
          })
          message.success('Đã từ chối lựa chọn kỳ hè.')
        } finally {
          setSubmittingAction(null)
        }
      },
    }
  }
  if (studentOnly && hasPermission(me, Permission.SUMMER_REGISTRATION_UPDATE)) {
    actions[SummerChoiceAction.EDIT] = {
      label: 'Sửa lựa chọn',
      icon: <EditOutlined />,
      onAction: () => navigate(listPath),
    }
  }

  const canCreateStorage =
    !studentOnly &&
    detail.status === SummerChoiceStatus.APPROVED &&
    detail.choice === SummerChoice.TEMP_LEAVE &&
    contractId != null &&
    hasPermission(me, Permission.STORAGE_CREATE)
  const canMoveOut =
    !studentOnly &&
    detail.status === SummerChoiceStatus.APPROVED &&
    detail.choice === SummerChoice.LEAVE &&
    contractId != null &&
    hasPermission(me, Permission.MOVE_OUT_CREATE)

  return (
    <>
      <PageHeader
        title={`Lựa chọn kỳ hè #${detail.id}`}
        status={<StatusBadge entity="request" status={detail.status} />}
        subtitle={[
          choiceLabel,
          student.fullName
            ? `${student.fullName}${student.studentCode ? ` · ${student.studentCode}` : ''}`
            : student.studentId != null
              ? `Sinh viên #${student.studentId}`
              : null,
          semesterLabel,
        ]
          .filter(Boolean)
          .join(' · ')}
        breadcrumb={[
          { label: 'Kỳ hè', to: listPath },
          { label: `Lựa chọn #${detail.id}` },
        ]}
        extra={
          <Flex gap={8} wrap justify="flex-end">
            <Button onClick={() => navigate(listPath)}>Danh sách</Button>
            <ActionBar allowedActions={allowedActions} actions={actions} />
          </Flex>
        }
      />

      {pending && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          title="Lựa chọn đang chờ duyệt"
          description="Kiểm tra phương án và tác động trước khi duyệt; sinh viên chỉ đổi được lựa chọn khi còn chờ."
        />
      )}

      <div className="dms-split-view">
        <div>
          <Card title="Phương án kỳ hè">
            <Descriptions
              column={1}
              size="small"
              items={[
                {
                  key: 'choice',
                  label: 'Phương án',
                  children: <strong>{choiceLabel}</strong>,
                },
                {
                  key: 'semester',
                  label: 'Học kỳ hè',
                  children: semesterLabel,
                },
                {
                  key: 'contract',
                  label: 'Hợp đồng',
                  children:
                    contractPath && contractId != null ? (
                      <Link to={contractPath}>
                        {contractCode || `Hợp đồng #${contractId}`}
                      </Link>
                    ) : (
                      contractCode
                    ),
                },
                {
                  key: 'contractStatus',
                  label: 'Trạng thái hợp đồng',
                  children: contractStatus ? (
                    <StatusBadge entity="contract" status={contractStatus} />
                  ) : (
                    '—'
                  ),
                },
                {
                  key: 'contractPeriod',
                  label: 'Thời hạn hợp đồng',
                  children:
                    contract?.startDate ||
                    contract?.endDate ||
                    detail.contractEndDate
                      ? `${formatDate(contract?.startDate)} – ${formatDate(contract?.endDate ?? detail.contractEndDate)}`
                      : '—',
                },
                {
                  key: 'createdAt',
                  label: 'Gửi lúc',
                  children: formatDateTime(detail.createdAt),
                },
                {
                  key: 'decidedAt',
                  label: 'Quyết định lúc',
                  children: formatDateTime(detail.decidedAt),
                },
                {
                  key: 'decisionReason',
                  label:
                    detail.status === SummerChoiceStatus.REJECTED
                      ? 'Lý do từ chối'
                      : 'Ghi chú quyết định',
                  children: detail.decisionReason ?? '—',
                },
              ]}
            />
          </Card>

          <Alert
            type={explanation.type}
            showIcon
            style={{ marginTop: 16 }}
            title={explanation.title}
            description={explanation.description}
          />

          <Flex gap={8} wrap style={{ marginTop: 16 }}>
            {canCreateStorage && (
              <Button
                type="primary"
                icon={<InboxOutlined />}
                onClick={() =>
                  navigate(
                    `${portalPath(actorRole, '/staff/storage')}?contractId=${contractId}`,
                  )
                }
              >
                Tạo phiếu gửi đồ
              </Button>
            )}
            {canMoveOut && (
              <Button
                type="primary"
                danger
                icon={<LogoutOutlined />}
                onClick={() =>
                  navigate(
                    `${portalPath(actorRole, '/staff/contracts')}/${contractId}/move-out`,
                  )
                }
              >
                Làm thủ tục trả phòng
              </Button>
            )}
            {!studentOnly &&
              detail.status === SummerChoiceStatus.APPROVED &&
              detail.choice === SummerChoice.STAY &&
              contractPath && (
                <Button onClick={() => navigate(contractPath)}>
                  Xem hợp đồng đã gia hạn
                </Button>
              )}
          </Flex>
        </div>

        <Card title="Tiến trình lựa chọn">
          <StatusTimeline steps={steps} />
        </Card>
      </div>
    </>
  )
}
