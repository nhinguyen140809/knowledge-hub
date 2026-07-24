/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** "real" talks to a live backend; "mock" serves built-in mock data. */
  readonly VITE_API_MODE?: 'real' | 'mock'
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
