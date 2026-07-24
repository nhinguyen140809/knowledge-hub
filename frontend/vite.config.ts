/// <reference types="vitest/config" />
import { readFileSync } from 'node:fs'
import { fileURLToPath, URL } from 'node:url'
import { defineConfig, type Plugin } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

/**
 * Inlines src/lib/theme.init.ts into <head> as a blocking script so the theme
 * class is set before first paint (anti-FOUC). The lib file stays the single
 * source of truth — typed and unit-tested — and must remain annotation-free
 * since it is injected as-is with only the `export` keyword stripped.
 */
function inlineThemeInit(): Plugin {
  return {
    name: 'inline-theme-init',
    transformIndexHtml() {
      const source = readFileSync(
        fileURLToPath(new URL('./src/lib/theme.init.ts', import.meta.url)),
        'utf8',
      )
      const code = source.replace(/^export /m, '') + '\ninitTheme()'
      return [{ tag: 'script', children: code, injectTo: 'head' }]
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss(), inlineThemeInit()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    // Bind 0.0.0.0 so the forwarded port works inside Codespaces / dev containers.
    host: true,
    port: 5173,
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./tests/setup.ts'],
  },
})
