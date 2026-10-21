import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default {
  plugins: [react()],
  server: {
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
      },
      "/media": {
        target: "http://localhost:8080",
        changeOrigin: true
      }
    }
  }
}
