import { useState } from 'react'
import {
  Row,
  Col,
  Card,
  Statistic,
  Progress,
  Button,
  Tag,
  Space,
  Spin,
  Alert,
} from 'antd'
import {
  HomeOutlined,
  DollarOutlined,
  ToolOutlined,
  UsergroupAddOutlined,
  ArrowRightOutlined,
  ThunderboltOutlined,
  CheckCircleOutlined,
  AuditOutlined,
  SafetyCertificateOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import { Link } from 'react-router'
import PageHeader from '../../components/layout/PageHeader'
import { useMe } from '../../features/account/useMe'
import { useStaffDashboard, IssueInvoicesModal } from '../../features/billing'

export default function StaffHomePage() {
  const { data: me } = useMe()
  const { data: dashboard, isLoading, error } = useStaffDashboard()
  const [issueModalOpen, setIssueModalOpen] = useState(false)

  const capacity = dashboard?.capacity
  const financial = dashboard?.financial
  const operations = dashboard?.operations

  return (
    <div style={{ maxWidth: 1400, margin: '0 auto' }}>
      <PageHeader
        title={`Bảng điều khiển BQL Ký túc xá - ${dashboard?.managementUnitName ?? 'Đơn vị quản lý'}`}
        subtitle={`Xin chào ${me?.fullName ?? ''}, theo dõi công suất phòng, công nợ hóa đơn và vận hành trực quan trong ca trực`}
        extra={
          <Button
            type="primary"
            icon={<ThunderboltOutlined />}
            size="large"
            onClick={() => setIssueModalOpen(true)}
            style={{
              background: 'linear-gradient(135deg, #1890ff 0%, #096dd9 100%)',
              border: 'none',
              boxShadow: '0 4px 12px rgba(24, 144, 255, 0.35)',
            }}
          >
            Phát hành hóa đơn
          </Button>
        }
      />

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '60px 0' }}>
          <Spin size="large" />
          <p style={{ marginTop: 16, color: 'var(--color-text-secondary)' }}>
            Đang tải dữ liệu tổng quan ký túc xá...
          </p>
        </div>
      ) : error ? (
        <Alert
          message="Không thể tải dữ liệu Dashboard"
          description="Vui lòng kiểm tra quyền truy cập hoặc thử tải lại trang."
          type="error"
          showIcon
          style={{ marginBottom: 24 }}
        />
      ) : (
        <Space direction="vertical" size={24} style={{ width: '100%' }}>
          {/* Section 1: KPI Stats Overview */}
          <Row gutter={[16, 16]}>
            {/* Widget 1: Công suất phòng */}
            <Col xs={24} sm={12} lg={6}>
              <Card
                bordered={false}
                style={{
                  borderRadius: 12,
                  boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
                  height: '100%',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div>
                    <span style={{ fontSize: 13, color: 'var(--color-text-secondary)' }}>
                      Tỷ lệ lấp đầy giường
                    </span>
                    <div style={{ fontSize: 28, fontWeight: 700, margin: '6px 0 2px', color: '#1890ff' }}>
                      {capacity?.occupancyRate ?? 0}%
                    </div>
                  </div>
                  <div
                    style={{
                      width: 44,
                      height: 44,
                      borderRadius: 10,
                      background: 'rgba(24, 144, 255, 0.1)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#1890ff',
                      fontSize: 20,
                    }}
                  >
                    <HomeOutlined />
                  </div>
                </div>
                <Progress
                  percent={capacity?.occupancyRate ?? 0}
                  showInfo={false}
                  strokeColor={{ '0%': '#1890ff', '100%': '#52c41a' }}
                  size="small"
                />
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginTop: 8, color: 'var(--color-text-muted)' }}>
                  <span>Đang ở: <b>{capacity?.occupiedBeds ?? 0}</b></span>
                  <span>Giữ chỗ: <b>{capacity?.heldBeds ?? 0}</b></span>
                  <span>Còn trống: <b style={{ color: '#52c41a' }}>{capacity?.freeBeds ?? 0}</b></span>
                </div>
              </Card>
            </Col>

            {/* Widget 2: Hóa đơn chưa thanh toán */}
            <Col xs={24} sm={12} lg={6}>
              <Card
                bordered={false}
                style={{
                  borderRadius: 12,
                  boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
                  height: '100%',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div>
                    <span style={{ fontSize: 13, color: 'var(--color-text-secondary)' }}>
                      Hóa đơn chưa nộp
                    </span>
                    <div style={{ fontSize: 28, fontWeight: 700, margin: '6px 0 2px', color: '#f5222d' }}>
                      {financial?.unpaidInvoicesCount ?? 0}
                    </div>
                  </div>
                  <div
                    style={{
                      width: 44,
                      height: 44,
                      borderRadius: 10,
                      background: 'rgba(245, 34, 45, 0.1)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#f5222d',
                      fontSize: 20,
                    }}
                  >
                    <DollarOutlined />
                  </div>
                </div>
                <div style={{ fontSize: 13, color: 'var(--color-text-secondary)', marginTop: 8 }}>
                  Tổng nợ đọng:{' '}
                  <b style={{ color: '#f5222d', fontWeight: 600 }}>
                    {(financial?.unpaidInvoicesAmount ?? 0).toLocaleString('vi-VN')} đ
                  </b>
                </div>
                <div style={{ marginTop: 8 }}>
                  <Link to="/staff/invoices" style={{ fontSize: 12, color: '#1890ff' }}>
                    Xem chi tiết công nợ <ArrowRightOutlined />
                  </Link>
                </div>
              </Card>
            </Col>

            {/* Widget 3: Cảnh báo nộp bù cọc */}
            <Col xs={24} sm={12} lg={6}>
              <Card
                bordered={false}
                style={{
                  borderRadius: 12,
                  boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
                  height: '100%',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div>
                    <span style={{ fontSize: 13, color: 'var(--color-text-secondary)' }}>
                      Cần nộp bù cọc
                    </span>
                    <div style={{ fontSize: 28, fontWeight: 700, margin: '6px 0 2px', color: '#fa8c16' }}>
                      {financial?.topupNeededCount ?? 0}
                    </div>
                  </div>
                  <div
                    style={{
                      width: 44,
                      height: 44,
                      borderRadius: 10,
                      background: 'rgba(250, 140, 22, 0.1)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#fa8c16',
                      fontSize: 20,
                    }}
                  >
                    <SafetyCertificateOutlined />
                  </div>
                </div>
                <div style={{ fontSize: 13, color: 'var(--color-text-secondary)', marginTop: 8 }}>
                  Khấu trừ đền bù hư hại vật tư
                </div>
                <div style={{ marginTop: 8 }}>
                  <Link to="/staff/deposits" style={{ fontSize: 12, color: '#fa8c16' }}>
                    Quản lý sổ tiền cọc <ArrowRightOutlined />
                  </Link>
                </div>
              </Card>
            </Col>

            {/* Widget 4: Phiếu sửa chữa mở */}
            <Col xs={24} sm={12} lg={6}>
              <Card
                bordered={false}
                style={{
                  borderRadius: 12,
                  boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
                  height: '100%',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div>
                    <span style={{ fontSize: 13, color: 'var(--color-text-secondary)' }}>
                      Báo hỏng chưa xong
                    </span>
                    <div style={{ fontSize: 28, fontWeight: 700, margin: '6px 0 2px', color: '#722ed1' }}>
                      {operations?.pendingTicketsCount ?? 0}
                    </div>
                  </div>
                  <div
                    style={{
                      width: 44,
                      height: 44,
                      borderRadius: 10,
                      background: 'rgba(114, 46, 209, 0.1)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#722ed1',
                      fontSize: 20,
                    }}
                  >
                    <ToolOutlined />
                  </div>
                </div>
                <div style={{ fontSize: 13, color: 'var(--color-text-secondary)', marginTop: 8 }}>
                  Hợp đồng sắp hết hạn: <b>{operations?.expiringContractsCount ?? 0}</b>
                </div>
                <div style={{ marginTop: 8 }}>
                  <Link to="/staff/maintenance" style={{ fontSize: 12, color: '#722ed1' }}>
                    Điều phối sửa chữa <ArrowRightOutlined />
                  </Link>
                </div>
              </Card>
            </Col>
          </Row>

          {/* Section 2: Detailed Operations & Room Status */}
          <Row gutter={[16, 16]}>
            {/* Phân bổ giường chi tiết */}
            <Col xs={24} lg={14}>
              <Card
                title={
                  <Space>
                    <HomeOutlined style={{ color: '#1890ff' }} />
                    <span>Hiện trạng giường & cơ sở vật chất</span>
                  </Space>
                }
                bordered={false}
                style={{ borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}
              >
                <Row gutter={[16, 16]}>
                  <Col xs={12} sm={6}>
                    <Statistic title="Tổng số giường" value={capacity?.totalBeds ?? 0} />
                  </Col>
                  <Col xs={12} sm={6}>
                    <Statistic
                      title="Đang ở (STAY)"
                      value={capacity?.occupiedBeds ?? 0}
                      valueStyle={{ color: '#1890ff' }}
                    />
                  </Col>
                  <Col xs={12} sm={6}>
                    <Statistic
                      title="Đang giữ chỗ (HOLD)"
                      value={capacity?.heldBeds ?? 0}
                      valueStyle={{ color: '#faad14' }}
                    />
                  </Col>
                  <Col xs={12} sm={6}>
                    <Statistic
                      title="Giường sẵn sàng"
                      value={capacity?.freeBeds ?? 0}
                      valueStyle={{ color: '#52c41a' }}
                    />
                  </Col>
                </Row>

                <div
                  style={{
                    marginTop: 20,
                    padding: 14,
                    borderRadius: 8,
                    background: 'var(--color-bg-subtle, #f8fafc)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <WarningOutlined style={{ color: '#fa8c16', fontSize: 18 }} />
                    <div>
                      <div style={{ fontWeight: 600 }}>Phòng chưa có trưởng phòng (BR-11)</div>
                      <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>
                        Hiện có <b>{operations?.roomsMissingLeader ?? 0}</b> phòng đang có sinh viên ở nhưng chưa chỉ định trưởng phòng.
                      </div>
                    </div>
                  </div>
                  <Link to="/staff/rooms">
                    <Button size="small">Gán trưởng phòng</Button>
                  </Link>
                </div>
              </Card>
            </Col>

            {/* Phân loại sự cố kỹ thuật */}
            <Col xs={24} lg={10}>
              <Card
                title={
                  <Space>
                    <ToolOutlined style={{ color: '#722ed1' }} />
                    <span>Sự cố kỹ thuật theo phân loại</span>
                  </Space>
                }
                bordered={false}
                style={{ borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}
              >
                <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                  {operations?.openTicketsByCategory && Object.keys(operations.openTicketsByCategory).length > 0 ? (
                    Object.entries(operations.openTicketsByCategory).map(([cat, count]) => {
                      const labels: Record<string, string> = {
                        ELECTRIC: 'Sự cố Điện',
                        WATER: 'Sự cố Nước',
                        FURNITURE: 'Bàn ghế / Giường tủ',
                        DOOR_LOCK: 'Khóa cửa / Cửa',
                        OTHER: 'Khác',
                      }
                      return (
                        <div
                          key={cat}
                          style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            padding: '8px 12px',
                            borderRadius: 6,
                            background: 'var(--color-bg-subtle, #f8fafc)',
                          }}
                        >
                          <span>{labels[cat] || cat}</span>
                          <Tag color="purple" style={{ fontWeight: 600 }}>
                            {count} phiếu
                          </Tag>
                        </div>
                      )
                    })
                  ) : (
                    <div style={{ textAlign: 'center', padding: '24px 0', color: 'var(--color-text-muted)' }}>
                      <CheckCircleOutlined style={{ fontSize: 24, color: '#52c41a', marginBottom: 8 }} />
                      <div>Không có sự cố nào đang chờ xử lý</div>
                    </div>
                  )}
                </div>
              </Card>
            </Col>
          </Row>

          {/* Section 3: Quick Navigation Modules */}
          <Card
            title="Lối tắt phân hệ nghiệp vụ"
            bordered={false}
            style={{ borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}
          >
            <Row gutter={[16, 16]}>
              {[
                {
                  title: 'Quản lý Hóa đơn & Thu phí',
                  desc: 'Tra cứu hóa đơn, thu tiền điện nước & tiền phòng',
                  path: '/staff/invoices',
                  icon: DollarOutlined,
                  color: '#1890ff',
                },
                {
                  title: 'Sổ tiền cọc & Bù cọc',
                  desc: 'Theo dõi biến động sổ cọc, bù cọc vi phạm & hoàn cọc',
                  path: '/staff/deposits',
                  icon: SafetyCertificateOutlined,
                  color: '#faad14',
                },
                {
                  title: 'Chỉ số Điện & Nước',
                  desc: 'Nhập chốt chỉ số công tơ điện nước hàng tháng',
                  path: '/staff/utilities',
                  icon: ThunderboltOutlined,
                  color: '#52c41a',
                },
                {
                  title: 'Sơ đồ Phòng & Trưởng phòng',
                  desc: 'Chi tiết từng phòng, chỉ định trưởng phòng & xem giường',
                  path: '/staff/rooms',
                  icon: HomeOutlined,
                  color: '#722ed1',
                },
                {
                  title: 'Hợp đồng lưu trú',
                  desc: 'Quản lý hợp đồng ký túc xá, kích hoạt & gia hạn',
                  path: '/staff/contracts',
                  icon: AuditOutlined,
                  color: '#13c2c2',
                },
                {
                  title: 'Xét duyệt đơn đăng ký',
                  desc: 'Duyệt đơn đăng ký chỗ ở, xếp giường đợt chính',
                  path: '/staff/applications',
                  icon: UsergroupAddOutlined,
                  color: '#eb2f96',
                },
              ].map((item) => (
                <Col key={item.path} xs={24} sm={12} md={8}>
                  <Link to={item.path} style={{ textDecoration: 'none', display: 'block' }}>
                    <div
                      className="dms-nav-card"
                      style={{
                        padding: 16,
                        border: '1px solid var(--color-border-subtle, #f0f0f0)',
                        borderRadius: 10,
                        transition: 'all 0.25s ease',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
                        <div
                          style={{
                            width: 36,
                            height: 36,
                            borderRadius: 8,
                            background: `${item.color}15`,
                            color: item.color,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontSize: 18,
                          }}
                        >
                          <item.icon />
                        </div>
                        <h4 style={{ margin: 0, fontSize: 14, fontWeight: 600 }}>{item.title}</h4>
                      </div>
                      <p style={{ margin: 0, fontSize: 12, color: 'var(--color-text-secondary)' }}>
                        {item.desc}
                      </p>
                    </div>
                  </Link>
                </Col>
              ))}
            </Row>
          </Card>
        </Space>
      )}

      {/* Modal phát hành hóa đơn */}
      <IssueInvoicesModal
        open={issueModalOpen}
        onClose={() => setIssueModalOpen(false)}
        currentUnitId={dashboard?.managementUnitId}
      />
    </div>
  )
}
