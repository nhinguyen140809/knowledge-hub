/**
 * Runtime information about a backend — GET /system/info. Shared rather than
 * feature-local: auth uses it to validate a candidate connection, the dashboard
 * renders it in the runtime panel.
 */
export interface SystemInfo {
  application: string
  version: string
  activeProfiles: string[]
}
