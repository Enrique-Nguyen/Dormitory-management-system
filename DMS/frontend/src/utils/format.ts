import dayjs, { type Dayjs } from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'

dayjs.extend(relativeTime)

// Định dạng hiển thị theo UI/UX mục 2.1a. Không tự format tiền/ngày trong component.
type DateInput = string | Date | Dayjs | null | undefined

const moneyFormat = new Intl.NumberFormat('vi-VN')

/** 1000000 → "1.000.000 đ" */
export function formatMoney(amount: number): string {
  return `${moneyFormat.format(amount)} đ`
}

/** Dòng sổ cọc: +1.000.000 đ / -200.000 đ */
export function formatSignedMoney(amount: number): string {
  return `${amount > 0 ? '+' : ''}${formatMoney(amount)}`
}

/** dd/MM/yyyy; giá trị rỗng hiện "—" */
export function formatDate(value: DateInput): string {
  return value ? dayjs(value).format('DD/MM/YYYY') : '—'
}

/** dd/MM/yyyy HH:mm */
export function formatDateTime(value: DateInput): string {
  return value ? dayjs(value).format('DD/MM/YYYY HH:mm') : '—'
}

/** "10 phút trước", "Hôm qua"... (locale vi đặt ở main.tsx) */
export function formatRelative(value: DateInput): string {
  return value ? dayjs(value).fromNow() : '—'
}

/** Số ngày còn lại tính theo ngày lịch; âm nếu đã qua. */
export function daysLeft(value: DateInput): number | null {
  if (!value) return null
  return dayjs(value).startOf('day').diff(dayjs().startOf('day'), 'day')
}

/** Mã đầy đủ của giường: tòa-phòng-giường ("A1-101-03"). */
export function formatBedCode(buildingCode: string, roomCode: string, bedCode: string) {
  return `${buildingCode}-${roomCode}-${bedCode}`
}
