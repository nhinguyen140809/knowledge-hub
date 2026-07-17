/** Query-key factory for the dashboard's own queries. The system-info read is a
 *  dashboard concern (the runtime panel), so its key lives here. */
export const dashboardKeys = {
  all: ['dashboard'] as const,
  systemInfo: (connectionId: string | undefined) =>
    [...dashboardKeys.all, 'system-info', connectionId] as const,
}
