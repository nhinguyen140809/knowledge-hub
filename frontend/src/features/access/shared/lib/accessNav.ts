import type { NavigateFunction } from 'react-router-dom'
import { ROUTES } from '@/shared/constants'

/**
 * Cross-feature entry points into the access pages, each carrying its pick as
 * navigation state (not a URL param) so opening the same principal or source
 * twice still re-selects it — every navigation gets a fresh `location.key`
 * even when the target is identical.
 */

export function navigateToPrincipal(navigate: NavigateFunction, principalId: string): void {
  navigate(ROUTES.accessPrincipals, { state: { selectPrincipal: principalId } })
}

export function navigateToSource(navigate: NavigateFunction, sourceId: string): void {
  navigate(ROUTES.accessSources, { state: { selectSource: sourceId } })
}
