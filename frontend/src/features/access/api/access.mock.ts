import type {
  Credential,
  EffectivePermissions,
  IssuedCredential,
  Principal,
} from '../types/access.type'

export const mockPrincipals: Principal[] = [
  { principalId: 'alice', type: 'SUBJECT', role: 'ADMIN' },
  { principalId: 'bob', type: 'SUBJECT', role: 'MEMBER' },
  { principalId: 'support-team', type: 'GROUP', role: 'MEMBER' },
]

export const mockMembers: Record<string, string[]> = {
  'support-team': ['bob'],
}

export const mockCredentials: Credential[] = [
  {
    credentialId: 'cred_01H9ZA',
    name: 'laptop',
    revoked: false,
    createdAt: '2026-06-01T09:15:00Z',
    lastUsedAt: '2026-07-17T07:55:12Z',
  },
  {
    credentialId: 'cred_01H9ZB',
    name: 'ci-pipeline',
    revoked: false,
    createdAt: '2026-06-14T11:02:30Z',
    lastUsedAt: null,
  },
  {
    credentialId: 'cred_01H9ZC',
    name: 'old-laptop',
    revoked: true,
    createdAt: '2026-01-08T16:40:00Z',
    lastUsedAt: '2026-05-30T10:11:00Z',
  },
]

export const mockIssuedCredential: IssuedCredential = {
  credentialId: 'cred_01H9ZD',
  name: 'new-credential',
  secret: 'kh_sk_mock_5f3a9c1e7b2d4086a1c3e5f7b9d0a2c4',
}

export const mockGrantedSources: string[] = ['engineering-wiki', 'product-docs']

export const mockEffectivePermissions: EffectivePermissions = {
  principalId: 'bob',
  defaultPolicy: 'DENY',
  readableSources: ['engineering-wiki', 'product-docs', 'support-macros'],
  grantedVia: {
    'engineering-wiki': ['bob'],
    'product-docs': ['bob'],
    'support-macros': ['support-team'],
  },
}
