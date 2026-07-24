// Extends Vitest's expect with jest-dom matchers (toBeInTheDocument, ...) and
// registers cleanup after each test. RTL's auto-cleanup only kicks in when the
// runner exposes afterEach globally; vitest here runs without `globals`, so the
// cleanup must be registered explicitly or DOM trees pile up across tests.
import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

afterEach(cleanup)
