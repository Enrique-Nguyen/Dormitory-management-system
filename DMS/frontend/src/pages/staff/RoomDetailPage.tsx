import { CrownOutlined, UserSwitchOutlined } from '@ant-design/icons'
import { Flex, Result, Typography } from 'antd'
import { useState } from 'react'
import { useParams } from 'react-router'
import {
  Alert,
  BedCard,
  Button,
  Card,
  DataTable,
  LeaderDialog,
  PageHeader,
  PageSkeleton,
  Tabs,
  Tag,
  useConfirm,
  type LeaderCandidate,
} from '../../components'
import { Permission } from '../../constants/authorization'
import { STAFF_ROOMS_PATH } from '../../constants/navigation'
import {
  assignmentOccupancy,
  roomGender,
  roomOperation,
} from '../../constants/statusConfig'
import { useMe } from '../../features/account/useMe'
import {
  BedCrudPanel,
  useAssignLeader,
  useRemoveLeader,
  useRoomCatalog,
  useRoomDetail,
} from '../../features/room'
import { hasPermission } from '../../services/api/auth'
import type { LeaderChange, StudentRef } from '../../services/api/room'
import { errorMessage } from '../../utils/errorMessages'
import { formatDateTime } from '../../utils/format'

const BROKEN = 'BROKEN'
const SYSTEM_ACTOR = 'Hệ thống'

function studentLabel(student: StudentRef | null) {
  return student ? `${student.fullName} (${student.studentCode})` : '—'
}

/**
 * Chi tiết phòng (UI 15): giường và người ở, trưởng phòng, cảnh báo thiếu trưởng phòng
 * (FR-ROOM-10), lịch sử đổi trưởng phòng (FR-ROOM-09) và quản lý giường.
 */
export default function RoomDetailPage() {
  const id = Number(useParams().id)
  const room = useRoomDetail(id)
  const { data: me } = useMe()
  const canUpdate = hasPermission(me, Permission.ROOM_UPDATE)
  const roomTypes = useRoomCatalog('/room-types')
  const buildings = useRoomCatalog('/buildings')
  const assign = useAssignLeader(id)
  const remove = useRemoveLeader(id)
  const { confirm, dialog } = useConfirm()
  const [leaderOpen, setLeaderOpen] = useState(false)

  if (room.isLoading) return <PageSkeleton />
  const detail = room.data
  if (!detail) return <Result status="404" title={errorMessage(room.error)} />

  const building = buildings.data?.find(
    (item) => Number(item.id) === detail.buildingId,
  )
  const roomType = roomTypes.data?.find(
    (item) => Number(item.id) === detail.roomTypeId,
  )
  const title = building
    ? `${String(building.code)}-${detail.code}`
    : detail.code
  const operation = roomOperation[detail.status]
  const occupants: LeaderCandidate[] = detail.beds.flatMap((bed) =>
    bed.occupant ? [bed.occupant] : [],
  )

  return (
    <>
      <PageHeader
        title={`Phòng ${title}`}
        status={operation && <Tag>{operation.label}</Tag>}
        subtitle={[
          roomType ? String(roomType.name) : null,
          roomGender[detail.gender] ?? detail.gender,
          `Tầng ${detail.floor}`,
          `${detail.occupiedBeds} đang ở · ${detail.heldBeds} đang giữ · ${detail.freeBeds} trống`,
        ]
          .filter(Boolean)
          .join(' · ')}
        breadcrumb={[
          { label: 'Phòng & Giường', to: STAFF_ROOMS_PATH },
          { label: title },
        ]}
      />
      {detail.missingLeader && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message="Phòng còn sinh viên đang ở nhưng chưa có trưởng phòng."
          description="Chỉ định trưởng phòng để nhận hóa đơn điện nước của phòng."
        />
      )}
      <Flex vertical gap={16}>
        <Card
          title="Trưởng phòng"
          extra={
            canUpdate && (
              <Flex gap={8} wrap>
                {detail.leader && (
                  <Button
                    danger
                    loading={remove.isPending}
                    onClick={() =>
                      confirm({
                        title: `Gỡ trưởng phòng ${title}`,
                        description: `${studentLabel(detail.leader)} sẽ không còn là trưởng phòng.`,
                        okText: 'Gỡ',
                        danger: true,
                        reasonRequired: true,
                        onConfirm: async (reason) => {
                          await remove.mutateAsync(reason ?? '')
                        },
                      })
                    }
                  >
                    Gỡ
                  </Button>
                )}
                <Button
                  type="primary"
                  icon={<UserSwitchOutlined />}
                  onClick={() => setLeaderOpen(true)}
                >
                  {detail.leader ? 'Đổi trưởng phòng' : 'Chỉ định trưởng phòng'}
                </Button>
              </Flex>
            )
          }
        >
          {detail.leader ? (
            <Flex gap={8} align="center">
              <CrownOutlined
                aria-hidden
                style={{ color: 'var(--color-warning)' }}
              />
              <Typography.Text strong>
                {studentLabel(detail.leader)}
              </Typography.Text>
            </Flex>
          ) : (
            <Typography.Text type="secondary">
              Chưa có trưởng phòng.
            </Typography.Text>
          )}
        </Card>
        <Tabs
          items={[
            {
              key: 'beds',
              label: 'Giường',
              children: (
                <Flex vertical gap={16}>
                  <div className="dms-bed-grid">
                    {detail.beds.map((bed) => (
                      <BedCard
                        key={bed.id}
                        number={bed.code}
                        broken={bed.status === BROKEN}
                        occupancy={
                          bed.occupant
                            ? assignmentOccupancy[bed.occupant.kind]
                            : 'FREE'
                        }
                        caption={bed.occupant?.fullName}
                      />
                    ))}
                  </div>
                  <BedCrudPanel
                    roomId={id}
                    beds={detail.beds}
                    reload={room.refetch}
                  />
                </Flex>
              ),
            },
            {
              key: 'history',
              label: 'Lịch sử trưởng phòng',
              children: (
                <DataTable<LeaderChange>
                  rowKey={(row) =>
                    `${row.at}-${row.to?.id ?? ''}-${row.from?.id ?? ''}`
                  }
                  pagination={false}
                  dataSource={[...detail.leaderHistory].reverse()}
                  columns={[
                    {
                      title: 'Thời điểm',
                      dataIndex: 'at',
                      render: (value: string) => formatDateTime(value),
                    },
                    {
                      title: 'Trưởng phòng cũ',
                      dataIndex: 'from',
                      render: (value: StudentRef | null) => studentLabel(value),
                    },
                    {
                      title: 'Trưởng phòng mới',
                      dataIndex: 'to',
                      render: (value: StudentRef | null) => studentLabel(value),
                    },
                    {
                      title: 'Người thực hiện',
                      dataIndex: 'actorName',
                      render: (value: string | null, row) =>
                        row.actorId == null ? SYSTEM_ACTOR : (value ?? '—'),
                    },
                    {
                      title: 'Lý do',
                      dataIndex: 'reason',
                      render: (value: string | null) => value ?? '—',
                    },
                  ]}
                />
              ),
            },
          ]}
        />
      </Flex>
      <LeaderDialog
        roomCode={title}
        open={leaderOpen}
        occupants={occupants}
        currentLeaderId={detail.leaderStudentId}
        loading={assign.isPending}
        onConfirm={(studentId) => assign.mutateAsync(studentId)}
        onClose={() => setLeaderOpen(false)}
      />
      {dialog}
    </>
  )
}
