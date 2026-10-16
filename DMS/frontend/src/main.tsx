import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import 'dayjs/locale/vi'
import './index.css'
import { initializeTheme } from './app/theme'
import { initializeAuthentication } from './services/api/auth'

async function startApplication() {
  initializeTheme()
  await initializeAuthentication()
  const { default: App } = await import('./app/App')
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <App />
    </StrictMode>,
  )
}

void startApplication().catch(() => {
  const container = document.getElementById('root')!
  container.textContent =
    'Không kết nối được tới máy chủ đăng nhập. Kiểm tra Keycloak đã chạy rồi tải lại trang.'
  const button = document.createElement('button')
  button.textContent = 'Thử lại'
  button.onclick = () => window.location.reload()
  container.appendChild(button)
})
