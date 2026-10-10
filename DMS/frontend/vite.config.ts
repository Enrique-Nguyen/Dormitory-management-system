import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Khi phát triển, /api chuyển sang Spring Boot để cookie phiên dùng chung origin (SAD mục 10)
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
