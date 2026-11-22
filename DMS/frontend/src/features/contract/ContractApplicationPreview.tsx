import { useQuery } from '@tanstack/react-query'
import { Alert, Descriptions, Spin } from 'antd'
import { useEffect } from 'react'
import { getLookupRecords, readValue } from '../crud/api'
import type { CrudRecord } from '../crud/types'
import { formatDate, formatMoney } from '../../utils/format'

const STALE_MS = 5 * 60 * 1000

const GENDER_LABEL: Record<string, string> = { M: 'Nam', F: 'Nữ' }

/** Tải danh mục một lần cho cả khung; React Query dùng chung cache với các ô chọn khác. */
function useLookup(endpoint: string, filters: Record<string, string> = {}) {
  return useQuery({
    queryKey: ['crud-lookup', endpoint, filters],
    queryFn: () => getLookupRecords(endpoint, filters),
    staleTime: STALE_MS,
  })
}

const byId = (rows: CrudRecord[] | undefined, id: unknown) =>
  rows?.find((row) => String(row.id) === String(id))

const text = (row: CrudRecord | undefined, path: string) => {
  const value = row ? readValue(row, path) : undefined
  return value == null || value === '' ? '—' : String(value)
}

const money = (value: unknown) =>
  typeof value === 'number' ? formatMoney(value) : '—'

interface Props {
  /** Đơn đã duyệt đang chọn; trống thì khung chỉ hiện hướng dẫn. */
  applicationId?: number
  /** Báo ngày bắt đầu/kết thúc của học kỳ để form tự điền khi người dùng chưa nhập. */
  onSemester?: (range: { start: string; end: string }) => void
  /** Báo khu nguyện vọng của đơn để lọc tòa khi chọn giường. */
  onApplication?: (application: CrudRecord | undefined) => void
}

/**
 * Khung tham khảo của đơn: sinh viên, khu, học kỳ, loại phòng nguyện vọng, giá và cọc dự kiến.
 * Giá và cọc ở đây chỉ để tham khảo; số chốt được nhập ở phần bên dưới của form hợp đồng.
 */
export default function ContractApplicationPreview({
  applicationId,
  onSemester,
  onApplication,
}: Props) {
  const applications = useLookup('/applications', { status: 'APPROVED' })
  const periods = useLookup('/registration-periods')
  const semesters = useLookup('/semesters')
  const roomTypes = useLookup('/room-types')
  const buildings = useLookup('/buildings')

  const application = applicationId == null ? undefined : byId(applications.data, applicationId)
  const period = byId(periods.data, application?.periodId)
  const semester = byId(semesters.data, period?.semesterId)
  const roomType = byId(roomTypes.data, application?.preferredRoomTypeId)
  const unitBuildings = (buildings.data ?? []).filter(
    (row) => String(row.managementUnitId) === String(application?.preferredUnitId),
  )
  const semesterStart = semester?.startDate as string | undefined
  const semesterEnd = semester?.endDate as string | undefined

  useEffect(() => {
    onApplication?.(application)
  }, [application, onApplication])

  useEffect(() => {
    if (semesterStart && semesterEnd) onSemester?.({ start: semesterStart, end: semesterEnd })
  }, [semesterStart, semesterEnd, onSemester])

  if (applicationId == null) {
    return (
      <Alert
        type="info"
        showIcon
        message="Thông tin tham khảo của đơn"
        description="Chọn một đơn đã duyệt để xem sinh viên, khu nguyện vọng, học kỳ, loại phòng, giá phòng và tiền cọc dự kiến. Ngày hợp đồng sẽ tự điền theo học kỳ."
      />
    )
  }
  if (applications.isLoading || periods.isLoading || semesters.isLoading) {
    return <Spin size="small" />
  }
  if (!application) {
    return <Alert type="warning" showIcon message="Không tìm thấy thông tin của đơn đã chọn" />
  }

  const deposits = [
    ...new Set(
      unitBuildings
        .map((row) => row.depositAmount)
        .filter((value): value is number => typeof value === 'number'),
    ),
  ].sort((a, b) => a - b)
  const depositText = deposits.length ? deposits.map((value) => formatMoney(value)).join(' hoặc ') : '—'
  const unitName = text(unitBuildings[0], 'managementUnitName')
  const buildingNames = unitBuildings.map((row) => text(row, 'code')).join(', ')

  return (
    <Descriptions
      bordered
      size="small"
      column={{ xs: 1, sm: 2 }}
      title="Thông tin tham khảo của đơn"
      items={[
        {
          key: 'student',
          label: 'Sinh viên',
          children: `${text(application, 'student.fullName')} (${text(application, 'student.studentCode')})`,
        },
        {
          key: 'gender',
          label: 'Giới tính',
          children: GENDER_LABEL[String(readValue(application, 'student.gender'))] ?? '—',
        },
        {
          key: 'unit',
          label: 'Khu nguyện vọng',
          children: buildingNames ? `${unitName} (${buildingNames})` : unitName,
        },
        { key: 'score', label: 'Điểm ưu tiên', children: text(application, 'priorityScore') },
        {
          key: 'semester',
          label: 'Học kỳ',
          children: semester
            ? `${text(semester, 'name')} (${formatDate(semesterStart ?? null)} – ${formatDate(semesterEnd ?? null)})`
            : '—',
        },
        {
          key: 'roomType',
          label: 'Loại phòng nguyện vọng',
          children: roomType ? `${text(roomType, 'name')} · ${text(roomType, 'bedCount')} giường` : 'Chưa chọn loại phòng',
        },
        {
          key: 'priceMain',
          label: 'Giá niêm yết / học kỳ',
          children: roomType ? money(roomType.priceMain) : 'Theo loại phòng của giường được chọn',
        },
        {
          key: 'deposit',
          label: 'Mức cọc niêm yết của tòa',
          children: depositText,
        },
      ]}
    />
  )
}
