import { Toast } from '@heroui/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { type ReactNode, useState } from 'react'
import { ThemeProvider } from '@/shared/components/theme/ThemeProvider'

/**
 * App-wide providers: theme (next-themes) wrapping the TanStack Query client.
 * HeroUI v3 needs no provider of its own. The query client is created once via
 * useState so it survives re-renders but is not shared across tests/SSR.
 *
 * Toast.Provider is a sibling, not a wrapper: its `children` prop is the custom
 * toast render function, so nesting the app inside it renders nothing.
 */
export function Providers({ children }: { children: ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { retry: false, refetchOnWindowFocus: false },
        },
      }),
  )
  return (
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        {children}
        <Toast.Provider />
      </QueryClientProvider>
    </ThemeProvider>
  )
}
