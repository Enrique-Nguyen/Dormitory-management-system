import { Alert, Button } from '../components/ui/Surface'
import { Spin, Typography } from 'antd'
import { useState } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router'
import { useMe } from '../features/account/useMe'
import {
  keycloak,
  loginWithKeycloak,
  logout,
  loginLandingPath,
} from '../services/api/auth'
import { errorMessage } from '../utils/errorMessages'

/** Mật khẩu chỉ nhập tại Keycloak; DMS không nhận mật khẩu qua form/API đăng nhập. */
export default function LoginPage() {
  const [params] = useSearchParams()
  const { data: me, error, isPending } = useMe()
  const [busy, setBusy] = useState(false)
  const [loginError, setLoginError] = useState<string | null>(null)
  const redirect = params.get('redirect')

  if (me && !error)
    return <Navigate to={loginLandingPath(me, redirect)} replace />

  async function signIn() {
    setBusy(true)
    try {
      const next =
        redirect &&
        /^\/(student|staff|technician|admin)(\/|\?|$)/.test(redirect)
          ? `/login?redirect=${encodeURIComponent(redirect)}`
          : '/login'
      await loginWithKeycloak(next)
    } catch (failure) {
      setLoginError(errorMessage(failure))
      setBusy(false)
    }
  }

  return (
    <main className="dms-auth-page dms-touch">
      <div className="dms-auth-card">
        <div className="dms-auth-header">
          <div className="dms-auth-logo-badge">DMS</div>
          <h1 className="dms-auth-title">Đăng nhập hệ thống</h1>
          <p className="dms-auth-subtitle">
            Hệ thống Quản lý Ký túc xá · Đại học Thủy Lợi
          </p>
        </div>
        <p>Bấm đăng nhập để tiếp tục tới trang xác thực của hệ thống.</p>
        {isPending && <Spin style={{ display: 'block', marginBottom: 16 }} />}
        {(loginError || (error && keycloak.authenticated)) && (
          <Alert
            showIcon
            type="error"
            style={{ marginBottom: 16 }}
            title={loginError || errorMessage(error)}
          />
        )}
        {params.get('reason') === 'expired' && (
          <Alert
            showIcon
            type="warning"
            title="Phiên đã hết hạn. Vui lòng đăng nhập lại."
            style={{ marginBottom: 16 }}
          />
        )}
        <Button
          type="primary"
          block
          loading={busy}
          onClick={() => void signIn()}
        >
          Đăng nhập
        </Button>
        {keycloak.authenticated && error && (
          <Button block style={{ marginTop: 12 }} onClick={() => void logout()}>
            Đăng xuất để dùng tài khoản khác
          </Button>
        )}
        <div style={{ marginTop: 16, textAlign: 'center' }}>
          <Link to="/forgot-password">Quên mật khẩu?</Link>
        </div>
        <div className="dms-auth-footer">
          Ký túc xá Đại học Thủy Lợi · Hỗ trợ kỹ thuật
        </div>
      </div>
    </main>
  )
}

/** Quên mật khẩu: người quản lý xác minh và reset; đặt cùng trang đăng nhập để giảm file. */
export function ForgotPasswordPage() {
  return (
    <main className="dms-auth-page dms-touch">
      <div className="dms-auth-card">
        <div className="dms-auth-header">
          <div className="dms-auth-logo-badge">DMS</div>
          <h1 className="dms-auth-title">Quên mật khẩu</h1>
          <p className="dms-auth-subtitle">
            Hệ thống Quản lý Ký túc xá · Đại học Thủy Lợi
          </p>
        </div>
        <Typography.Paragraph
          style={{
            color: 'var(--color-text-secondary)',
            fontSize: 13,
            marginBottom: 12,
          }}
        >
          Liên hệ Ban quản lý ký túc xá hoặc quản trị viên để xác minh tài khoản
          và nhận mật khẩu tạm thời.
        </Typography.Paragraph>
        <Typography.Paragraph
          style={{
            color: 'var(--color-text-secondary)',
            fontSize: 13,
            marginBottom: 16,
          }}
        >
          Sinh viên vui lòng cung cấp mã sinh viên cho cán bộ quản lý tòa
          nhà/đơn vị. Tài khoản cán bộ, kỹ thuật viên và quản trị viên liên hệ
          ban quản trị.
        </Typography.Paragraph>
        <Alert
          type="info"
          showIcon
          title="Quy trình cấp lại mật khẩu"
          description="Mật khẩu tạm sẽ được cấp sau khi xác minh nhân thân. Khi đăng nhập bằng mật khẩu tạm, hệ thống sẽ yêu cầu bạn đổi mật khẩu mới."
          style={{ marginBottom: 20 }}
        />
        <Link to="/login">
          <Button type="primary" block>
            Quay lại đăng nhập
          </Button>
        </Link>
        <div className="dms-auth-footer">
          <span>Ký túc xá Đại học Thủy Lợi · Hỗ trợ kỹ thuật</span>
        </div>
      </div>
    </main>
  )
}
