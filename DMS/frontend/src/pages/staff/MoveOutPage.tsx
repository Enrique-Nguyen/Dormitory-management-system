import { useState } from 'react'
import { useParams } from 'react-router'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  FormField,
  InputNumber,
  PageHeader,
  StatusBadge,
} from '../../components'
import { MoveOutModal } from '../../features/contract'
import {
  AssetCondition,
  type MoveOutResult,
} from '../../services/api/checkout'
import { formatDate, formatMoney } from '../../utils/format'

export interface MoveOutPageProps {
  /** Route có thể truyền thẳng ID hợp đồng; không có thì đọc `:contractId`/`:id` hoặc nhập tay. */
  contractId?: number
  contractCode?: string
}

function ResultCard({ result }: { result: MoveOutResult }) {
  return (
    <Card title="Kết quả trả phòng" style={{ marginTop: 16 }}>
      <Alert
        type="success"
        showIcon
        title={`Đã hoàn tất trả phòng hợp đồng ${result.contractCode}.`}
        description="Hợp đồng, phân phòng, trưởng phòng và hoàn cọc đã được cập nhật theo kết quả máy chủ."
      />
      <Descriptions
        bordered
        size="small"
        column={{ xs: 1, sm: 2 }}
        style={{ marginTop: 16 }}
        items={[
          {
            key: 'contract',
            label: 'Hợp đồng',
            children: result.contractCode,
          },
          {
            key: 'status',
            label: 'Trạng thái',
            children: (
              <StatusBadge entity="contract" status={result.endStatus} />
            ),
          },
          {
            key: 'date',
            label: 'Ngày trả phòng',
            children: formatDate(result.moveOutDate),
          },
          {
            key: 'asset',
            label: 'Tình trạng tài sản',
            children:
              result.assetCondition === AssetCondition.DAMAGED
                ? 'Có hư hỏng'
                : 'Bình thường',
          },
          {
            key: 'assetNote',
            label: 'Ghi chú tài sản',
            children: result.assetNote ?? '—',
          },
          {
            key: 'unpaidRoomFee',
            label: 'Nợ tiền phòng',
            children: formatMoney(result.unpaidRoomFee),
          },
          {
            key: 'deposit',
            label: 'Số dư cọc',
            children: formatMoney(result.depositBalance),
          },
          {
            key: 'refund',
            label: 'Tiền hoàn',
            children: formatMoney(result.refundAmount),
          },
          {
            key: 'debt',
            label: 'Nợ cọc',
            children: formatMoney(result.negativeBalanceDebt),
          },
        ]}
      />
    </Card>
  )
}

/**
 * Trả phòng (UI 22A, API 4.8). Trang nhận ID hợp đồng từ prop hoặc tham số route và mở hộp thoại
 * xem trước ngay; nếu mở từ menu không có ID thì cho nhập mã hợp đồng. Chỉ Staff được khai báo
 * route; component còn kiểm tra quyền MOVE_OUT_CREATE trước khi gửi.
 */
export default function MoveOutPage({
  contractId,
  contractCode,
}: MoveOutPageProps = {}) {
  const params = useParams()
  const routeId = Number(params.contractId ?? params.id)
  const routed =
    (Number.isSafeInteger(contractId) && (contractId ?? 0) > 0
      ? contractId
      : undefined) ??
    (Number.isSafeInteger(routeId) && routeId > 0 ? routeId : undefined)
  const [manualId, setManualId] = useState<number | null>(null)
  const [manualError, setManualError] = useState<string>()
  const [open, setOpen] = useState(true)
  const [result, setResult] = useState<MoveOutResult | null>(null)
  const resolved = routed ?? manualId ?? undefined

  function openWithManualId() {
    if (!manualId) {
      setManualError('Nhập mã hợp đồng đang ACTIVE cần trả phòng.')
      return
    }
    setManualError(undefined)
    setResult(null)
    setOpen(true)
  }

  return (
    <>
      <PageHeader
        title="Trả phòng"
        subtitle="Kiểm tra công nợ, số dư cọc, ticket chưa kết luận và ghi nhận tình trạng tài sản trước khi kết thúc hợp đồng."
        extra={
          resolved &&
          !result && (
            <Button type="primary" onClick={() => setOpen(true)}>
              Bắt đầu trả phòng
            </Button>
          )
        }
      />

      {!routed && (
        <Card title="Chọn hợp đồng cần trả phòng">
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
            title="Chức năng chỉ áp dụng cho hợp đồng đang ACTIVE"
            description="Thông thường mở từ danh sách hoặc chi tiết hợp đồng; nếu mở từ menu, nhập mã hợp đồng bên dưới."
          />
          <FormField label="Mã hợp đồng" required error={manualError}>
            {(control) => (
              <InputNumber
                {...control}
                min={1}
                precision={0}
                style={{ width: '100%', maxWidth: 320 }}
                placeholder="Ví dụ: 1024"
                value={manualId}
                onChange={(value) =>
                  setManualId(typeof value === 'number' ? value : null)
                }
              />
            )}
          </FormField>
          <div style={{ marginTop: 16 }}>
            <Button type="primary" onClick={openWithManualId}>
              Xem trước trả phòng
            </Button>
          </div>
        </Card>
      )}

      {result && <ResultCard result={result} />}

      <MoveOutModal
        open={open && resolved != null}
        contractId={resolved}
        contractCode={contractCode}
        onClose={() => setOpen(false)}
        onSuccess={setResult}
      />
    </>
  )
}
