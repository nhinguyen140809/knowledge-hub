import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { type ReactNode, useState } from 'react'
import { ThemeProvider } from '@/shared/components/theme/ThemeProvider'
import { Toast } from '@heroui/react'

/**
 * App-wide providers: theme (next-themes) wrapping the TanStack Query client.
 * HeroUI v3 needs no provider of its own. The query client is created once via
 * useState so it survives re-renders but is not shared across tests/SSR.
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
      <Toast.Provider>
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
      </Toast.Provider>
    </ThemeProvider>
  )
}
