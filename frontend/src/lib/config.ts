/**
 * Runtime configuration derived from Vite env vars (see .env). `VITE_API_MODE`
 * flips the whole app between talking to a real backend and serving local mock
 * data — read once here so the rest of the app just checks {@link isMock}.
 */
export const apiMode = import.meta.env.VITE_API_MODE ?? 'real'

export const isMock = apiMode === 'mock'
