/**
 * Runtime information about a backend — GET /system/info. Shared rather than
 * feature-local: auth uses it to validate a candidate connection, the dashboard
 * renders it in the runtime panel.
 */
export interface SystemInfo {
  /** Human-facing product name, shown to operators and used as the fallback
   *  connection label. Falls back to `application` when not configured. */
  productName: string
  /** Technical service id (the configured application name), not a display
   *  name — kept for reference, not surfaced as the product label. */
  application: string
  version: string
  activeProfiles: string[]
}
