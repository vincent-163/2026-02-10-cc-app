import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../server/public',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      '/health': 'http://127.0.0.1:8080',
      '/sessions': 'http://127.0.0.1:8080',
    },
  },
})
