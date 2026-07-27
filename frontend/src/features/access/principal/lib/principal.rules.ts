import type { Principal, PrincipalType, Role } from '../../shared/types/access.type'
import type { Credential } from '../types/principal.type'

/**
 * The access-control business rules in one place. Each export answers one "is
 * this action allowed?" question; components consume these instead of
 * re-deriving domain logic inline, so a rule change lands in exactly one file.
 * Every rule is re-validated on submit anyway — these gates only spare a round
 * trip and keep the UI from offering actions that would be rejected.
 * Grant-eligibility rules (revocable, traceable, receivable) live in the
 * shared `grant.rules` instead — both sides' panels need those.
 */

/** Role ADMIN is SUBJECT-only: roles don't inherit through membership, so a
 *  GROUP with role ADMIN would confer admin on nobody. */
export function canBeAdmin(type: PrincipalType): boolean {
  return type === 'SUBJECT'
}

/** Admins stay out of the membership graph — their access is already total by
 *  role, so membership edges would only draw misleading "inherited" access.
 *  Gates add-to-group and move-to-group. */
export function canJoinGroup(principal: Principal): boolean {
  return principal.role !== 'ADMIN'
}

/** Only groups hold members. Gates the "Add member" action and makes every
 *  dialog's target-group list. */
export function canHaveMembers(principal: Principal): boolean {
  return principal.type === 'GROUP'
}

/** The only legal role for a principal born inside a group: ADMIN can't live
 *  in a group, and a GROUP can't be ADMIN — the intersection leaves MEMBER. */
export const ROLE_IN_GROUP: Role = 'MEMBER'

/** An already-revoked credential can't be revoked again — revoke is a
 *  soft-delete, so revoked ones stay listed but inert. Gates the revoke button
 *  on a credential row. */
export function canRevokeCredential(credential: Credential): boolean {
  return !credential.revoked
}

/** Deleting the last admin locks administration out — the bootstrap seeder
 *  only re-creates one on restart. */
export function canDelete(principal: Principal, adminCount: number): boolean {
  return principal.role !== 'ADMIN' || adminCount > 1
}

/** {@link canDelete}'s other input: the LAST_ADMIN rule is about the
 *  population, not the row, so the count lives next to the rule it feeds. */
export function countAdmins(principals: Principal[]): number {
  return principals.filter((p) => p.role === 'ADMIN').length
}

/** Headline breakdown of the principal population for the section summary.
 *  `groups` and `admins` are overlapping subsets of `total` (a group can't be
 *  an admin, but both are counted against the whole), chosen because neither
 *  is obvious from scanning the tree the way the total is. */
export function summarizePrincipals(principals: Principal[]): {
  total: number
  groups: number
  admins: number
} {
  return {
    total: principals.length,
    groups: principals.filter(canHaveMembers).length,
    admins: countAdmins(principals),
  }
}
