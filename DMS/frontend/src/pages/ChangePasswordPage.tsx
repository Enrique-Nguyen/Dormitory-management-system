import { Button } from '../components/ui/Surface'
import { Alert } from 'antd'
import { useState } from 'react'
import { changeKeycloakPassword } from '../services/api/auth'
import { errorMessage } from '../utils/errorMessages'

export default function ChangePasswordPage() {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function changePassword() {
    setBusy(true)
    try {
      await changeKeycloakPassword()
    } catch (failure) {
      setError(errorMessage(failure))
      setBusy(false)
    }
  }

  return (
    <main className="dms-auth-page dms-touch">
      <div className="dms-auth-card">
        <div className="dms-auth-header">
          <div className="dms-auth-logo-badge">DMS</div>
          <h1 className="dms-auth-title">Đổi mật khẩu</h1>
          <p className="dms-auth-subtitle">
            Hệ thống Quản lý Ký túc xá · Đại học Thủy Lợi
          </p>
        </div>
        <p className="dms-auth-note">
          Bạn sẽ được chuyển tới trang bảo mật để đổi mật khẩu đăng nhập.
        </p>
        {error && (
          <Alert
            type="error"
            showIcon
            title={error}
            style={{ marginBottom: 16 }}
          />
        )}
        <Button
          type="primary"
          block
          loading={busy}
          onClick={() => void changePassword()}
        >
          Tiếp tục đổi mật khẩu
        </Button>
        <div className="dms-auth-footer">
          Ký túc xá Đại học Thủy Lợi · Hỗ trợ kỹ thuật
        </div>
      </div>
    </main>
  )
}
