import { Flex, Typography } from 'antd'
import { roomGender } from '../../constants/statusConfig'
import type { AvailableRoom } from '../../services/api/room'
import { formatBedCode, formatMoney } from '../../utils/format'
import EmptyState from '../ui/EmptyState'
import { Radio, RadioGroup, Select } from '../ui/FormControls'
import PageSkeleton from '../ui/PageSkeleton'
import { Card, FilterBar, Tag } from '../ui/Surface'

export interface PickerOption {
  value: number
  label: string
}

interface Props {
  /** Giường trống gom theo phòng (API 4.4); backend đã lọc đơn vị, giới tính và trạng thái. */
  rooms: AvailableRoom[]
  selectedBedId?: number
  onSelect: (bedId: number) => void
  buildings: PickerOption[]
  roomTypes: PickerOption[]
  buildingId?: number
  roomTypeId?: number
  onFilterChange: (filter: { buildingId?: number; roomTypeId?: number }) => void
  loading?: boolean
  title?: string
}

/**
 * Panel chọn giường của split view (UI 14.2, 22A): phân giường đơn đã duyệt và duyệt chuyển
 * phòng dùng chung. Chỉ nhận props; trang gọi API qua features/room.
 */
export default function BedPicker({
  rooms,
  selectedBedId,
  onSelect,
  buildings,
  roomTypes,
  buildingId,
  roomTypeId,
  onFilterChange,
  loading,
  title = 'Chọn giường',
}: Props) {
  return (
    <Card title={title}>
      <FilterBar>
        <Select<number>
          allowClear
          placeholder="Tòa"
          aria-label="Lọc theo tòa"
          options={buildings}
          value={buildingId}
          onChange={(value) =>
            onFilterChange({ buildingId: value, roomTypeId })
          }
        />
        <Select<number>
          allowClear
          placeholder="Loại phòng"
          aria-label="Lọc theo loại phòng"
          options={roomTypes}
          value={roomTypeId}
          onChange={(value) =>
            onFilterChange({ buildingId, roomTypeId: value })
          }
        />
      </FilterBar>
      {loading ? (
        <PageSkeleton rows={3} />
      ) : rooms.length === 0 ? (
        <EmptyState
          title="Không còn giường trống phù hợp"
          description="Bỏ bớt bộ lọc hoặc chờ có giường được trả."
        />
      ) : (
        <RadioGroup
          value={selectedBedId}
          onChange={(event) => onSelect(Number(event.target.value))}
          style={{ width: '100%' }}
        >
          <Flex vertical gap={12}>
            {rooms.map(
              ({ room, freeBeds, activeBeds, matchesPreferredType }) => (
                <div key={room.id} className="dms-bed-picker-room">
                  <Flex wrap gap={8} align="center">
                    <Typography.Text strong>
                      {room.buildingCode}-{room.code}
                    </Typography.Text>
                    <Typography.Text type="secondary">
                      {room.roomType.name} ·{' '}
                      {roomGender[room.gender] ?? room.gender} · Tầng{' '}
                      {room.floor}
                    </Typography.Text>
                    {matchesPreferredType && (
                      <Tag color="blue">Đúng nguyện vọng</Tag>
                    )}
                  </Flex>
                  <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                    {formatMoney(room.priceMain)}/HK · còn {freeBeds.length}/
                    {activeBeds} giường
                  </Typography.Text>
                  <Flex wrap gap={16} style={{ marginTop: 6 }}>
                    {freeBeds.map((bed) => (
                      <Radio
                        key={bed.id}
                        value={bed.id}
                        aria-label={`Giường ${formatBedCode(room.buildingCode, room.code, bed.code)}`}
                      >
                        Giường {bed.code}
                      </Radio>
                    ))}
                  </Flex>
                </div>
              ),
            )}
          </Flex>
        </RadioGroup>
      )}
    </Card>
  )
}
