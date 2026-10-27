import { useState } from 'react'
import { Col, Progress, Row, Space, Spin, Statistic } from 'antd'
import { Link } from 'react-router'
import { Alert, Button, Card, PageHeader, StatusBadge } from '../../components'
import { Permission } from '../../constants/authorization'
import { permissionsForPath, staffNav } from '../../constants/navigation'
import { hasAnyPermission, hasPermission } from '../../services/api/auth'
import { useMe } from '../../features/account/useMe'
import { IssueInvoicesModal, useStaffDashboard } from '../../features/billing'
import { formatMoney } from '../../utils/format'

export default function StaffHomePage() {
  const { data: me } = useMe()
  const { data: dashboard, isLoading, error } = useStaffDashboard()
  const [issueOpen, setIssueOpen] = useState(false)
  const capacity = dashboard?.capacity
  const finance = dashboard?.finance
  const operations = dashboard?.operations
  const shortcuts = staffNav.flatMap((group) => group.items)
    .filter((item) => hasAnyPermission(me, permissionsForPath(item.path)))
  return (
    <div style={{ maxWidth: 1400, marginInline: 'auto' }}>
      <PageHeader title={'Bảng điều khiển — ' + (dashboard?.unitName ?? 'Toàn ký túc xá')}
        subtitle={'Xin chào ' + (me?.fullName ?? '')}
        extra={hasPermission(me, Permission.INVOICE_CREATE)
          ? <Button type="primary" onClick={() => setIssueOpen(true)}>Phát hành hóa đơn</Button> : undefined} />
      {isLoading ? <Spin /> : error ? <Alert type="error" showIcon title="Không thể tải dữ liệu Dashboard" /> : dashboard ? (
        <Space orientation="vertical" size={24} style={{ width: '100%' }}>
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} lg={6}>
              <Card><Statistic title="Tỷ lệ lấp đầy giường" value={capacity?.occupancyRate} suffix="%" />
                <Progress percent={capacity?.occupancyRate} showInfo={false} />
                <span>Đang ở: {capacity?.occupiedBeds} · Còn trống: {capacity?.availableBeds}</span>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card><Statistic title="Tổng tiền chưa thanh toán" value={finance?.totalUnpaidAmount} suffix="đ" />
                <span>Tiền phòng: {formatMoney(finance?.unpaidRoomFeeAmount ?? 0)}</span><br />
                <span>Điện nước: {formatMoney(finance?.unpaidUtilityAmount ?? 0)}</span>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card><Statistic title="Tổng tiền cần bù cọc" value={finance?.totalTopupNeededAmount} suffix="đ" /></Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card><Statistic title="Hợp đồng sắp hết hạn" value={operations?.contractsExpiringSoon} />
                <span>Quá hạn trả phòng: {operations?.contractsOverdueMoveout}</span>
              </Card>
            </Col>
          </Row>
          <Row gutter={[16, 16]}>
            <Col xs={24} lg={14}>
              <Card title="Giường và vận hành phòng">
                <Row gutter={[16, 16]}>
                  <Col span={12}><Statistic title="Giường khai thác" value={capacity?.operationalBeds} /></Col>
                  <Col span={12}><Statistic title="Giường trong phòng bảo trì" value={capacity?.maintenanceBeds} /></Col>
                </Row>
                <p>Phòng có người ở chưa có trưởng phòng: <b>{operations?.roomsMissingLeader}</b></p>
              </Card>
            </Col>
            <Col xs={24} lg={10}>
              <Card title="Phiếu sửa chữa theo trạng thái">
                <Space orientation="vertical" style={{ width: '100%' }}>
                  {Object.entries(operations?.ticketsByStatus ?? {}).map(([status, count]) => (
                    <div key={status} style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <StatusBadge entity="ticket" status={status} /><span>{count} phiếu</span>
                    </div>
                  ))}
                  {!Object.keys(operations?.ticketsByStatus ?? {}).length && <span>Chưa có phiếu sửa chữa.</span>}
                </Space>
              </Card>
            </Col>
          </Row>
          <Card title="Lối tắt nghiệp vụ">
            <Row gutter={[16, 16]}>{shortcuts.map((item) => (
              <Col xs={24} sm={12} md={8} key={item.path}>
                <Link to={item.path}>{item.label}</Link>
              </Col>
            ))}</Row>
          </Card>
        </Space>
      ) : null}
      <IssueInvoicesModal open={issueOpen} onClose={() => setIssueOpen(false)} currentUnitId={me?.managementUnit?.id} />
    </div>
  )
}
