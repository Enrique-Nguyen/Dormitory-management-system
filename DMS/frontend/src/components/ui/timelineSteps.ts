import type { ReactNode } from 'react'
import { getStatus, type StatusEntity } from '../../constants/statusConfig'

export type StepState = 'done' | 'current' | 'todo' | 'ended'

export interface TimelineStep {
  key: string
  title: string
  state: StepState
  /** Người thực hiện hoặc ghi chú ngắn ("KTV: Nguyễn C"). */
  description?: ReactNode
  at?: string | null
  /** Lý do cho nhánh kết thúc (Bị từ chối, Đã hủy). */
  reason?: string | null
}

export interface StepDetail {
  description?: ReactNode
  at?: string | null
  reason?: string | null
}

/**
 * Dựng các bước từ chuỗi trạng thái chuẩn của luồng, ví dụ ticket:
 *   stepsFromFlow('ticket', ['OPEN','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED'], 'IN_PROGRESS', { OPEN: { at: '...' } })
 * Trạng thái nằm ngoài luồng chuẩn (REJECTED, CANCELLED) là nhánh kết thúc: thêm một bước kết thúc (truyền `reason` trong
 * details[current]). `reachedStatus` là trạng thái cuối cùng đã đạt trước khi kết thúc (`fromStatus` của dòng timeline
 * cuối từ API); các bước tới đó được coi là đã qua dù chưa có chi tiết. Bỏ trống thì chỉ giữ bước có sẵn chi tiết.
 */
export function stepsFromFlow(
  entity: StatusEntity,
  flow: string[],
  current: string,
  details: Record<string, StepDetail> = {},
  reachedStatus?: string,
): TimelineStep[] {
  const index = flow.indexOf(current)
  if (index === -1) {
    // Trạng thái nằm ngoài luồng chuẩn → nhánh kết thúc
    const reached = reachedStatus ? flow.indexOf(reachedStatus) : -1
    const done = flow
      .filter((status, i) => i <= reached || details[status])
      .map((status) => toStep(entity, status, 'done', details[status]))
    return [...done, toStep(entity, current, 'ended', details[current])]
  }
  return flow.map((status, i) => {
    const state: StepState = i < index ? 'done' : i === index ? (index === flow.length - 1 ? 'done' : 'current') : 'todo'
    return toStep(entity, status, state, details[status])
  })
}

function toStep(entity: StatusEntity, status: string, state: StepState, detail: StepDetail = {}): TimelineStep {
  return { key: status, title: getStatus(entity, status).label, state, ...detail }
}
