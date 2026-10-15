import { stepsFromFlow } from '../ui/timelineSteps'
import StatusTimeline from '../ui/StatusTimeline'
import { Card, Descriptions } from '../ui/Surface'
import { bedOccupancy, assignmentOccupancy } from '../../constants/statusConfig'
import type { TransferDetail } from '../../services/api/contract'
import { formatBedCode, formatDateTime } from '../../utils/format'

/** Luồng chuẩn của yêu cầu chuyển phòng; Bị từ chối / Đã hủy là nhánh kết thúc. */
const TRANSFER_FLOW = ['PENDING', 'APPROVED']

/**
 * Thông tin yêu cầu chuyển phòng: sinh viên, giường hiện tại, lý do, kết quả và lịch sử. Dùng ở
 * cổng Staff (panel trái của split view) và cổng sinh viên. Chỉ nhận props.
 */
export default function TransferSummary({
  detail,
}: {
  detail: TransferDetail
}) {
  const bed = detail.currentBed
  const last = detail.timeline.at(-1)
  const steps = stepsFromFlow(
    'request',
    TRANSFER_FLOW,
    detail.status,
    {
      PENDING: { at: detail.createdAt },
      ...(last && last.toStatus === detail.status
        ? { [detail.status]: { at: last.at, reason: detail.decisionReason } }
        : {}),
    },
    last?.fromStatus ?? undefined,
  )
  return (
    <Card title="Yêu cầu">
      <Descriptions
        column={1}
        size="small"
        items={[
          {
            key: 'student',
            label: 'Sinh viên',
            children: `${detail.studentName} · ${detail.studentCode}`,
          },
          { key: 'contract', label: 'Hợp đồng', children: detail.contractCode },
          {
            key: 'bed',
            label: 'Giường hiện tại',
            children: bed
              ? `${formatBedCode(bed.buildingCode, bed.roomCode, bed.bedCode)} · ${
                  bedOccupancy[assignmentOccupancy[bed.kind]].label
                }`
              : 'Không còn phân phòng',
          },
          { key: 'reason', label: 'Lý do', children: detail.reason },
          {
            key: 'created',
            label: 'Gửi lúc',
            children: formatDateTime(detail.createdAt),
          },
          ...(detail.decisionReason
            ? [
                {
                  key: 'decision',
                  label: 'Lý do từ chối',
                  children: detail.decisionReason,
                },
              ]
            : []),
        ]}
      />
      <div style={{ marginTop: 16 }}>
        <StatusTimeline steps={steps} />
      </div>
    </Card>
  )
}
