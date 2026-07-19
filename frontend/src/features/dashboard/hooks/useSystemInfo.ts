import { useQuery } from '@tanstack/react-query'
import { fetchSystemInfo } from '../api/dashboard.api'
import { useActiveConnection } from '@/lib/store/connections.store'
import { dashboardKeys } from '../api/dashboard.keys'

/** Runtime/system info for the active backend. Combines the shared system-info
 *  api call with the dashboard's query key; disabled until a backend is chosen. */
export function useSystemInfo() {
  const active = useActiveConnection()
  return useQuery({
    queryKey: dashboardKeys.systemInfo(active?.id),
    queryFn: fetchSystemInfo,
    enabled: !!active,
  })
}
