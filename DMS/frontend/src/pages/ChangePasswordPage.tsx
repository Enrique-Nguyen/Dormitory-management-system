import { Alert, Button, Card } from '../components/ui/Surface'
import { Typography } from 'antd'
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
      <Card>
        <Typography.Title level={3}>Đổi mật khẩu</Typography.Title>
        <Typography.Paragraph>
          Bạn sẽ được chuyển tới trang bảo mật để đổi mật khẩu đăng nhập.
        </Typography.Paragraph>
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
      </Card>
    </main>
  )
}
