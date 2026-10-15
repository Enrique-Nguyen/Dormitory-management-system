import { Navigate } from 'react-router'
import { useMe } from '../../features/account/useMe'
import { accessibleHomePath } from '../../services/api/auth'

/** Đặt dưới <RequireRole />: đường dẫn gốc hoặc không tồn tại đưa người đã đăng nhập về trang chủ vai trò của họ. */
export default function HomeRedirect() {
  const { data: me } = useMe()
  return <Navigate to={me ? accessibleHomePath(me) : '/login'} replace />
}
