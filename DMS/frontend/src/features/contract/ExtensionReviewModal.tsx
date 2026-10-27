import { App } from 'antd'
import { useState } from 'react'
import {
  Alert,
  Button,
  Descriptions,
  FormField,
  Modal,
  TextArea,
} from '../../components'
import {
  ExtensionAction,
  ExtensionStatus,
  extensionOldEndDate,
  extensionOverlapList,
  extensionStudentInfo,
  type ExtensionDetail,
} from '../../services/api/contract'
import { formatDate, formatDateTime } from '../../utils/format'
import { useApproveExtension, useRejectExtension } from './useContract'

export type ExtensionReviewMode = 'approve' | 'reject'

export interface ExtensionReviewModalProps {
  open: boolean
  detail: ExtensionDetail
  /** Thao tác Staff vừa chọn; mặc định là duyệt. */
  mode?: ExtensionReviewMode
  /** Ngày kết thúc hợp đồng hiện tại nếu API chi tiết yêu cầu chưa trả. */
  currentEndDate?: string | null
  onClose: () => void
  onSuccess?: (detail: ExtensionDetail) => void
}

function overlapText(overlap: ReturnType<typeof extensionOverlapList>[number]) {
  const code = overlap.contractCode ?? `#${overlap.contractId ?? ''}`
  const period =
    overlap.startDate || overlap.endDate
      ? ` (${formatDate(overlap.startDate)} – ${formatDate(overlap.endDate)})`
      : ''
  return `${code}${period}`
}

/**
 * Xem xét yêu cầu gia hạn (API 4.11, UI 22A): hiển thị ngày kết thúc cũ/mới và cảnh báo chồng
 * khoảng lưu trú do backend trả. Duyệt cập nhật endDate; từ chối bắt buộc lý do 5–500 ký tự.
 */
export default function ExtensionReviewModal({
  open,
  detail,
  mode = 'approve',
  currentEndDate,
  onClose,
  onSuccess,
}: ExtensionReviewModalProps) {
  const { message } = App.useApp()
  const approve = useApproveExtension(detail.id)
  const reject = useRejectExtension(detail.id)
  const [reason, setReason] = useState('')
  const [reasonError, setReasonError] = useState<string>()

  const allowedActions = detail.allowedActions ?? []
  const canApprove = allowedActions.includes(ExtensionAction.APPROVE)
  const canReject = allowedActions.includes(ExtensionAction.REJECT)
  const oldEndDate = extensionOldEndDate(detail) ?? currentEndDate ?? null
  const overlaps = extensionOverlapList(detail)
  const student = extensionStudentInfo(detail)
  const pending = detail.status === ExtensionStatus.PENDING
  const submitting = approve.isPending || reject.isPending

  function reset() {
    setReason('')
    setReasonError(undefined)
  }

  async function submitApprove() {
    if (!canApprove || submitting) return
    try {
      const result = await approve.mutateAsync()
      const contractCode = result?.contractCode ?? detail.contractCode
      message.success(
        contractCode
          ? `Đã duyệt gia hạn hợp đồng ${contractCode}.`
          : 'Đã duyệt yêu cầu gia hạn.',
      )
      onSuccess?.(result)
      onClose()
    } catch {
      // useApiErrorHandler đã hiện câu tiếng Việt; giữ hộp thoại để xem lại dữ liệu.
    }
  }

  async function submitReject() {
    if (!canReject || submitting) return
    const trimmed = reason.trim()
    if (trimmed.length < 5) {
      setReasonError('Lý do cần ít nhất 5 ký tự.')
      return
    }
    if (trimmed.length > 500) {
      setReasonError('Lý do tối đa 500 ký tự.')
      return
    }
    try {
      const result = await reject.mutateAsync(trimmed)
      message.success('Đã từ chối yêu cầu gia hạn.')
      onSuccess?.(result)
      onClose()
    } catch {
      // Giữ hộp thoại để Staff sửa lý do hoặc xem lỗi trạng thái.
    }
  }

  const footers = [
    <Button key="cancel" onClick={onClose} disabled={submitting}>
      Đóng
    </Button>,
  ]
  if (pending && mode === 'reject' && canReject) {
    footers.push(
      <Button
        key="reject"
        type="primary"
        danger
        loading={reject.isPending}
        onClick={() => void submitReject()}
      >
        Từ chối gia hạn
      </Button>,
    )
  } else if (pending && canApprove) {
    footers.push(
      <Button
        key="approve"
        type="primary"
        loading={approve.isPending}
        onClick={() => void submitApprove()}
      >
        Duyệt gia hạn
      </Button>,
    )
  }

  return (
    <Modal
      title={
        mode === 'reject'
          ? 'Từ chối yêu cầu gia hạn'
          : 'Duyệt yêu cầu gia hạn'
      }
      open={open}
      onCancel={() => {
        if (!submitting) onClose()
      }}
      footer={footers}
      width={720}
      mask={{ closable: false }}
      keyboard={!submitting}
      closable={!submitting}
      afterOpenChange={(visible) => {
        if (visible) reset()
      }}
    >
      <Descriptions
        bordered
        size="small"
        column={{ xs: 1, sm: 2 }}
        items={[
          {
            key: 'student',
            label: 'Sinh viên',
            children: student.fullName
              ? `${student.fullName}${student.studentCode ? ` · ${student.studentCode}` : ''}`
              : student.studentId != null
                ? `Sinh viên #${student.studentId}`
                : '—',
          },
          {
            key: 'contract',
            label: 'Hợp đồng',
            children:
              detail.contractCode ??
              (detail.contractId != null ? `#${detail.contractId}` : '—'),
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
            key: 'createdAt',
            label: 'Gửi lúc',
            children: formatDateTime(detail.submittedAt ?? detail.createdAt),
          },
          {
            key: 'reason',
            label: 'Lý do gia hạn',
            children: detail.reason ?? '—',
          },
        ]}
      />

      {overlaps.length > 0 && (
        <Alert
          type="error"
          showIcon
          style={{ marginTop: 16 }}
          title="Khoảng gia hạn chồng hợp đồng khác"
          description={
            <>
              Không thể duyệt khi thời gian lưu trú mới trùng hợp đồng:
              <ul style={{ margin: '8px 0 0', paddingInlineStart: 20 }}>
                {overlaps.map((overlap, index) => (
                  <li key={`${overlap.contractId ?? 'overlap'}-${index}`}>
                    {overlapText(overlap)}
                  </li>
                ))}
              </ul>
            </>
          }
        />
      )}

      {mode === 'approve' && !overlaps.length && (
        <Alert
          type="info"
          showIcon
          style={{ marginTop: 16 }}
          title="Duyệt sẽ cập nhật ngày kết thúc hợp đồng"
          description="Hệ thống kiểm tra chồng khoảng lưu trú với hợp đồng khác của sinh viên và ghi lịch sử trong cùng giao dịch."
        />
      )}

      {mode === 'reject' && (
        <div style={{ marginTop: 16 }}>
          <FormField
            label="Lý do từ chối"
            required
            error={reasonError}
            hint="5–500 ký tự. Sinh viên thấy lý do này trong chi tiết yêu cầu."
          >
            {(control) => (
              <TextArea
                {...control}
                rows={3}
                maxLength={500}
                showCount
                value={reason}
                placeholder="Ví dụ: ngày kết thúc mới trùng hợp đồng khác hoặc chưa đủ điều kiện gia hạn..."
                onChange={(event) => {
                  setReason(event.target.value)
                  setReasonError(undefined)
                }}
              />
            )}
          </FormField>
        </div>
      )}

      {!pending && (
        <Alert
          type="warning"
          showIcon
          style={{ marginTop: 16 }}
          title="Yêu cầu không còn chờ duyệt"
          description="Trạng thái đã thay đổi; đóng hộp thoại và tải lại chi tiết để xem kết quả mới nhất."
        />
      )}
    </Modal>
  )
}
