import { Role } from '../constants/authorization'
import { AppstoreOutlined, ArrowRightOutlined } from '@ant-design/icons'
import { Col, Row } from 'antd'
import { Link } from 'react-router'
import PageHeader from '../components/layout/PageHeader'
import { Alert, Button } from '../components/ui/Surface'
import PageSkeleton from '../components/ui/PageSkeleton'
import { useMe } from '../features/account/useMe'
import { hasAnyPermission } from '../services/api/auth'
import { errorMessage } from '../utils/errorMessages'
import {
  adminNav,
  staffNav,
  studentMoreNav,
  technicianNav,
} from '../constants/navigation'

export default function CrudHomePage() {
  const { data: me, isPending, error, refetch } = useMe()
  const items =
    me?.role === Role.STAFF
      ? staffNav.flatMap((group) => group.items)
      : me?.role === Role.ADMIN
        ? adminNav.flatMap((group) => group.items)
        : me?.role === Role.TECHNICIAN
          ? technicianNav
          : studentMoreNav
  if (isPending && !me)
    return (
      <>
        <PageHeader
          title="Xin chào"
          subtitle="Chọn danh mục nghiệp vụ để tìm kiếm, tra cứu và xử lý dữ liệu"
        />
        <PageSkeleton rows={6} />
      </>
    )
  if (error && !me)
    return (
      <>
        <PageHeader
          title="Xin chào"
          subtitle="Chọn danh mục nghiệp vụ để tìm kiếm, tra cứu và xử lý dữ liệu"
        />
        <Alert
          type="error"
          showIcon
          title={errorMessage(error)}
          action={
            <Button onClick={() => void refetch()}>Thử lại</Button>
          }
        />
      </>
    )
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
              !item.path.endsWith('/dashboard') &&
              hasAnyPermission(me, item.permissions),
          )
          .map((item) => (
            <Col key={item.key} xs={24} sm={12} lg={8}>
              <Link to={item.path} className="dms-nav-card-link">
                <div className="dms-nav-card">
                  <div className="dms-nav-card-body">
                    <div className="dms-nav-card-head">
                      <span className="dms-nav-icon-box">
                        {item.icon ? <item.icon /> : <AppstoreOutlined />}
                      </span>
                      <ArrowRightOutlined
                        style={{
                          color: 'var(--color-text-muted)',
                          fontSize: 16,
                        }}
                      />
                    </div>
                    <h3 className="dms-nav-card-title">{item.label}</h3>
                    <p className="dms-nav-card-caption">Mở màn hình quản lý</p>
                  </div>
                </div>
              </Link>
            </Col>
          ))}
      </Row>
    </>
  )
}
