import { Card, Col, Row, Typography } from 'antd'
import { Link } from 'react-router'
import PageHeader from '../components/layout/PageHeader'
import { useMe } from '../features/account/useMe'
import {
  adminNav,
  staffNav,
  studentMoreNav,
  technicianNav,
} from '../constants/navigation'

export default function CrudHomePage() {
  const { data: me } = useMe()
  const items =
    me?.role === 'STAFF'
      ? staffNav.flatMap((group) => group.items)
      : me?.role === 'ADMIN'
        ? adminNav.flatMap((group) => group.items)
        : me?.role === 'TECHNICIAN'
          ? technicianNav
          : studentMoreNav
  return (
    <>
      <PageHeader
        title={`Xin chào, ${me?.fullName ?? ''}`}
        subtitle="Chọn danh mục để tìm kiếm và quản lý dữ liệu"
      />
      <Row gutter={[16, 16]}>
        {items
          .filter((item) => !item.path.endsWith('/dashboard'))
          .map((item) => (
            <Col key={item.key} xs={24} sm={12} lg={8}>
              <Link to={item.path}>
                <Card hoverable>
                  <Typography.Title level={5}>{item.label}</Typography.Title>
                  <Typography.Text type="secondary">
                    Mở màn hình quản lý
                  </Typography.Text>
                </Card>
              </Link>
            </Col>
          ))}
      </Row>
    </>
  )
}
