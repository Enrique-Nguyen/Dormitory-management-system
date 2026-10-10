import { Navigate } from 'react-router'
import { roleHome } from '../../constants/navigation'
import { useMe } from '../../features/account/useMe'

/** Đặt dưới <RequireRole />: đường dẫn gốc hoặc không tồn tại đưa người đã đăng nhập về trang chủ vai trò của họ. */
export default function HomeRedirect() {
  const { data: me } = useMe()
  return <Navigate to={me ? roleHome[me.role] : '/login'} replace />
}
