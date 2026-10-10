import { zodResolver } from '@hookform/resolvers/zod'
import { useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Card, Input, Typography } from 'antd'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { useNavigate } from 'react-router'
import { z } from 'zod'
import FormField from '../components/ui/FormField'
import { roleHome } from '../constants/navigation'
import { meQueryKey, useMe } from '../features/account/useMe'
import { changePassword } from '../services/api/auth'
import { errorMessage } from '../utils/errorMessages'

// Chính sách mật khẩu: API Spec 4.1 (8–64 ký tự, có chữ và số, khác mật khẩu cũ). Backend vẫn kiểm tra lại.
const schema = z
  .object({
    currentPassword: z.string().min(1, 'Vui lòng nhập mật khẩu hiện tại.'),
    newPassword: z
      .string()
      .min(8, 'Mật khẩu mới cần ít nhất 8 ký tự.')
      .max(64, 'Mật khẩu mới tối đa 64 ký tự.')
      .regex(/[A-Za-z]/, 'Mật khẩu mới cần có ít nhất một chữ cái.')
      .regex(/\d/, 'Mật khẩu mới cần có ít nhất một chữ số.'),
    confirmPassword: z.string().min(1, 'Vui lòng nhập lại mật khẩu mới.'),
  })
  .refine((v) => v.newPassword !== v.currentPassword, { path: ['newPassword'], message: 'Mật khẩu mới phải khác mật khẩu cũ.' })
  .refine((v) => v.newPassword === v.confirmPassword, { path: ['confirmPassword'], message: 'Mật khẩu nhập lại chưa khớp.' })
type PasswordForm = z.infer<typeof schema>

/** Đổi mật khẩu: bắt buộc ở lần đăng nhập đầu (FR-ACC-03) và dùng được từ menu người dùng. */
export default function ChangePasswordPage() {
  const { data: me } = useMe()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { message } = App.useApp()
  const [error, setError] = useState<string | null>(null)
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<PasswordForm>({
    resolver: zodResolver(schema),
    defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
  })

  const forced = me?.mustChangePassword ?? false

  const onSubmit = handleSubmit(async ({ currentPassword, newPassword }) => {
    setError(null)
    try {
      await changePassword(currentPassword, newPassword)
      await queryClient.invalidateQueries({ queryKey: meQueryKey })
      message.success('Đã đổi mật khẩu.')
      if (me) navigate(roleHome[me.role], { replace: true })
    } catch (e) {
      setError(errorMessage(e))
    }
  })

  return (
    <main className="dms-auth-page dms-touch">
      <Card>
        <Typography.Title level={3} style={{ marginTop: 0 }}>
          {forced ? 'Đổi mật khẩu lần đầu' : 'Đổi mật khẩu'}
        </Typography.Title>
        {forced && (
          <Alert type="info" showIcon style={{ marginBottom: 16 }} title="Bạn cần đổi mật khẩu mặc định trước khi sử dụng hệ thống." />
        )}
        {error && <Alert type="error" showIcon title={error} role="alert" style={{ marginBottom: 16 }} />}

        <form onSubmit={onSubmit} noValidate>
          <FormField label="Mật khẩu hiện tại" required error={errors.currentPassword?.message}>
            {(c) => (
              <Controller
                name="currentPassword"
                control={control}
                render={({ field }) => <Input.Password {...field} {...c} autoComplete="current-password" />}
              />
            )}
          </FormField>
          <FormField label="Mật khẩu mới" required error={errors.newPassword?.message} hint="8–64 ký tự, gồm cả chữ và số.">
            {(c) => (
              <Controller
                name="newPassword"
                control={control}
                render={({ field }) => <Input.Password {...field} {...c} autoComplete="new-password" />}
              />
            )}
          </FormField>
          <FormField label="Nhập lại mật khẩu mới" required error={errors.confirmPassword?.message}>
            {(c) => (
              <Controller
                name="confirmPassword"
                control={control}
                render={({ field }) => <Input.Password {...field} {...c} autoComplete="new-password" />}
              />
            )}
          </FormField>
          <Button type="primary" htmlType="submit" block loading={isSubmitting}>
            Đổi mật khẩu
          </Button>
          {!forced && me && (
            <Button type="link" block onClick={() => navigate(-1)} style={{ marginTop: 8 }}>
              Quay lại
            </Button>
          )}
        </form>
      </Card>
    </main>
  )
}
