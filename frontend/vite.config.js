import react from '@vitejs/plugin-react'
import process from 'node:process'

// https://vite.dev/config/
export default {
  plugins: [react()],
  server: {
    proxy: {
      "/api": {
        target: process.env.E2E_API_TARGET || "http://localhost:8080",
        changeOrigin: true
      },
      "/media": {
        target: process.env.E2E_API_TARGET || "http://localhost:8080",
        changeOrigin: true
      }
    }
  }
}
