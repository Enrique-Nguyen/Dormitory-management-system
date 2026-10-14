import { StarFilled, ToolOutlined } from '@ant-design/icons'
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

/** Một ô giường. Trạng thái luôn có chữ, không chỉ màu nền (Rule 03). */
export default function BedCard({ number, occupancy, broken, isMine, caption, onClick }: Props) {
  const { label, background, border } = bedOccupancy[occupancy]
  const stateLabel = broken ? bedOperation.BROKEN.label : label
  const Tag = onClick ? 'button' : 'div'

  const classes = [
    'dms-bed-card',
    'dms-wrap',
    onClick ? 'dms-bed-card--clickable' : '',
    isMine ? 'dms-bed-card--mine' : '',
    broken ? 'dms-bed-card--broken' : '',
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <Tag
      type={onClick ? 'button' : undefined}
      onClick={onClick}
      aria-label={`Giường ${number}: ${stateLabel}${isMine ? ', giường của bạn' : ''}${caption ? `, ${caption}` : ''}`}
      className={classes}
      style={{
        background: broken ? undefined : isMine ? undefined : background,
        borderColor: isMine ? undefined : broken ? undefined : border,
        font: 'inherit',
      }}
    >
      <div className="dms-bed-number">
        {isMine && <StarFilled aria-hidden="true" style={{ color: '#D97706', fontSize: 13 }} />}
        {broken && <ToolOutlined aria-hidden="true" style={{ color: '#DC2626', fontSize: 13 }} />}
        <span>{number}</span>
      </div>
      <div className="dms-bed-status" style={{ color: isMine ? '#1D4ED8' : undefined }}>
        {isMine ? 'Bạn' : stateLabel}
      </div>
      {caption && <div className="dms-bed-caption">{caption}</div>}
    </Tag>
  )
}
