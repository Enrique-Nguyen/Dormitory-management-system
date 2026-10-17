import { Flex, Typography } from 'antd'
import { useState } from 'react'
import { assignmentLabel } from '../../constants/statusConfig'
import { Radio, RadioGroup } from '../ui/FormControls'
import { Modal } from '../ui/Surface'

export interface LeaderCandidate {
  studentId: number
  fullName: string
  studentCode: string
  kind: 'HOLD' | 'STAY'
}

interface Props {
  roomCode: string
  open: boolean
  occupants: LeaderCandidate[]
  currentLeaderId?: number | null
  loading?: boolean
  /** Throw để giữ dialog mở khi API báo lỗi. */
  onConfirm: (studentId: number) => Promise<unknown> | void
  onClose: () => void
}

/**
 * Dialog chỉ định trưởng phòng (UI 15, FR-ROOM-09, BR-11): chỉ sinh viên đang ở mới chọn được,
 * người đang giữ chỗ hiện mờ. Backend vẫn kiểm tra lại điều kiện.
 */
export default function LeaderDialog({
  roomCode,
  open,
  occupants,
  currentLeaderId,
  loading,
  onConfirm,
  onClose,
}: Props) {
  const [selected, setSelected] = useState<number | undefined>()
  const value = selected ?? currentLeaderId ?? undefined
  const hasHolders = occupants.some((occupant) => occupant.kind !== 'STAY')

  return (
    <Modal
      title={`Chỉ định trưởng phòng ${roomCode}`}
      open={open}
      okText="Xác nhận"
      cancelText="Hủy"
      confirmLoading={loading}
      okButtonProps={{ disabled: value == null || value === currentLeaderId }}
      onCancel={() => {
        setSelected(undefined)
        onClose()
      }}
      onOk={async () => {
        if (value == null) return
        try {
          await onConfirm(value)
          setSelected(undefined)
          onClose()
        } catch {
          // Lỗi đã được báo; giữ dialog để chọn lại.
        }
      }}
    >
      {occupants.length === 0 ? (
        <Typography.Text type="secondary">
          Phòng chưa có sinh viên.
        </Typography.Text>
      ) : (
        <RadioGroup
          value={value}
          onChange={(event) => setSelected(Number(event.target.value))}
        >
          <Flex vertical gap={10}>
            {occupants.map((occupant) => (
              <Radio
                key={occupant.studentId}
                value={occupant.studentId}
                disabled={occupant.kind !== 'STAY'}
              >
                {occupant.fullName} · {occupant.studentCode} ·{' '}
                {assignmentLabel(occupant.kind)}
              </Radio>
            ))}
          </Flex>
        </RadioGroup>
      )}
      {hasHolders && (
        <Typography.Paragraph
          type="secondary"
          style={{ marginTop: 12, marginBottom: 0 }}
        >
          Chỉ sinh viên đang ở mới được chỉ định.
        </Typography.Paragraph>
      )}
    </Modal>
  )
}
