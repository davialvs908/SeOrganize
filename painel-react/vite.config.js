import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Em produção o build vai direto para o Spring Boot servir em /painel.
// No modo "mock" (demo) os dados são simulados e o build fica em dist-demo.
export default defineConfig(({ mode }) => ({
  plugins: [react()],
  base: mode === 'mock' ? './' : '/painel/',
  build: {
    outDir: mode === 'mock'
      ? 'dist-demo'
      : '../Sla_Reuniao/Sla_Reuniao/src/main/resources/static/painel',
    emptyOutDir: true,
  },
  server: {
    // npm run dev: o Vite repassa /api para o Spring rodando em :8080
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
    globals: true,
  },
}))
