import { ArrowRightOutlined, CrownOutlined, WarningOutlined } from '@ant-design/icons'
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

const dotSymbol: Record<BedOccupancy, string> = { OCCUPIED: '●', HELD: '◐', FREE: '○' }

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
    <Card
      size="small"
      className="dms-room-card"
      title={
        <span style={{ fontSize: 16, fontWeight: 700, color: 'var(--color-heading)' }}>
          {code}
        </span>
      }
      extra={operation !== 'ACTIVE' && <Tag color="warning">{op.label}</Tag>}
    >
      <div style={{ color: 'var(--color-text-secondary)', fontSize: 13 }}>{typeLabel}</div>
      <div className="dms-bed-dots" aria-label={summary}>
        {beds.map((bed, i) => (
          <span
            key={i}
            aria-hidden="true"
            className={`dms-bed-dot ${
              bed.broken
                ? 'dms-bed-dot--broken'
                : bed.occupancy === 'OCCUPIED'
                  ? 'dms-bed-dot--occupied'
                  : bed.occupancy === 'HELD'
                    ? 'dms-bed-dot--held'
                    : 'dms-bed-dot--free'
            }`}
          >
            {bed.broken ? '✕' : dotSymbol[bed.occupancy]}
          </span>
        ))}
      </div>
      <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>{summary}</div>
      <Flex vertical gap={6} style={{ marginTop: 10, paddingTop: 10, borderTop: '1px solid var(--color-border-light)' }}>
        {leaderName ? (
          <div style={{ fontSize: 13, display: 'flex', alignItems: 'center', gap: 6 }}>
            <CrownOutlined style={{ color: '#D97706' }} />
            <span>Trưởng phòng: <strong>{leaderName}</strong></span>
          </div>
        ) : (
          occupied > 0 && (
            <Tag icon={<WarningOutlined aria-hidden="true" />} variant="outlined" color="#B45309" style={{ background: '#FFFBEB' }}>
              Chưa có trưởng phòng
            </Tag>
          )
        )}
        {to && (
          <Link to={to} style={{ fontSize: 13, display: 'inline-flex', alignItems: 'center', gap: 4, fontWeight: 500, color: 'var(--color-primary)' }}>
            <span>Xem chi tiết</span>
            <ArrowRightOutlined style={{ fontSize: 11 }} />
          </Link>
        )}
      </Flex>
    </Card>
  )
}
