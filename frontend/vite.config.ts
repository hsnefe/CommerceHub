import { defineConfig } from 'vite';

export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api/v1/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/v1/products': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/v1/inventory': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
      '/internal/inventory': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
    },
  },
});
