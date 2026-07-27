import type { GrantOrigin, Principal } from '../types/access.type'

/**
 * Grant rules shared by both the principal-centric and source-centric access
 * panels — either side can offer a new direct grant, or list/revoke an
 * existing one.
 */

/** A new direct grant to an admin is dead config (its role already reads
 *  every source); pre-existing grants stay listed and revocable. Gates the
 *  "+ Source"/"+ Principal" action, whichever side offers it. */
export function canReceiveGrants(principal: Principal): boolean {
  return principal.role !== 'ADMIN'
}

/** A source's access can be traced in the graph only when it arrives through a
 *  grant edge. POLICY (default-allow) and ADMIN (role bypass) reach a source
 *  with no edge to follow, so there is nothing to point at. Gates whether a
 *  grant row is clickable-to-trace. */
export function isTraceableOrigin(origin: GrantOrigin): boolean {
  return origin === 'DIRECT' || origin === 'INHERITED'
}

/** Only a DIRECT grant is revocable, whichever side's panel it's shown from;
 *  inherited/admin/policy access has no edge to remove here — it goes away
 *  only by revoking the group grant or changing the default policy. Gates the
 *  revoke button on a grant row. */
export function isRevocableGrant(origin: GrantOrigin): boolean {
  return origin === 'DIRECT'
}
