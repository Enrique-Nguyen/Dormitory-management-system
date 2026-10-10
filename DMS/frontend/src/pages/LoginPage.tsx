import { zodResolver } from '@hookform/resolvers/zod'
import { useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Card, Input, Typography } from 'antd'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { Link, Navigate, useSearchParams } from 'react-router'
import { z } from 'zod'
import FormField from '../components/ui/FormField'
import { CHANGE_PASSWORD_PATH, roleHome } from '../constants/navigation'
import { meQueryKey, useMe } from '../features/account/useMe'
import { login } from '../services/api/auth'
import { getMockMe, mockRole } from '../services/api/mock'
import type { Me } from '../types/auth'
import { errorMessage } from '../utils/errorMessages'

const schema = z.object({
  username: z.string().trim().min(1, 'Vui lòng nhập tên đăng nhập (mã sinh viên).'),
  password: z.string().min(1, 'Vui lòng nhập mật khẩu.'),
})
type LoginForm = z.infer<typeof schema>

/** Chỉ chấp nhận đường dẫn nội bộ để tránh chuyển hướng ra ngoài. */
function safeRedirect(value: string | null): string | null {
  return value && value.startsWith('/') && !value.startsWith('//') && !value.startsWith('/login') ? value : null
}

/** Tài khoản bắt buộc đổi mật khẩu (sinh viên mới import) không vào được trang nào khác (FR-ACC-03). */
function landingPath(me: Me, redirect: string | null): string {
  return me.mustChangePassword ? CHANGE_PASSWORD_PATH : (redirect ?? roleHome[me.role])
}

export default function LoginPage() {
  const [params] = useSearchParams()
  const { data: me } = useMe()
  const queryClient = useQueryClient()
  const [error, setError] = useState<string | null>(null)
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({ resolver: zodResolver(schema), defaultValues: { username: '', password: '' } })

  const redirect = safeRedirect(params.get('redirect'))
  const expired = params.get('reason') === 'expired'

  const onSubmit = handleSubmit(async ({ username, password }) => {
    setError(null)
    try {
      // Có `me` trong cache thì <Navigate> bên dưới chuyển trang; cùng một đường cho đăng nhập mới và phiên còn hiệu lực
      queryClient.setQueryData(meQueryKey, await login(username.trim(), password))
    } catch (e) {
      // Không phân biệt sai tên hay sai mật khẩu; thông báo khóa tạm lấy theo errorCode
      setError(errorMessage(e))
    }
  })

  // Đã có phiên (mở /login khi còn đăng nhập) thì vào thẳng. Chế độ mock luôn có `me` nên giữ trang để xem được form.
  if (me && !mockRole) return <Navigate to={landingPath(me, redirect)} replace />

  return (
    <main className="dms-auth-page dms-touch">
      <Card>
        <Typography.Title level={3} style={{ marginTop: 0, color: '#1D4ED8' }}>
          DMS
        </Typography.Title>
        <Typography.Paragraph type="secondary">Hệ thống quản lý ký túc xá Đại học Thủy Lợi</Typography.Paragraph>

        {expired && <Alert type="warning" showIcon title="Phiên đã hết hạn. Vui lòng đăng nhập lại." style={{ marginBottom: 16 }} />}
        {mockRole && (
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
            title={`Chế độ mock (VITE_MOCK_ROLE=${mockRole})`}
            description={<Link to={roleHome[getMockMe().role]}>Vào cổng của vai trò này</Link>}
          />
        )}
        {error && <Alert type="error" showIcon title={error} role="alert" style={{ marginBottom: 16 }} />}

        <form onSubmit={onSubmit} noValidate>
          <FormField label="Tên đăng nhập" required error={errors.username?.message}>
            {(control_) => (
              <Controller
                name="username"
                control={control}
                render={({ field }) => <Input {...field} {...control_} autoComplete="username" autoFocus placeholder="Mã sinh viên" />}
              />
            )}
          </FormField>
          <FormField label="Mật khẩu" required error={errors.password?.message}>
            {(control_) => (
              <Controller
                name="password"
                control={control}
                render={({ field }) => <Input.Password {...field} {...control_} autoComplete="current-password" />}
              />
            )}
          </FormField>
          <Button type="primary" htmlType="submit" block loading={isSubmitting}>
            Đăng nhập
          </Button>
        </form>
      </Card>
    </main>
  )
}
