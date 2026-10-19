import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import path from 'path';
import { VitePWA } from 'vite-plugin-pwa';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: 'autoUpdate',
      // Précache : tous les assets statiques (y compris les chunks hachés lazy-loaded)
      workbox: {
        globPatterns: ['**/*.{js,css,html,ico,png,svg,woff,woff2}'],
        // Cache les appels API GET avec strategy NetworkFirst (tentative réseau, fallback cache)
        runtimeCaching: [
          {
            // Cache uniquement les données non sensibles utiles hors ligne (liste employés réduite, organigramme, départements, postes)
            // Les détails collaborateur (/employees/{id}) et historiques sont EXCLUS du cache (données sensibles).
            urlPattern: /^https?:\/\/.*\/api\/v1\/(employees(\?.*)?$|orgchart\/tree|leaves(\?.*)?$|departments|positions)/,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'careertrack-api-cache',
              expiration: {
                maxEntries: 50,
                maxAgeSeconds: 24 * 60 * 60, // 24h
              },
              networkTimeoutSeconds: 5,
            },
          },
        ],
      },
      includeAssets: ['favicon.ico', 'apple-touch-icon.png'],
      manifest: {
        name: 'CareerTrack – Plateforme RH Sénégal',
        short_name: 'CareerTrack',
        description: 'Gestion RH & Carrières pour PME sénégalaise',
        theme_color: '#1A6B3C',
        background_color: '#ffffff',
        display: 'standalone',
        start_url: '/',
        scope: '/',
        lang: 'fr',
        icons: [
          { src: 'pwa-192x192.png', sizes: '192x192', type: 'image/png' },
          { src: 'pwa-512x512.png', sizes: '512x512', type: 'image/png', purpose: 'any maskable' },
        ],
      },
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
  },
});
