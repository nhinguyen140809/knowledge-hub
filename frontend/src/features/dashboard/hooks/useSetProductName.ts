import { useMutation, useQueryClient } from '@tanstack/react-query'
import { setProductName } from '../api/dashboard.api'
import { dashboardKeys } from '../api/dashboard.keys'
import { useActiveConnection } from '@/lib/store/connections.store'
import type { SystemInfo } from '@/shared/types/system.type'

/** Renames the product. The PUT echoes the updated system info, so the response is
 *  written straight into the system-info cache and the runtime panel reflects the
 *  new name without a refetch. Restricted to admins by the backend. */
export function useSetProductName() {
  const active = useActiveConnection()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (productName: string) => setProductName(productName),
    onSuccess: (info: SystemInfo) =>
      queryClient.setQueryData(dashboardKeys.systemInfo(active?.id), info),
  })
}
