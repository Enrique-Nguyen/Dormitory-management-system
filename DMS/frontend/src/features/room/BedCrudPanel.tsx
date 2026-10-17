import { DeleteOutlined, EditOutlined } from '@ant-design/icons'
import { App, Form, Space, Tooltip } from 'antd'
import { useState } from 'react'
import { Input } from '../../components/ui/FormControls'
import { Button, Card, Modal, Tag } from '../../components/ui/Surface'
import Table from '../../components/ui/DataTable'
import { Permission } from '../../constants/authorization'
import {
  assignmentOccupancy,
  bedOccupancy,
  bedOperation,
} from '../../constants/statusConfig'
import { useApiErrorHandler } from '../../hooks/useApiErrorHandler'
import { hasPermission } from '../../services/api/auth'
import type { RoomBedItem } from '../../services/api/room'
import { useMe } from '../account/useMe'
import {
  saveOrUpdate,
  validationFieldErrors,
  writeCrudRecord,
} from '../crud/api'

/** Bảng giường của phòng: thêm, sửa mã và xóa giường chưa từng được phân (quyền BED_*). */
export default function BedCrudPanel({
  roomId,
  beds,
  reload,
}: {
  roomId: number
  beds: RoomBedItem[]
  reload: () => Promise<unknown>
}) {
  const { modal } = App.useApp()
  const handleApiError = useApiErrorHandler()
  const { data: me } = useMe()
  const canRead = hasPermission(me, Permission.BED_READ)
  const canCreate = hasPermission(me, Permission.BED_CREATE)
  const canUpdate = hasPermission(me, Permission.BED_UPDATE)
  const canDelete = hasPermission(me, Permission.BED_DELETE)
  const [form] = Form.useForm()
  const [editing, setEditing] = useState<RoomBedItem | null | undefined>()
  const [busy, setBusy] = useState(false)
  async function save(values: Record<string, unknown>) {
    if (editing ? !canUpdate : !canCreate) return
    setBusy(true)
    try {
      await saveOrUpdate('/beds', {
        code: values.code,
        roomId,
        ...(editing ? { id: editing.id } : {}),
      })
      setEditing(undefined)
      form.resetFields()
      await reload()
    } catch (e) {
      form.setFields(
        validationFieldErrors(e).map((item) => ({
          name: item.field,
          errors: [item.message],
        })),
      )
      handleApiError(e)
    } finally {
      setBusy(false)
    }
  }
  if (!canRead) return null
  return (
    <Card
      title="Giường trong phòng"
      extra={
        canCreate && (
          <Button
            onClick={() => {
              form.resetFields()
              setEditing(null)
            }}
          >
            Thêm giường
          </Button>
        )
      }
    >
      <Table<RoomBedItem>
        rowKey="id"
        pagination={false}
        dataSource={beds}
        columns={[
          { title: 'Mã', dataIndex: 'code' },
          {
            title: 'Vận hành',
            dataIndex: 'status',
            render: (value: string) => (
              <Tag>{bedOperation[value]?.label ?? value}</Tag>
            ),
          },
          {
            title: 'Người ở / giữ',
            dataIndex: 'occupant',
            render: (_: unknown, bed) =>
              bed.occupant
                ? `${bed.occupant.fullName} · ${bed.occupant.studentCode} · ${
                    bedOccupancy[assignmentOccupancy[bed.occupant.kind]].label
                  }`
                : '—',
          },
          {
            title: 'Thao tác',
            key: 'actions',
            width: 100,
            render: (_: unknown, bed) => (
              <Space size={4} wrap={false}>
                {canUpdate && (
                  <Tooltip title="Sửa mã giường">
                    <Button
                      size="small"
                      icon={<EditOutlined />}
                      onClick={() => {
                        form.setFieldsValue({ code: bed.code })
                        setEditing(bed)
                      }}
                      aria-label="Sửa mã giường"
                    />
                  </Tooltip>
                )}
                {canDelete && (
                  <Tooltip title="Xóa giường">
                    <Button
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                      onClick={() =>
                        modal.confirm({
                          title: 'Xóa giường chưa có lịch sử?',
                          onOk: async () => {
                            try {
                              await writeCrudRecord(`/beds/${bed.id}`, 'delete')
                              await reload()
                            } catch (e) {
                              handleApiError(e)
                              throw e
                            }
                          },
                        })
                      }
                      aria-label="Xóa giường"
                    />
                  </Tooltip>
                )}
              </Space>
            ),
          },
        ]}
      />
      <Modal
        title={editing ? 'Sửa giường' : 'Thêm giường'}
        open={editing !== undefined}
        onCancel={() => setEditing(undefined)}
        onOk={() => form.submit()}
        confirmLoading={busy}
        okText="Lưu"
        cancelText="Đóng"
      >
        <Form
          form={form}
          onFinish={(values) => {
            void save(values)
          }}
        >
          <Form.Item
            name="code"
            label="Mã giường"
            rules={[
              { required: true, message: 'Nhập mã giường' },
              { max: 10, message: 'Tối đa 10 ký tự' },
            ]}
          >
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
