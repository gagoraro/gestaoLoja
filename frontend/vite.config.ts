import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // em dev, /api é repassado ao backend Spring (evita CORS)
    proxy: { '/api': process.env.BACKEND_URL ?? 'http://localhost:8080' },
  },
});
