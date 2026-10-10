import { QueryClientProvider } from '@tanstack/react-query'
import { App as AntApp, ConfigProvider } from 'antd'
import viVN from 'antd/locale/vi_VN'
import dayjs from 'dayjs'
import { RouterProvider } from 'react-router'
import { queryClient } from './queryClient'
import { router } from './router'
import { theme } from './theme'

dayjs.locale('vi')

export default function App() {
  return (
    <ConfigProvider theme={theme} locale={viVN}>
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <RouterProvider router={router} />
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>
  )
}
