import type { Source } from '../types/source.type'

/** Sample sources so the UI can be explored in mock mode without a backend. */
export const mockSources: Source[] = [
  {
    id: 'engineering-wiki',
    type: 'GIT',
    uriOrPath: 'https://github.com/acme/engineering-wiki.git',
    ref: 'main',
    include: ['**/*.md'],
    ignore: ['archive/**'],
    name: 'Engineering Wiki',
    description: 'Internal engineering handbook and runbooks.',
  },
  {
    id: 'product-docs',
    type: 'GIT',
    uriOrPath: 'https://github.com/acme/product-docs.git',
    ref: 'release',
    include: [],
    ignore: [],
    name: 'Product Docs',
    description: 'Public product documentation and API references.',
  },
  {
    id: 'support-macros',
    type: 'FS',
    uriOrPath: '/srv/knowledge/support',
    ref: null,
    include: ['**/*.txt', '**/*.md'],
    ignore: [],
    name: 'Support Macros',
    description: null,
  },
]
