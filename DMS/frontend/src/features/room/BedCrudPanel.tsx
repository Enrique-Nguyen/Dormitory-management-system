import { DeleteOutlined, EditOutlined } from '@ant-design/icons'
import { App, Form, Space, Tooltip } from 'antd'
import { useState } from 'react'
import { Input, Select, TextArea } from '../../components/ui/FormControls'
import { Button, Card, Modal, Tag } from '../../components/ui/Surface'
import Table from '../../components/ui/DataTable'
import { Permission } from '../../constants/authorization'
import {
  assignmentLabel,
  bedOperation,
  bedPositionLabels,
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
      // Chuỗi rỗng xóa thông tin mô tả tùy chọn; mã và phòng nguồn do backend kiểm tra.
      await saveOrUpdate('/beds', {
        code: values.code,
        roomId,
        position: values.position ?? '',
        assetCode: values.assetCode ?? '',
        note: values.note ?? '',
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
            title: 'Vị trí',
            dataIndex: 'position',
            render: (value: string | null) => (value ? bedPositionLabels[value] ?? value : '—'),
          },
          {
            title: 'Mã tài sản',
            dataIndex: 'assetCode',
            render: (value: string | null) => value || '—',
          },
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
                    assignmentLabel(bed.occupant.kind)
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
                  <Tooltip title="Sửa giường">
                    <Button
                      size="small"
                      icon={<EditOutlined />}
                      onClick={() => {
                        form.setFieldsValue({
                          code: bed.code,
                          position: bed.position ?? undefined,
                          assetCode: bed.assetCode ?? '',
                          note: bed.note ?? '',
                        })
                        setEditing(bed)
                      }}
                      aria-label="Sửa giường"
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
        forceRender
      >
        <Form
          form={form}
          layout="vertical"
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
          <Form.Item name="position" label="Vị trí giường">
            <Select
              allowClear
              placeholder="Chưa khai"
              options={Object.entries(bedPositionLabels).map(([value, label]) => ({ value, label }))}
            />
          </Form.Item>
          <Form.Item
            name="assetCode"
            label="Mã tài sản"
            rules={[{ max: 30, message: 'Tối đa 30 ký tự' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="note"
            label="Ghi chú"
            rules={[{ max: 500, message: 'Tối đa 500 ký tự' }]}
          >
            <TextArea rows={3} maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
