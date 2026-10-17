import { CheckOutlined, CloseOutlined } from '@ant-design/icons'
import { Result } from 'antd'
import { useMemo, useState } from 'react'
import { useParams } from 'react-router'
import {
  ActionBar,
  BedPicker,
  PageHeader,
  PageSkeleton,
  StatusBadge,
  TransferSummary,
} from '../../components'
import { portalPath, STAFF_REQUESTS_PATH } from '../../constants/navigation'
import { TRANSFER_ACTION } from '../../constants/statusConfig'
import { useMe } from '../../features/account/useMe'
import {
  useApproveTransfer,
  useRejectTransfer,
  useTransferRequest,
} from '../../features/contract'
import { useAvailableBeds, useRoomCatalog } from '../../features/room'
import type { CrudRecord } from '../../features/crud/types'
import { errorMessage } from '../../utils/errorMessages'
import { formatBedCode } from '../../utils/format'

const { APPROVE, REJECT } = TRANSFER_ACTION

function toOptions(records: CrudRecord[] | undefined, labelKey: string) {
  return (records ?? []).map((record) => ({
    value: Number(record.id),
    label: String(record[labelKey] ?? record.id),
  }))
}

/**
 * Duyệt chuyển phòng (UI 22A, 14.2): trái là yêu cầu và giường hiện tại, phải là giường trống cùng
 * đơn vị. Nút chỉ hiện theo allowedActions; 409 (BED_TAKEN...) giữ trang và tải lại giường.
 */
export default function TransferReviewPage() {
  const id = Number(useParams().id)
  const request = useTransferRequest(id)
  const { data: me } = useMe()
  const detail = request.data
  const canApprove = detail?.allowedActions.includes(APPROVE) ?? false
  const [filter, setFilter] = useState<{
    buildingId?: number
    roomTypeId?: number
  }>({})
  const [picked, setPicked] = useState<number | undefined>()
  const beds = useAvailableBeds(
    { transferRequestId: id, ...filter },
    canApprove,
  )
  const buildings = useRoomCatalog('/buildings')
  const roomTypes = useRoomCatalog('/room-types')
  const approve = useApproveTransfer(id)
  const reject = useRejectTransfer(id)

  // Giường đã chọn không còn trong danh sách mới (vừa bị lấy, đổi bộ lọc) thì coi như chưa chọn.
  const selected = useMemo(() => {
    for (const { room, freeBeds } of beds.data ?? []) {
      const bed = freeBeds.find((item) => item.id === picked)
      if (bed)
        return {
          id: bed.id,
          label: formatBedCode(room.buildingCode, room.code, bed.code),
        }
    }
    return undefined
  }, [beds.data, picked])

  if (request.isLoading) return <PageSkeleton />
  if (!detail)
    return <Result status="404" title={errorMessage(request.error)} />

  const current = detail.currentBed
  return (
    <>
      <PageHeader
        title={`Yêu cầu chuyển phòng #${detail.id}`}
        status={<StatusBadge entity="request" status={detail.status} />}
        subtitle={`${detail.studentName} · ${detail.studentCode}`}
        breadcrumb={[
          {
            label: 'Yêu cầu chờ xử lý',
            to: portalPath(me?.role, STAFF_REQUESTS_PATH),
          },
          { label: `Chuyển phòng #${detail.id}` },
        ]}
        extra={
          <ActionBar
            allowedActions={detail.allowedActions}
            actions={{
              [REJECT]: {
                label: 'Từ chối',
                danger: true,
                icon: <CloseOutlined />,
                loading: reject.isPending,
                confirm: {
                  title: 'Từ chối yêu cầu chuyển phòng',
                  description:
                    'Sinh viên giữ nguyên giường hiện tại và thấy lý do từ chối.',
                  okText: 'Từ chối',
                  reasonRequired: true,
                },
                onAction: async (reason) => {
                  await reject.mutateAsync(reason ?? '')
                },
              },
              [APPROVE]: {
                label: selected
                  ? `Chuyển sang ${selected.label}`
                  : 'Chọn giường để chuyển',
                icon: <CheckOutlined />,
                loading: approve.isPending,
                disabled: !selected,
                confirm: {
                  title: 'Xác nhận chuyển phòng',
                  description: selected && (
                    <>
                      Chuyển {detail.studentName} từ{' '}
                      {current
                        ? formatBedCode(
                            current.buildingCode,
                            current.roomCode,
                            current.bedCode,
                          )
                        : 'giường hiện tại'}{' '}
                      sang <strong>{selected.label}</strong>. Đơn giá theo loại
                      phòng mới áp dụng từ hôm nay.
                    </>
                  ),
                  okText: 'Chuyển phòng',
                },
                onAction: async () => {
                  if (!selected) return
                  await approve.mutateAsync(selected.id)
                  setPicked(undefined)
                },
              },
            }}
          />
        }
      />
      <div className={canApprove ? 'dms-split-view' : undefined}>
        <TransferSummary detail={detail} />
        {canApprove && (
          <BedPicker
            title="Chọn giường đích (cùng đơn vị)"
            rooms={beds.data ?? []}
            loading={beds.isLoading}
            selectedBedId={selected?.id}
            onSelect={setPicked}
            buildings={toOptions(buildings.data, 'name')}
            roomTypes={toOptions(roomTypes.data, 'name')}
            buildingId={filter.buildingId}
            roomTypeId={filter.roomTypeId}
            onFilterChange={setFilter}
          />
        )}
      </div>
    </>
  )
}
