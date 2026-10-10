import { WarningOutlined } from '@ant-design/icons'
import { Card, Flex, Tag } from 'antd'
import { Link } from 'react-router'
import { bedOccupancy, bedOperation, roomOperation, type BedOccupancy } from '../../constants/statusConfig'

export interface RoomBed {
  occupancy: BedOccupancy
  broken?: boolean
}

interface Props {
  code: string
  /** "Phòng 6 người" */
  typeLabel: string
  /** Vận hành phòng; ACTIVE thì không hiện badge (UI 2.2 lớp 3). */
  operation?: keyof typeof roomOperation
  beds: RoomBed[]
  leaderName?: string | null
  /** Link tới chi tiết phòng. */
  to?: string
}

const dot: Record<BedOccupancy, string> = { OCCUPIED: '●', HELD: '◐', FREE: '○' }

/** Thẻ phòng cho Card View của Staff (UI mục 15). Số liệu chiếm dụng tính từ `beds`, không nhận số đã tính sẵn. */
export default function RoomCard({ code, typeLabel, operation = 'ACTIVE', beds, leaderName, to }: Props) {
  const usable = beds.filter((bed) => !bed.broken)
  const count = (state: BedOccupancy) => usable.filter((bed) => bed.occupancy === state).length
  const broken = beds.length - usable.length
  const occupied = count('OCCUPIED')
  const part = (n: number, label: string) => `${n} ${label.toLowerCase()}`
  const summary = [
    part(occupied, bedOccupancy.OCCUPIED.label),
    part(count('HELD'), bedOccupancy.HELD.label),
    part(count('FREE'), bedOccupancy.FREE.label),
    ...(broken ? [part(broken, bedOperation.BROKEN.label)] : []),
  ].join(' · ')
  const op = roomOperation[operation]

  return (
    <Card size="small" title={code} extra={operation !== 'ACTIVE' && <Tag>{op.label}</Tag>}>
      <div>{typeLabel}</div>
      <div aria-label={summary} style={{ letterSpacing: 4, margin: '8px 0' }}>
        {beds.map((bed, i) => (
          <span key={i} aria-hidden="true">
            {bed.broken ? '✕' : dot[bed.occupancy]}
          </span>
        ))}
      </div>
      <div style={{ fontSize: 12 }}>{summary}</div>
      <Flex vertical gap={4} style={{ marginTop: 8 }}>
        {leaderName ? (
          <span>Trưởng phòng: {leaderName}</span>
        ) : (
          occupied > 0 && (
            <Tag icon={<WarningOutlined aria-hidden="true" />} variant="outlined" color="#B45309" style={{ background: '#fff' }}>
              Chưa có trưởng phòng
            </Tag>
          )
        )}
        {to && <Link to={to}>Chi tiết</Link>}
      </Flex>
    </Card>
  )
}
