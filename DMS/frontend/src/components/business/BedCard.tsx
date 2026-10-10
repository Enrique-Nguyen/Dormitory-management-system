import { StarFilled } from '@ant-design/icons'
import { bedOccupancy, bedOperation, type BedOccupancy } from '../../constants/statusConfig'

interface Props {
  /** Số giường hiển thị trên ô ("03"). */
  number: string
  /** Chiếm dụng (tính ra từ bed + assignment mở). Bỏ qua khi giường Hỏng. */
  occupancy: BedOccupancy
  /** Giường Hỏng: gạch chéo ô, không có chiếm dụng. */
  broken?: boolean
  /** Giường của chính người xem (cổng sinh viên): ★ Bạn. */
  isMine?: boolean
  /** Tên người ở/giữ (cổng Staff), hoặc ghi chú ngắn như "hạn 15/10". */
  caption?: string
  onClick?: () => void
}

const BROKEN_STRIKE = 'linear-gradient(to top right, transparent calc(50% - 1px), #6B7280 50%, transparent calc(50% + 1px))'

/** Một ô giường. Trạng thái luôn có chữ, không chỉ màu nền (Rule 03). */
export default function BedCard({ number, occupancy, broken, isMine, caption, onClick }: Props) {
  const { label, background, border } = bedOccupancy[occupancy]
  const stateLabel = broken ? bedOperation.BROKEN.label : label
  const Tag = onClick ? 'button' : 'div'

  return (
    <Tag
      type={onClick ? 'button' : undefined}
      onClick={onClick}
      aria-label={`Giường ${number}: ${stateLabel}${isMine ? ', giường của bạn' : ''}${caption ? `, ${caption}` : ''}`}
      className="dms-wrap"
      style={{
        minWidth: 88,
        minHeight: 72,
        padding: 8,
        textAlign: 'center',
        font: 'inherit',
        color: '#1F2937',
        borderRadius: 6,
        border: `${isMine ? 2 : 1}px solid ${isMine ? '#1D4ED8' : broken ? '#9CA3AF' : border}`,
        background: broken ? '#F3F4F6' : background,
        backgroundImage: broken ? BROKEN_STRIKE : undefined,
        cursor: onClick ? 'pointer' : 'default',
      }}
    >
      <div style={{ fontWeight: 600 }}>
        {isMine && <StarFilled aria-hidden="true" style={{ color: '#B45309', marginInlineEnd: 4 }} />}
        {number}
      </div>
      <div style={{ fontSize: 12 }}>{isMine ? 'Bạn' : stateLabel}</div>
      {caption && <div style={{ fontSize: 12, color: '#6B7280' }}>{caption}</div>}
    </Tag>
  )
}
