import { Role } from '../constants/authorization'
import { AppstoreOutlined, ArrowRightOutlined } from '@ant-design/icons'
import { Col, Row } from 'antd'
import { Link } from 'react-router'
import PageHeader from '../components/layout/PageHeader'
import { useMe } from '../features/account/useMe'
import { hasAnyPermission } from '../services/api/auth'
import {
  adminNav,
  staffNav,
  studentMoreNav,
  technicianNav,
} from '../constants/navigation'

export default function CrudHomePage() {
  const { data: me } = useMe()
  const items =
    me?.role === Role.STAFF
      ? staffNav.flatMap((group) => group.items)
      : me?.role === Role.ADMIN
        ? adminNav.flatMap((group) => group.items)
        : me?.role === Role.TECHNICIAN
          ? technicianNav
          : studentMoreNav
  return (
    <>
      <PageHeader
        title={`Xin chào, ${me?.fullName ?? ''}`}
        subtitle="Chọn danh mục nghiệp vụ để tìm kiếm, tra cứu và xử lý dữ liệu"
      />
      <Row gutter={[16, 16]}>
        {items
          .filter(
            (item) =>
              !item.path.endsWith('/dashboard') && hasAnyPermission(me, item.permissions),
          )
          .map((item) => (
            <Col key={item.key} xs={24} sm={12} lg={8}>
              <Link
                to={item.path}
                style={{ display: 'block', height: '100%', textDecoration: 'none' }}
              >
                <div className="dms-nav-card" style={{ padding: '20px' }}>
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      marginBottom: 12,
                    }}
                  >
                    <div className="dms-nav-icon-box">
                      {item.icon ? <item.icon /> : <AppstoreOutlined />}
                    </div>
                    <ArrowRightOutlined style={{ color: '#9CA3AF', fontSize: 16 }} />
                  </div>
                  <h3
                    style={{
                      margin: '0 0 4px',
                      fontSize: 16,
                      fontWeight: 600,
                      color: 'var(--color-heading)',
                    }}
                  >
                    {item.label}
                  </h3>
                  <p
                    style={{
                      margin: 0,
                      fontSize: 13,
                      color: 'var(--color-text-secondary)',
                    }}
                  >
                    Mở màn hình quản lý
                  </p>
                </div>
              </Link>
            </Col>
          ))}
      </Row>
    </>
  )
}
