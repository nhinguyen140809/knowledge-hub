/** Every route path in one place: a rename (or the /access → /access/principals
 *  split) becomes a one-file change instead of hunting down every literal
 *  scattered across the router, the sidebar, and navigate() calls. */
export const ROUTES = {
  dashboard: '/',
  connect: '/connect',
  sources: '/sources',
  sourceDetail: (id: string) => `/sources/${encodeURIComponent(id)}`,
  accessPrincipals: '/access/principals',
  accessSources: '/access/sources',
  query: '/query',
  help: '/help',
} as const
