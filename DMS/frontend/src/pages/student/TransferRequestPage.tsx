import { StopOutlined } from '@ant-design/icons'
import { Result } from 'antd'
import { useParams } from 'react-router'
import {
  ActionBar,
  PageHeader,
  PageSkeleton,
  StatusBadge,
  TransferSummary,
} from '../../components'
import { STUDENT_REQUESTS_PATH } from '../../constants/navigation'
import { useCancelTransfer, useTransferRequest } from '../../features/contract'
import { errorMessage } from '../../utils/errorMessages'

/** Sinh viên xem yêu cầu chuyển phòng của mình và hủy khi còn chờ duyệt (FR-CON-07). */
export default function TransferRequestPage() {
  const id = Number(useParams().id)
  const request = useTransferRequest(id)
  const cancel = useCancelTransfer(id)
  const detail = request.data

  if (request.isLoading) return <PageSkeleton />
  if (!detail)
    return <Result status="404" title={errorMessage(request.error)} />

  return (
    <>
      <PageHeader
        title="Yêu cầu chuyển phòng"
        status={<StatusBadge entity="request" status={detail.status} />}
        breadcrumb={[
          { label: 'Yêu cầu', to: STUDENT_REQUESTS_PATH },
          { label: `Chuyển phòng #${detail.id}` },
        ]}
        extra={
          <ActionBar
            allowedActions={detail.allowedActions}
            actions={{
              cancel: {
                label: 'Hủy yêu cầu',
                danger: true,
                icon: <StopOutlined />,
                loading: cancel.isPending,
                confirm: {
                  title: 'Hủy yêu cầu chuyển phòng?',
                  description:
                    'Bạn giữ nguyên giường hiện tại và có thể gửi yêu cầu mới sau.',
                  okText: 'Hủy yêu cầu',
                },
                onAction: async () => {
                  await cancel.mutateAsync(undefined)
                },
              },
            }}
          />
        }
      />
      <TransferSummary detail={detail} />
    </>
  )
}
