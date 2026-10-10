import { CheckCircleFilled, ClockCircleFilled, CloseCircleFilled } from '@ant-design/icons'
import { Timeline, Typography } from 'antd'
import type { StepState, TimelineStep } from './timelineSteps'
import { formatDateTime } from '../../utils/format'

const stepColor: Record<StepState, string> = { done: '#15803D', current: '#1D4ED8', todo: '#9CA3AF', ended: '#B91C1C' }

export default function StatusTimeline({ steps }: { steps: TimelineStep[] }) {
  return (
    <Timeline
      items={steps.map((step) => ({
        key: step.key,
        color: stepColor[step.state],
        // Trạng thái nhận biết bằng icon + chữ, không chỉ bằng màu (Rule 03)
        icon:
          step.state === 'done' ? (
            <CheckCircleFilled aria-label="Đã qua" />
          ) : step.state === 'ended' ? (
            <CloseCircleFilled aria-label="Kết thúc" />
          ) : step.state === 'current' ? (
            <ClockCircleFilled aria-label="Hiện tại" />
          ) : undefined,
        // Tiêu đề nằm trong content để cả khối hiện một cột, bên phải mốc (antd 6 đặt `title` ở phía đối diện)
        content: (
          <div className="dms-wrap">
            <Typography.Text strong={step.state === 'current' || step.state === 'ended'} type={step.state === 'todo' ? 'secondary' : undefined}>
              {step.title}
            </Typography.Text>
            {step.description && <div>{step.description}</div>}
            {step.at && <Typography.Text type="secondary">{formatDateTime(step.at)}</Typography.Text>}
            {step.reason && <div>Lý do: {step.reason}</div>}
          </div>
        ),
      }))}
    />
  )
}
