import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/v1/bookings':      { target: 'http://localhost:8081', changeOrigin: true },
      '/api/v1/payments':      { target: 'http://localhost:8082', changeOrigin: true },
      '/api/v1/notifications': { target: 'http://localhost:8083', changeOrigin: true },
    },
  },
})
