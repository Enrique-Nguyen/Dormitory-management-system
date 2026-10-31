import { CheckOutlined, CloseOutlined, StopOutlined } from '@ant-design/icons'
import { Result } from 'antd'
import { useMemo, useState, type ComponentProps } from 'react'
import { useParams } from 'react-router'
import {
  ActionBar,
  BedPicker,
  Card,
  DataTable,
  Descriptions,
  PageHeader,
  PageSkeleton,
  StatusBadge,
  TransferSummary,
} from '../../components'
import { Permission, type Role } from '../../constants/authorization'
import { portalPath, STAFF_REQUESTS_PATH } from '../../constants/navigation'
import { TRANSFER_ACTION } from '../../constants/statusConfig'
import { useMe } from '../../features/account/useMe'
import {
  useApproveTransfer,
  useCancelTransfer,
  useRejectTransfer,
  useTransferRequest,
} from '../../features/contract'
import { useAvailableBeds, useRoomCatalog } from '../../features/room'
import type { CrudRecord } from '../../features/crud/types'
import { hasPermission } from '../../services/api/auth'
import type { TimelineEntry } from '../../services/api/contract'
import { errorMessage } from '../../utils/errorMessages'
import { formatBedCode, formatDateTime } from '../../utils/format'

const { APPROVE, REJECT, CANCEL } = TRANSFER_ACTION

function toOptions(records: CrudRecord[] | undefined, labelKey: string) {
  return (records ?? []).map((record) => ({
    value: Number(record.id),
    label: String(record[labelKey] ?? record.id),
  }))
}

// Danh mục chọn giường chỉ tải khi người xem thực sự được duyệt chuyển phòng.
function TransferBedPicker(
  props: Omit<ComponentProps<typeof BedPicker>, 'buildings' | 'roomTypes'>,
) {
  const buildings = useRoomCatalog('/buildings')
  const roomTypes = useRoomCatalog('/room-types')
  return (
    <BedPicker
      {...props}
      buildings={toOptions(buildings.data, 'name')}
      roomTypes={toOptions(roomTypes.data, 'name')}
    />
  )
}

export interface TransferReviewPageProps {
  role?: Role
  transferId?: number
  embedded?: boolean
  readOnly?: boolean
}

/**
 * Duyệt chuyển phòng (UI 22A, 14.2): trái là yêu cầu và giường hiện tại, phải là giường trống cùng
 * đơn vị. Nút chỉ hiện theo allowedActions và quyền; chế độ chỉ đọc không tải giường đích.
 */
export default function TransferReviewPage({
  role,
  transferId,
  embedded = false,
  readOnly = false,
}: TransferReviewPageProps = {}) {
  const params = useParams()
  const id = transferId ?? Number(params.id)
  const request = useTransferRequest(id)
  const { data: me } = useMe()
  const detail = request.data
  const canReview = !readOnly && hasPermission(me, Permission.ROOM_TRANSFER_APPROVE)
  const canDelete = !readOnly && hasPermission(me, Permission.ROOM_TRANSFER_DELETE)
  const canApprove = canReview && (detail?.allowedActions.includes(APPROVE) ?? false)
  const [filter, setFilter] = useState<{
    buildingId?: number
    roomTypeId?: number
  }>({})
  const [picked, setPicked] = useState<number | undefined>()
  const beds = useAvailableBeds(
    { transferRequestId: id, ...filter },
    canApprove,
  )
  const approve = useApproveTransfer(id)
  const reject = useRejectTransfer(id)
  const cancel = useCancelTransfer(id)

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
        breadcrumb={embedded ? undefined : [
          {
            label: 'Yêu cầu chờ xử lý',
            to: portalPath(role ?? me?.role, STAFF_REQUESTS_PATH),
          },
          { label: `Chuyển phòng #${detail.id}` },
        ]}
        extra={(canReview || canDelete) && (
          <ActionBar
            allowedActions={detail.allowedActions}
            actions={{
              ...(canReview ? {
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
              } : {}),
              ...(canDelete ? {
                [CANCEL]: {
                  label: 'Hủy yêu cầu',
                  danger: true,
                  icon: <StopOutlined />,
                  loading: cancel.isPending,
                  confirm: {
                    title: 'Hủy yêu cầu chuyển phòng?',
                    description: 'Giường hiện tại được giữ nguyên; lịch sử yêu cầu không bị xóa.',
                    okText: 'Hủy yêu cầu',
                  },
                  onAction: async () => {
                    await cancel.mutateAsync(undefined)
                  },
                },
              } : {}),
            }}
          />
        )}
      />
      <div className={canApprove ? 'dms-split-view' : undefined}>
        <div>
          <TransferSummary detail={detail} />
          <Card title="Kết quả xử lý">
            <Descriptions
              column={1}
              items={[
                {
                  key: 'targetBedId',
                  label: 'Giường đích',
                  children: detail.targetBedId != null ? `#${detail.targetBedId}` : '—',
                },
                {
                  key: 'decidedAt',
                  label: 'Quyết định lúc',
                  children: formatDateTime(detail.decidedAt),
                },
                {
                  key: 'decidedBy',
                  label: 'Người quyết định',
                  children: detail.decidedBy != null ? `#${detail.decidedBy}` : '—',
                },
              ]}
            />
          </Card>
        </div>
        {canApprove && (
          <TransferBedPicker
            title="Chọn giường đích (cùng đơn vị)"
            rooms={beds.data ?? []}
            loading={beds.isLoading}
            selectedBedId={selected?.id}
            onSelect={setPicked}
            buildingId={filter.buildingId}
            roomTypeId={filter.roomTypeId}
            onFilterChange={setFilter}
          />
        )}
      </div>
      <Card title="Lịch sử xử lý">
        <DataTable<TimelineEntry>
          dataSource={detail.timeline}
          rowKey={(row) => `${row.at}-${row.toStatus}-${row.actorId ?? ''}`}
          pagination={false}
          columns={[
            {
              title: 'Thời điểm',
              dataIndex: 'at',
              render: (value: string) => formatDateTime(value),
            },
            {
              title: 'Từ trạng thái',
              dataIndex: 'fromStatus',
              render: (value: string | null) => value ? <StatusBadge entity="request" status={value} /> : '—',
            },
            {
              title: 'Đến trạng thái',
              dataIndex: 'toStatus',
              render: (value: string) => <StatusBadge entity="request" status={value} />,
            },
            {
              title: 'Người thực hiện',
              dataIndex: 'actorId',
              render: (value: number | null) => value == null ? 'Hệ thống' : `#${value}`,
            },
            { title: 'Ghi chú / lý do', dataIndex: 'reason' },
          ]}
        />
      </Card>
    </>
  )
}
